package com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.widgetreplacement.widgetpicker

import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.pixellaunchermods.BuildConfig
import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.components.navigation.ContainerNavigation
import com.kieronquinn.app.pixellaunchermods.model.tweaks.WidgetReplacement
import com.kieronquinn.app.pixellaunchermods.repositories.AppsRepository
import com.kieronquinn.app.pixellaunchermods.repositories.ProxyAppWidgetRepository
import com.kieronquinn.app.pixellaunchermods.repositories.RootServiceRepository
import com.kieronquinn.app.pixellaunchermods.repositories.SettingsRepository
import com.kieronquinn.app.pixellaunchermods.widget.ProxyWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class WidgetReplacementPickerViewModel: ViewModel() {

    abstract val state: StateFlow<State>
    abstract val searchShowClear: StateFlow<Boolean>
    abstract val errorBus: Flow<Int>

    abstract fun onAppClicked(app: Item.App): Array<Int>
    abstract fun getSearchTerm(): String
    abstract fun setSearchTerm(search: String)

    abstract fun onWidgetClicked(
        context: Context,
        provider: AppWidgetProviderInfo,
        configureLauncher: ActivityResultLauncher<IntentSenderRequest>,
        bindLauncher: ActivityResultLauncher<Intent>
    )

    abstract fun onWidgetBound(
        context: Context,
        provider: AppWidgetProviderInfo?,
        configureLauncher: ActivityResultLauncher<IntentSenderRequest>,
        appWidgetId: Int
    )

    abstract fun onWidgetConfigured(
        context: Context,
        provider: AppWidgetProviderInfo?,
        appWidgetId: Int
    )

    abstract fun onWidgetCancelled(
        appWidgetId: Int
    )

    sealed class State {
        object Loading: State()
        data class Loaded(val items: List<Item>, val replacement: WidgetReplacement): State()
    }

    sealed class Item(open val label: CharSequence, val id: String, val type: Type) {
        object Header: Item("", "plm:header", Type.HEADER)

        data class App(
            override val label: CharSequence,
            val appInfo: ApplicationInfo,
            var widgetCount: Int = 0,
            var isOpen: Boolean = false
        ): Item(label, appInfo.packageName, Type.APP)

        data class WidgetImage(
            override val label: CharSequence,
            val description: CharSequence?,
            val parent: App,
            val info: AppWidgetProviderInfo
        ): Item(label, info.provider.flattenToString(), Type.WIDGET_IMAGE)

        data class WidgetLayout(
            override val label: CharSequence,
            val description: CharSequence?,
            val parent: App,
            val info: AppWidgetProviderInfo
        ): Item(label, info.provider.flattenToString(), Type.WIDGET_LAYOUT)

        enum class Type {
            HEADER, APP, WIDGET_IMAGE, WIDGET_LAYOUT
        }
    }

}

class WidgetReplacementPickerViewModelImpl(
    private val appWidgetRepository: ProxyAppWidgetRepository,
    private val appsRepository: AppsRepository,
    settingsRepository: SettingsRepository,
    private val rootServiceRepository: RootServiceRepository,
    private val navigation: ContainerNavigation
): WidgetReplacementPickerViewModel() {

    private val searchTerm = MutableStateFlow("")
    private var pendingProvider: AppWidgetProviderInfo? = null

    override val searchShowClear = searchTerm.map { it.isNotBlank() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    override val errorBus = MutableSharedFlow<Int>()

    private val widgetReplacement = settingsRepository.widgetReplacement.asFlow()

    private val appWidgets = flow {
        emit(appWidgetRepository.getAllProviders().filterNot {
            //Ignore our own proxy widget
            it.provider.packageName == BuildConfig.APPLICATION_ID &&
                    it.provider.className == ProxyWidget::class.java.name
        }.groupBy {
            val packageName = it.activityInfo.packageName
            val appInfo = appsRepository.getApplicationInfoForPackage(packageName) ?: return@groupBy null
            val label = appsRepository.loadApplicationLabel(appInfo)
            Item.App(label, appInfo)
        })
    }

    override val state = combine(appWidgets, searchTerm, widgetReplacement) { all, search, replacement ->
        State.Loaded(getItems(all, search), replacement)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    private suspend fun getItems(
        allWidgets: Map<Item.App?, List<AppWidgetProviderInfo>>,
        searchTerm: String
    ): ArrayList<Item> = withContext(Dispatchers.IO) {
        val items = ArrayList<Item>()
        items.add(Item.Header)
        allWidgets.entries
            .filterNot { it.key == null }
            .sortedBy { it.key!!.label.toString().lowercase() }
            .forEach {
                val app = it.key
                val appContainsTerm = app!!.label.contains(searchTerm, true)
                val widgets = it.value.map { item -> item.toWidgetItem(app) }.filter { item ->
                    appContainsTerm || searchTerm.isBlank() || item.label.contains(searchTerm, true)
                }.sortedBy { item -> item.label.toString().lowercase() }
                if(widgets.isEmpty()) return@forEach
                app.widgetCount = widgets.size
                items.add(app)
                items.addAll(widgets)
            }
        items
    }

    private fun AppWidgetProviderInfo.toWidgetItem(parent: Item.App): Item {
        val label = appWidgetRepository.loadWidgetLabel(this)
        val description = appWidgetRepository.loadWidgetDescription(this)
        return if(previewLayout != 0){
            Item.WidgetLayout(label, description, parent,this)
        }else{
            Item.WidgetImage(label, description, parent,this)
        }
    }

    override fun onAppClicked(app: Item.App): Array<Int> {
        val items = (state.value as? State.Loaded)?.items ?: return emptyArray()
        return items.mapIndexedNotNull { index, item ->
            val parent = (item as? Item.WidgetImage)?.parent ?: (item as? Item.WidgetLayout)?.parent
                ?: return@mapIndexedNotNull null
            if(parent == app) {
                index
            }else{
                null
            }
        }.toTypedArray()
    }

    override fun onWidgetClicked(
        context: Context,
        provider: AppWidgetProviderInfo,
        configureLauncher: ActivityResultLauncher<IntentSenderRequest>,
        bindLauncher: ActivityResultLauncher<Intent>
    ) {
        pendingProvider = provider
        val appWidgetId = appWidgetRepository.allocateAppWidgetId(provider, bindLauncher) ?: return
        onWidgetBound(context, provider, configureLauncher, appWidgetId)
    }

    override fun onWidgetBound(
        context: Context,
        provider: AppWidgetProviderInfo?,
        configureLauncher: ActivityResultLauncher<IntentSenderRequest>,
        appWidgetId: Int
    ) {
        val boundProvider = provider ?: pendingProvider ?: return
        if(boundProvider.configure != null){
            //Start the extra configure step, onWidgetConfigured will be called when done
            val intentSender = appWidgetRepository.getConfigureIntentSenderForProvider(appWidgetId)
            configureLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
        }else{
            //No config required, proceed with setup immediately
            onWidgetConfigured(context, boundProvider, appWidgetId)
        }
    }

    override fun onWidgetConfigured(
        context: Context,
        provider: AppWidgetProviderInfo?,
        appWidgetId: Int
    ) {
        val emitError = suspend {
            pendingProvider = null
            appWidgetRepository.deallocateAppWidgetId(appWidgetId)
            errorBus.emit(R.string.widget_replacement_picker_error_toast)
        }
        viewModelScope.launch {
            val appWidgetProvider = provider ?: pendingProvider ?: run {
                emitError()
                return@launch
            }
            pendingProvider = null
            rootServiceRepository.runWithRootService {
                it.setSearchWidgetPackageEnabled(true)
                it.restartLauncherImmediately()
            } ?: run {
                emitError()
                return@launch
            }
            appWidgetRepository.commitProxyWidgetProvider(
                context, appWidgetProvider.provider.flattenToShortString(), appWidgetId
            )
            navigation.navigateBack()
        }
    }

    override fun onWidgetCancelled(appWidgetId: Int) {
        pendingProvider = null
        appWidgetRepository.deallocateAppWidgetId(appWidgetId)
    }

    override fun getSearchTerm(): String {
        return searchTerm.value
    }

    override fun setSearchTerm(search: String) {
        viewModelScope.launch {
            searchTerm.emit(search)
        }
    }

}