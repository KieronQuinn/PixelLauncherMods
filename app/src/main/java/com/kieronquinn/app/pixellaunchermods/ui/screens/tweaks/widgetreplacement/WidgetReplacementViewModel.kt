package com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.widgetreplacement

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.pixellaunchermods.components.navigation.ContainerNavigation
import com.kieronquinn.app.pixellaunchermods.model.tweaks.ParceledWidgetReplacement
import com.kieronquinn.app.pixellaunchermods.model.tweaks.WidgetReplacement
import com.kieronquinn.app.pixellaunchermods.repositories.OverlayRepository
import com.kieronquinn.app.pixellaunchermods.repositories.ProxyAppWidgetRepository
import com.kieronquinn.app.pixellaunchermods.repositories.SettingsRepository
import com.kieronquinn.app.pixellaunchermods.utils.extensions.allowBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class WidgetReplacementViewModel: ViewModel() {

    abstract suspend fun getWidgetView(widgetPosition: WidgetPosition): AppWidgetHostView?
    abstract fun startListening()
    abstract fun stopListening()

    abstract val state: StateFlow<State>
    abstract fun onSwitchStateChanged(enabled: Boolean)
    abstract fun onToggleStateChanged(position: WidgetPosition)
    abstract fun onSaveClicked()
    abstract fun onSelectClicked()
    abstract fun onReconfigureClicked(configureLauncher: ActivityResultLauncher<IntentSenderRequest>)
    abstract fun reload()
    abstract fun onSaveModuleClicked(launcher: ActivityResultLauncher<String>)
    abstract fun saveModule(uri: Uri)

    enum class WidgetPosition(val index: Int) {
        @Deprecated("No longer possible, removed")
        TOP(0),
        BOTTOM(1)
    }

    sealed class State {
        object Loading: State()
        object ModuleRequired: State()
        data class Loaded(val showFab: Boolean, val items: List<Item>): State()
    }

    sealed class Item(val type: Type) {
        data class Switch(val enabled: Boolean): Item(Type.SWITCH)
        data class Preview(val position: WidgetPosition): Item(Type.PREVIEW)
        data class ProviderPicker(val provider: AppWidgetProviderInfo?): Item(Type.PROVIDER_PICKER)
        object ProviderReconfigure: Item(Type.PROVIDER_RECONFIGURE)
        object Info: Item(Type.INFO)

        enum class Type {
            SWITCH, PREVIEW, PROVIDER_PICKER, PROVIDER_RECONFIGURE, INFO
        }
    }

}

class WidgetReplacementViewModelImpl(
    private val widgetRepository: ProxyAppWidgetRepository,
    private val overlayRepository: OverlayRepository,
    private val navigation: ContainerNavigation,
    private val settingsRepository: SettingsRepository
): WidgetReplacementViewModel() {

    private val reloadBus = MutableStateFlow(System.currentTimeMillis())

    private val replacementState = flow {
        emit(overlayRepository.getWidgetReplacement())
    }

    override val state by lazy {
        reloadBus.flatMapLatest {
            loadState()
        }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)
    }

    private val isOverlayInstalled = flow {
        emit(overlayRepository.isOverlayInstalled())
    }

    private val provider = settingsRepository.qsbWidgetProvider.asFlow()
    private val localSwitchState = MutableStateFlow<Boolean?>(null)
    private val localToggleState = MutableStateFlow<WidgetPosition?>(null)

    private val localWidgetReplacement = combine(
        localSwitchState,
        localToggleState,
        settingsRepository.widgetReplacement.asFlow()
    ){ switch, toggle, remote ->
        when {
            switch != null && !switch -> WidgetReplacement.NONE
            toggle == WidgetPosition.TOP -> WidgetReplacement.BOTTOM
            toggle == WidgetPosition.BOTTOM -> WidgetReplacement.BOTTOM
            switch != null && remote != WidgetReplacement.NONE -> {
                remote //Default to whatever the remote value is if one is set
            }
            switch != null -> {
                WidgetReplacement.BOTTOM
            }
            else -> null
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private fun loadState() = combine(
        isOverlayInstalled,
        replacementState,
        localWidgetReplacement,
        provider
    ) { installed, replacement, localReplacement, provider ->
        if(!installed) return@combine State.ModuleRequired
        val providerInfo = widgetRepository.getProviderInfo(provider)
        val bestReplacement = localReplacement ?: replacement
        val enabled = bestReplacement != WidgetReplacement.NONE
        val canReconfigure = providerInfo?.let {
            it.widgetFeatures and AppWidgetProviderInfo.WIDGET_FEATURE_RECONFIGURABLE != 0
        } ?: false
        val widgetPosition = when(bestReplacement){
            WidgetReplacement.TOP -> WidgetPosition.BOTTOM
            WidgetReplacement.BOTTOM -> WidgetPosition.BOTTOM
            WidgetReplacement.NONE -> WidgetPosition.BOTTOM //Default to top, although it will be hidden
        }
        val items = listOfNotNull(
            Item.Switch(enabled),
            if (enabled) Item.Preview(widgetPosition) else null,
            Item.Info,
            if (enabled) Item.ProviderPicker(providerInfo) else null,
            if (enabled && canReconfigure) Item.ProviderReconfigure else null
        )
        State.Loaded(localReplacement != null, items)
    }

    override suspend fun getWidgetView(widgetPosition: WidgetPosition): AppWidgetHostView? {
        return when(widgetPosition){
            WidgetPosition.TOP -> widgetRepository.getCurrentProxyWidgetPreviewTop()
            WidgetPosition.BOTTOM -> widgetRepository.getCurrentProxyWidgetPreviewBottom()
        }
    }

    override fun onSwitchStateChanged(enabled: Boolean) {
        viewModelScope.launch {
            localSwitchState.emit(enabled)
        }
    }

    override fun onToggleStateChanged(position: WidgetPosition) {
        viewModelScope.launch {
            localToggleState.emit(position)
        }
    }

    override fun onSaveClicked() {
        val localReplacement = localWidgetReplacement.value ?: return
        viewModelScope.launch {
            navigation.navigate(WidgetReplacementFragmentDirections
                .actionTweaksWidgetReplacementFragmentToTweaksApplyFragment(
                    null,
                    ParceledWidgetReplacement(localReplacement),
                    null,
                    null,
                    null,
                    null
                ))
        }
    }

    override fun startListening() {
        widgetRepository.startListeningRegular()
        widgetRepository.setListening(true)
    }

    override fun stopListening() {
        widgetRepository.stopListeningRegular()
        widgetRepository.setListening(false)
    }

    override fun onSelectClicked() {
        viewModelScope.launch {
            navigation.navigate(WidgetReplacementFragmentDirections.actionTweaksWidgetReplacementFragmentToWidgetReplacementPickerFragment())
        }
    }

    override fun reload() {
        viewModelScope.launch {
            localSwitchState.emit(null)
            localToggleState.emit(null)
            reloadBus.emit(System.currentTimeMillis())
        }
    }

    override fun onSaveModuleClicked(launcher: ActivityResultLauncher<String>) {
        launcher.launch(overlayRepository.getModuleFilename())
    }

    override fun saveModule(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            overlayRepository.saveModule(uri)
        }
    }

    override fun onReconfigureClicked(
        configureLauncher: ActivityResultLauncher<IntentSenderRequest>
    ) {
        viewModelScope.launch {
            val appWidgetId = settingsRepository.qsbWidgetId.get()
            if(appWidgetId == -1) return@launch
            val intentSender = IntentSenderRequest.Builder(
                widgetRepository.getConfigureIntentSenderForProvider(appWidgetId)
            ).build()
            val options = ActivityOptionsCompat.makeBasic().allowBackground()
            configureLauncher.launch(intentSender, options)
        }
    }

}