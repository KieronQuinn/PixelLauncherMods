package com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.recents

import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.pixellaunchermods.components.navigation.ContainerNavigation
import com.kieronquinn.app.pixellaunchermods.repositories.OverlayRepository
import com.kieronquinn.app.pixellaunchermods.repositories.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

abstract class RecentsTweaksViewModel: ViewModel() {

    sealed class State {
        object Loading: State()
        object ModuleRequired: State()
        data class Loaded(val transparency: Float): State()
    }

    abstract val state: StateFlow<State>
    abstract val transparency: StateFlow<Float>

    abstract fun reload()
    abstract fun onTransparencyChanged(transparency: Float)
    abstract fun onSaveClicked()
    abstract fun onSaveModuleClicked(launcher: ActivityResultLauncher<String>)
    abstract fun saveModule(uri: Uri)

}

class RecentsTweaksViewModelImpl(
    private val overlayRepository: OverlayRepository,
    private val containerNavigation: ContainerNavigation,
    settingsRepository: SettingsRepository
): RecentsTweaksViewModel() {

    private val reloadBus = MutableStateFlow(System.currentTimeMillis())

    override val state = reloadBus.mapLatest {
        State.Loaded(settingsRepository.recentsBackgroundTransparency.get())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override val transparency =
        MutableStateFlow(settingsRepository.recentsBackgroundTransparency.getSync())

    override fun reload() {
        viewModelScope.launch {
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

    override fun onTransparencyChanged(transparency: Float) {
        viewModelScope.launch {
            this@RecentsTweaksViewModelImpl.transparency.emit(transparency)
        }
    }

    override fun onSaveClicked() {
        viewModelScope.launch {
            val transparency = transparency.value.toString()
            containerNavigation.navigate(RecentsTweaksFragmentDirections.actionRecentsTweaksFragmentToTweaksApplyFragment(
                null, null, transparency
            ))
        }
    }

}