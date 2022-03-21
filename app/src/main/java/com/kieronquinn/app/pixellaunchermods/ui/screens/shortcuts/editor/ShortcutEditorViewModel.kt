package com.kieronquinn.app.pixellaunchermods.ui.screens.shortcuts.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.pixellaunchermods.components.navigation.ContainerNavigation
import com.kieronquinn.app.pixellaunchermods.model.editor.ShortcutEditorInfo
import com.kieronquinn.app.pixellaunchermods.model.icon.IconPickerResult
import com.kieronquinn.app.pixellaunchermods.model.remote.RemoteFavourite
import com.kieronquinn.app.pixellaunchermods.model.room.IconMetadata
import com.kieronquinn.app.pixellaunchermods.model.room.ModifiedShortcut
import com.kieronquinn.app.pixellaunchermods.repositories.DatabaseRepository
import com.kieronquinn.app.pixellaunchermods.repositories.IconLoaderRepository
import com.kieronquinn.app.pixellaunchermods.repositories.RemoteAppsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

abstract class ShortcutEditorViewModel: ViewModel() {

    sealed class State {
        object Loading: State()
        data class Loaded(
            val shortcutEditorInfo: ShortcutEditorInfo
        ): State()
        object Saving: State()
        object Resetting: State()

        //State should always notify for any changes
        override fun equals(other: Any?) = false
    }

    abstract val state: StateFlow<State>
    abstract val fabVisible: StateFlow<Boolean>
    abstract fun setupWithRemoteFavourite(remoteFavourite: RemoteFavourite)
    abstract fun onLabelChanged(newLabel: String)
    abstract fun onIconChanged(result: IconPickerResult)
    abstract fun onSaveClicked()
    abstract fun onResetClicked()
    abstract fun onIconClick()

}

class ShortcutEditorViewModelImpl(
    private val iconLoaderRepository: IconLoaderRepository,
    private val navigation: ContainerNavigation,
    private val databaseRepository: DatabaseRepository,
    private val remoteAppsRepository: RemoteAppsRepository
) : ShortcutEditorViewModel() {

    private val remoteFavourite = MutableStateFlow<RemoteFavourite?>(null)

    private val modifiedApp = remoteFavourite.filterNotNull().flatMapLatest {
        databaseRepository.getModifiedShortcut(it.intent!!)
    }

    private val workspace = MutableStateFlow<ShortcutEditorInfo?>(null)

    private val appEditorInfo = combine(remoteFavourite.filterNotNull(), modifiedApp, workspace){
            remote, modified, working ->
        working ?: ShortcutEditorInfo.merge(remote, modified)
    }

    private val saving = MutableStateFlow(false)
    private val hasChanges = MutableStateFlow(false)
    private val resetting = MutableStateFlow(false)

    override val state = combine(appEditorInfo, saving, resetting) { info, isSaving, isResetting ->
        when {
            isResetting -> State.Resetting
            isSaving -> State.Saving
            else -> State.Loaded(info)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override val fabVisible = combine(hasChanges, saving, resetting) { _, _, _ ->
        shouldShowFab()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, shouldShowFab())

    override fun setupWithRemoteFavourite(remoteFavourite: RemoteFavourite) {
        if(state.value !is State.Loading && state.value !is State.Resetting) return
        viewModelScope.launch(Dispatchers.IO) {
            this@ShortcutEditorViewModelImpl.remoteFavourite.emit(remoteFavourite)
        }
    }

    override fun onLabelChanged(newLabel: String) {
        viewModelScope.launch {
            val appEditorInfo = getCurrentEditorInfoOrNull() ?: return@launch
            appEditorInfo.updateLabel(newLabel)
            hasChanges.emit(true)
            workspace.emit(appEditorInfo)
        }
    }

    override fun onSaveClicked() {
        viewModelScope.launch {
            val editorInfo = workspace.value ?: return@launch
            saveShortcutInfo(editorInfo)
        }
    }

    override fun onResetClicked() {
        viewModelScope.launch {
            if(resetting.value) return@launch
            val appEditorInfo = getCurrentEditorInfoOrNull() ?: return@launch
            resetting.emit(true)
            val newShortcutEditorInfo = ShortcutEditorInfo(
                appEditorInfo.intent,
                appEditorInfo.originalTitle,
                false,
                appEditorInfo.originalIcon,
                false,
                null,
                appEditorInfo.originalTitle,
                appEditorInfo.originalIcon
            )
            resetShortcutInfo(newShortcutEditorInfo)
            resetting.emit(false)
        }
    }

    override fun onIconClick() {
        viewModelScope.launch {
            navigation.navigate(ShortcutEditorFragmentDirections.actionShortcutEditorFragmentToNavGraphIconPicker(
                "", false
            ))
        }
    }

    override fun onIconChanged(result: IconPickerResult) {
        viewModelScope.launch {
            val editorInfo = getCurrentEditorInfoOrNull() ?: return@launch
            editorInfo.run {
                editorInfo.updateIcon(
                    result.toByteArray(iconLoaderRepository),
                    result.toModifiedAppMetadata(IconMetadata.ApplyType.MANUAL)
                )
                hasChangedIcon = true
            }
            hasChanges.emit(true)
            workspace.emit(editorInfo)
        }
    }

    private suspend fun saveShortcutInfo(shortcutEditorInfo: ShortcutEditorInfo){
        saving.emit(true)
        val modifiedShortcut = ModifiedShortcut(
            shortcutEditorInfo.intent,
            if(shortcutEditorInfo.hasChangedTitle) shortcutEditorInfo.title else null,
            if(shortcutEditorInfo.hasChangedIcon) shortcutEditorInfo.icon else null,
            shortcutEditorInfo.iconMetadata,
            shortcutEditorInfo.originalTitle,
            shortcutEditorInfo.originalIcon
        )
        remoteAppsRepository.updateShortcut(shortcutEditorInfo.toRemoteFavourite())
        databaseRepository.saveModifiedShortcut(modifiedShortcut)
        navigation.navigateBack()
    }

    private suspend fun resetShortcutInfo(shortcutEditorInfo: ShortcutEditorInfo){
        val modifiedShortcut = ModifiedShortcut(
            shortcutEditorInfo.intent,
            if(shortcutEditorInfo.hasChangedTitle) shortcutEditorInfo.title else null,
            if(shortcutEditorInfo.hasChangedIcon) shortcutEditorInfo.icon else null,
            shortcutEditorInfo.iconMetadata,
            shortcutEditorInfo.originalTitle,
            shortcutEditorInfo.originalIcon
        )
        val favourite = shortcutEditorInfo.toRemoteFavourite()
        remoteAppsRepository.updateShortcut(favourite)
        databaseRepository.removeModifiedShortcut(modifiedShortcut.intent)
        workspace.emit(null)
        hasChanges.emit(false)
        setupWithRemoteFavourite(favourite)
    }

    private fun getCurrentEditorInfoOrNull(): ShortcutEditorInfo? {
        return (state.value as? State.Loaded)?.shortcutEditorInfo
    }

    private fun shouldShowFab(): Boolean {
        return hasChanges.value && !resetting.value && !saving.value
    }

}