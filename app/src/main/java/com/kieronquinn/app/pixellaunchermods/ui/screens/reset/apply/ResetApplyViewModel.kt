package com.kieronquinn.app.pixellaunchermods.ui.screens.reset.apply

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.pixellaunchermods.components.navigation.ContainerNavigation
import com.kieronquinn.app.pixellaunchermods.repositories.RemoteAppsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

abstract class ResetApplyViewModel: ViewModel() {

    abstract val completeBus: Flow<Boolean>

    abstract fun reload()
    abstract fun close()

}

class ResetApplyViewModelImpl(
    private val remoteAppsRepository: RemoteAppsRepository,
    private val navigation: ContainerNavigation
): ResetApplyViewModel() {

    override val completeBus = remoteAppsRepository.resetAllApps()

    override fun reload() {
        viewModelScope.launch {
            remoteAppsRepository.forceReload()
        }
    }

    override fun close() {
        viewModelScope.launch {
            navigation.navigateBack()
        }
    }

}