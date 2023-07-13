package com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks

import android.net.Uri
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.pixellaunchermods.components.navigation.ContainerNavigation
import com.kieronquinn.app.pixellaunchermods.repositories.HideClockRepository
import com.kieronquinn.app.pixellaunchermods.repositories.OverlayRepository
import com.kieronquinn.app.pixellaunchermods.repositories.SettingsRepository
import com.kieronquinn.app.pixellaunchermods.ui.base.settings.BaseSettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class TweaksViewModel: BaseSettingsViewModel() {

    abstract val hideClock: SettingsRepository.PixelLauncherModsSetting<Boolean>

    abstract fun onWidgetResizeClicked()
    abstract fun onWidgetReplacementClicked()
    abstract fun onRecentsClicked()
    abstract fun onHideAppsClicked()
    abstract fun onSaveModuleClicked(launcher: ActivityResultLauncher<String>)
    abstract fun saveModule(uri: Uri)

}

class TweaksViewModelImpl(
    private val containerNavigation: ContainerNavigation,
    private val hideClockRepository: HideClockRepository,
    private val overlayRepository: OverlayRepository,
    settingsRepository: SettingsRepository
): TweaksViewModel() {

    override val hideClock = settingsRepository.hideClock

    override fun onWidgetResizeClicked() {
        viewModelScope.launch {
            containerNavigation.navigate(TweaksFragmentDirections.actionTweaksFragmentToWidgetResizeActivity())
        }
    }

    override fun onWidgetReplacementClicked() {
        viewModelScope.launch {
            containerNavigation.navigate(TweaksFragmentDirections.actionTweaksFragmentToTweaksWidgetReplacementFragment())
        }
    }

    override fun onHideAppsClicked() {
        viewModelScope.launch {
            containerNavigation.navigate(TweaksFragmentDirections.actionTweaksFragmentToHideAppsFragment())
        }
    }

    override fun onRecentsClicked() {
        viewModelScope.launch {
            containerNavigation.navigate(TweaksFragmentDirections.actionTweaksFragmentToRecentsTweaksFragment())
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

    //Immediately show the clock when the option is disabled to prevent it getting stuck disabled
    private fun setupDisableHideClock() = viewModelScope.launch {
        hideClock.asFlow().collect {
            if(!it){
                hideClockRepository.setClockVisible(true)
            }
        }
    }

    init {
        setupDisableHideClock()
    }

}