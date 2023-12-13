package com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.overlay

import android.net.Uri
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.pixellaunchermods.components.navigation.ContainerNavigation
import com.kieronquinn.app.pixellaunchermods.repositories.OverlayRepository
import com.kieronquinn.app.pixellaunchermods.repositories.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class OverlayTweaksViewModel: ViewModel() {

    sealed class State {
        object Loading: State()
        object ModuleRequired: State()
        data class Loaded(
            val transparency: Float,
            val wallpaperScrim: Boolean,
            val wallpaperRegionColours: Boolean,
            val disableSmartspace: Boolean
        ): State()
    }

    abstract val state: StateFlow<State>
    abstract val transparency: StateFlow<Float>
    abstract val wallpaperScrim: StateFlow<Boolean>
    abstract val wallpaperRegionColours: StateFlow<Boolean>
    abstract val smartspace: StateFlow<Boolean>

    abstract fun reload()
    abstract fun onTransparencyChanged(transparency: Float)
    abstract fun onWallpaperScrimChanged(enabled: Boolean)
    abstract fun onWallpaperRegionColoursChanged(enabled: Boolean)
    abstract fun onDisableSmartspaceChanged(enabled: Boolean)
    abstract fun onSaveClicked()
    abstract fun onSaveModuleClicked(launcher: ActivityResultLauncher<String>)
    abstract fun saveModule(uri: Uri)

}

class OverlayTweaksViewModelImpl(
    private val overlayRepository: OverlayRepository,
    private val containerNavigation: ContainerNavigation,
    settingsRepository: SettingsRepository
): OverlayTweaksViewModel() {

    private val reloadBus = MutableStateFlow(System.currentTimeMillis())

    private val recentsTransparency = MutableStateFlow(
        settingsRepository.recentsBackgroundTransparency.getSyncOrNull()
    )

    private val disableWallpaperScrim = MutableStateFlow(
        settingsRepository.disableWallpaperScrim.getSyncOrNull() ?: false
                && Build.VERSION.SDK_INT < 34
    )

    private val disableWallpaperRegionColours = MutableStateFlow(
        settingsRepository.disableWallpaperRegionColours.getSyncOrNull()
    )

    private val disableSmartspace = MutableStateFlow(
        settingsRepository.disableSmartspace.getSyncOrNull()
    )

    override val transparency = recentsTransparency.mapLatest {
        it ?: 0f
    }.stateIn(viewModelScope, SharingStarted.Eagerly, recentsTransparency.value ?: 0f)

    override val wallpaperScrim = disableWallpaperScrim.mapLatest {
        it ?: false
    }.stateIn(viewModelScope, SharingStarted.Eagerly, disableWallpaperScrim.value ?: false)

    override val smartspace = disableSmartspace.mapLatest {
        it ?: false
    }.stateIn(viewModelScope, SharingStarted.Eagerly, disableSmartspace.value ?: false)

    override val wallpaperRegionColours = disableWallpaperRegionColours.mapLatest {
        it ?: false
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        disableWallpaperRegionColours.value ?: false
    )

    override val state = combine(
        reloadBus,
        transparency,
        wallpaperScrim,
        wallpaperRegionColours,
        smartspace
    ) { _, recents, scrim, region, disableSmartspace ->
        State.Loaded(recents, scrim, region, disableSmartspace)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

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
            this@OverlayTweaksViewModelImpl.recentsTransparency.emit(transparency)
        }
    }

    override fun onWallpaperScrimChanged(enabled: Boolean) {
        viewModelScope.launch {
            disableWallpaperScrim.emit(enabled)
        }
    }

    override fun onWallpaperRegionColoursChanged(enabled: Boolean) {
        viewModelScope.launch {
            disableWallpaperRegionColours.emit(enabled)
        }
    }

    override fun onDisableSmartspaceChanged(enabled: Boolean) {
        viewModelScope.launch {
            disableSmartspace.emit(enabled)
        }
    }

    override fun onSaveClicked() {
        viewModelScope.launch {
            containerNavigation.navigate(OverlayTweaksFragmentDirections.actionRecentsTweaksFragmentToTweaksApplyFragment(
                null,
                null,
                recentsTransparency.value?.toString(),
                disableWallpaperScrim.value?.toString(),
                disableWallpaperRegionColours.value?.toString(),
                disableSmartspace.value?.toString()
            ))
        }
    }

}