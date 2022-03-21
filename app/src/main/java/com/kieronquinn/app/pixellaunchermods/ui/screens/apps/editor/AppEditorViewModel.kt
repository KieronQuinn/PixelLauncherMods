package com.kieronquinn.app.pixellaunchermods.ui.screens.apps.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.pixellaunchermods.components.navigation.ContainerNavigation
import com.kieronquinn.app.pixellaunchermods.model.editor.AppEditorInfo
import com.kieronquinn.app.pixellaunchermods.model.icon.IconPickerResult
import com.kieronquinn.app.pixellaunchermods.model.remote.RemoteApp
import com.kieronquinn.app.pixellaunchermods.model.room.IconMetadata.ApplyType
import com.kieronquinn.app.pixellaunchermods.repositories.DatabaseRepository
import com.kieronquinn.app.pixellaunchermods.repositories.IconLoaderRepository
import com.kieronquinn.app.pixellaunchermods.repositories.RemoteAppsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

abstract class AppEditorViewModel: ViewModel() {

    sealed class State {
        object Loading: State()
        data class Loaded(
            val appEditorInfo: AppEditorInfo,
            val areThemedIconsEnabled: Boolean,
            val nativeMonoIcons: Boolean
        ): State()
        object Saving: State()
        object Resetting: State()

        //State should always notify for any changes
        override fun equals(other: Any?) = false
    }

    abstract val state: StateFlow<State>
    abstract val fabVisible: StateFlow<Boolean>
    abstract fun setupWithRemoteApp(remoteApp: RemoteApp)
    abstract fun onLabelChanged(newLabel: String)
    abstract fun onThemedIconChanged(result: IconPickerResult?)
    abstract fun onNormalIconChanged(result: IconPickerResult)
    abstract fun onSaveClicked()
    abstract fun onResetClicked()
    abstract fun onThemedIconClick()
    abstract fun onNormalIconClick()

}

class AppEditorViewModelImpl(
    private val iconLoaderRepository: IconLoaderRepository,
    private val navigation: ContainerNavigation,
    private val databaseRepository: DatabaseRepository,
    private val remoteAppsRepository: RemoteAppsRepository
) : AppEditorViewModel() {

    private val remoteApp = MutableStateFlow<RemoteApp?>(null)
    private val modifiedApp = remoteApp.filterNotNull().flatMapLatest {
        databaseRepository.getModifiedApp(it.componentName)
    }
    private val workspace = MutableStateFlow<AppEditorInfo?>(null)

    private val appEditorInfo = combine(remoteApp.filterNotNull(), modifiedApp, workspace){
        remote, modified, working ->
        working ?: AppEditorInfo.merge(remote, modified, areNativeMonoIconsSupported())
    }

    private val themedIconsEnabled = remoteAppsRepository.areThemedIconsEnabled

    private val saving = MutableStateFlow(false)
    private val resetting = MutableStateFlow(false)
    private val hasChanges = MutableStateFlow(false)

    override val state = combine(appEditorInfo, themedIconsEnabled.filterNotNull(), saving, resetting) {
        info, themed, isSaving, isResetting ->
        when {
            isResetting -> State.Resetting
            isSaving -> State.Saving
            else-> State.Loaded(info, themed, areNativeMonoIconsSupported())
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override val fabVisible = combine(hasChanges, saving, resetting) { _, _, _ ->
        shouldShowFab()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, shouldShowFab())

    override fun setupWithRemoteApp(remoteApp: RemoteApp) {
        if(state.value !is State.Loading && state.value !is State.Resetting) return
        viewModelScope.launch(Dispatchers.IO) {
            this@AppEditorViewModelImpl.remoteApp.emit(remoteApp)
            resetting.emit(false)
        }
    }

    override fun onLabelChanged(newLabel: String) {
        viewModelScope.launch {
            val appEditorInfo = getCurrentEditorInfoOrNull() ?: return@launch
            if(appEditorInfo.label == newLabel) return@launch
            appEditorInfo.updateLabel(newLabel)
            hasChanges.emit(true)
            workspace.emit(appEditorInfo)
        }
    }

    override fun onSaveClicked() {
        viewModelScope.launch {
            val editorInfo = workspace.value ?: return@launch
            saveAppInfo(editorInfo)
        }
    }

    override fun onResetClicked() {
        viewModelScope.launch(Dispatchers.IO) {
            if(resetting.value) return@launch
            val appEditorInfo = getCurrentEditorInfoOrNull() ?: return@launch
            databaseRepository.removeModifiedApp(appEditorInfo.componentName)
            resetting.emit(true)
            val remoteApp = if(appEditorInfo.iconType == RemoteApp.Type.APP_SHORTCUT){
                remoteAppsRepository.resetModifiedShortcut(appEditorInfo.componentName)
            }else {
                remoteAppsRepository.resetModifiedApp(appEditorInfo.componentName)
            } ?: run {
                //Invalid response back from DB, fall back to non-reset item
                resetting.emit(false)
                return@launch
            }
            workspace.emit(null)
            hasChanges.emit(false)
            setupWithRemoteApp(remoteApp)
        }
    }

    override fun onThemedIconClick() {
        viewModelScope.launch {
            val appEditorInfo = getCurrentEditorInfoOrNull() ?: return@launch
            if(appEditorInfo.isDynamic()) return@launch
            navigation.navigate(AppEditorFragmentDirections.actionAppEditorFragmentToNavGraphIconPicker(
                appEditorInfo.componentName, true
            ))
        }
    }

    override fun onNormalIconClick() {
        viewModelScope.launch {
            val appEditorInfo = getCurrentEditorInfoOrNull() ?: return@launch
            if(appEditorInfo.isDynamic()) return@launch
            navigation.navigate(AppEditorFragmentDirections.actionAppEditorFragmentToNavGraphIconPicker(
                appEditorInfo.componentName, false
            ))
        }
    }

    override fun onThemedIconChanged(result: IconPickerResult?) {
        viewModelScope.launch {
            val editorInfo = getCurrentEditorInfoOrNull() ?: return@launch
            editorInfo.updateMonoIcon(
                result?.toMonoByteArray(iconLoaderRepository),
                result?.toLegacyMonoByteArray(iconLoaderRepository),
                result?.toModifiedAppMetadata(ApplyType.MANUAL),
                result?.toMonoByteArray(iconLoaderRepository, true)
            )
            hasChanges.emit(true)
            workspace.emit(editorInfo)
        }
    }

    override fun onNormalIconChanged(result: IconPickerResult) {
        viewModelScope.launch {
            val editorInfo = getCurrentEditorInfoOrNull() ?: return@launch
            editorInfo.run {
                editorInfo.updateIcon(
                    result.toByteArray(iconLoaderRepository),
                    result.toModifiedAppMetadata(ApplyType.MANUAL),
                    result.toByteArray(iconLoaderRepository, true)
                )
                hasChangedIcon = true
            }
            hasChanges.emit(true)
            workspace.emit(editorInfo)
        }
    }

    private suspend fun saveAppInfo(appEditorInfo: AppEditorInfo){
        saving.emit(true)
        databaseRepository.saveAppEditorInfo(appEditorInfo)
        remoteAppsRepository.updateModifiedApps()
        navigation.navigateBack()
    }

    private suspend fun areNativeMonoIconsSupported(): Boolean {
        return remoteAppsRepository.areNativeThemedIconsSupported.filterNotNull().first()
    }

    private fun getCurrentEditorInfoOrNull(): AppEditorInfo? {
        return (state.value as? State.Loaded)?.appEditorInfo
    }

    private fun shouldShowFab(): Boolean {
        return hasChanges.value && !resetting.value && !saving.value
    }

}