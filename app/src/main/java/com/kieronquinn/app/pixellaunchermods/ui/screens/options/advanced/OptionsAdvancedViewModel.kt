package com.kieronquinn.app.pixellaunchermods.ui.screens.options.advanced

import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.pixellaunchermods.components.navigation.ContainerNavigation
import com.kieronquinn.app.pixellaunchermods.repositories.RootServiceRepository
import com.kieronquinn.app.pixellaunchermods.repositories.SettingsRepository
import com.kieronquinn.app.pixellaunchermods.ui.base.settings.BaseSettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

abstract class OptionsAdvancedViewModel: BaseSettingsViewModel() {

    abstract val restartBus: Flow<Unit>
    abstract val suppressShortcutSetting: SettingsRepository.PixelLauncherModsSetting<Boolean>

    abstract fun onRestartClicked()
    abstract fun onResetClicked()

}

class OptionsAdvancedViewModelImpl(
    private val rootServiceRepository: RootServiceRepository,
    private val navigation: ContainerNavigation,
    settingsRepository: SettingsRepository
): OptionsAdvancedViewModel() {

    override val restartBus = MutableSharedFlow<Unit>()
    override val suppressShortcutSetting = settingsRepository.suppressShortcutChangeListener

    override fun onRestartClicked() {
        viewModelScope.launch(Dispatchers.IO) {
            rootServiceRepository.runWithRootService {
                it.restartLauncherImmediately()
                restartBus.emit(Unit)
            }
        }
    }

    override fun onResetClicked() {
        viewModelScope.launch {
            navigation.navigate(OptionsAdvancedFragmentDirections.actionOptionsAdvancedFragmentToResetInfoFragment())
        }
    }

}