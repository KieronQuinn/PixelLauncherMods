package com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks

import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.pixellaunchermods.components.navigation.ContainerNavigation
import com.kieronquinn.app.pixellaunchermods.components.navigation.RootNavigation
import com.kieronquinn.app.pixellaunchermods.repositories.HideClockRepository
import com.kieronquinn.app.pixellaunchermods.repositories.SettingsRepository
import com.kieronquinn.app.pixellaunchermods.ui.base.settings.BaseSettingsViewModel
import com.kieronquinn.app.pixellaunchermods.ui.screens.container.ContainerFragmentDirections
import kotlinx.coroutines.launch

abstract class TweaksViewModel: BaseSettingsViewModel() {

    abstract val hideClock: SettingsRepository.PixelLauncherModsSetting<Boolean>

    abstract fun onWidgetResizeClicked()
    abstract fun onWidgetReplacementClicked()
    abstract fun onRecentsClicked()
    abstract fun onHideAppsClicked()

}

class TweaksViewModelImpl(
    private val rootNavigation: RootNavigation,
    private val containerNavigation: ContainerNavigation,
    private val hideClockRepository: HideClockRepository,
    settingsRepository: SettingsRepository
): TweaksViewModel() {

    override val hideClock = settingsRepository.hideClock

    override fun onWidgetResizeClicked() {
        viewModelScope.launch {
            rootNavigation.navigate(ContainerFragmentDirections.actionContainerFragmentToWidgetResizeFragment())
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