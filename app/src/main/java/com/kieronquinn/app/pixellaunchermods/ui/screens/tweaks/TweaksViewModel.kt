package com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks

import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.pixellaunchermods.components.navigation.ContainerNavigation
import com.kieronquinn.app.pixellaunchermods.components.navigation.RootNavigation
import com.kieronquinn.app.pixellaunchermods.repositories.SettingsRepository
import com.kieronquinn.app.pixellaunchermods.ui.base.settings.BaseSettingsViewModel
import com.kieronquinn.app.pixellaunchermods.ui.screens.container.ContainerFragmentDirections
import kotlinx.coroutines.launch

abstract class TweaksViewModel: BaseSettingsViewModel() {

    abstract val hideClock: SettingsRepository.PixelLauncherModsSetting<Boolean>

    abstract fun onWidgetResizeClicked()
    abstract fun onWidgetReplacementClicked()
    abstract fun onHideAppsClicked()

}

class TweaksViewModelImpl(
    private val rootNavigation: RootNavigation,
    private val containerNavigation: ContainerNavigation,
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

}