package com.kieronquinn.app.pixellaunchermods.ui.screens.options.reapply

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.pixellaunchermods.components.navigation.ContainerNavigation
import com.kieronquinn.app.pixellaunchermods.repositories.RemoteAppsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class OptionsReapplyViewModel: ViewModel() {

    abstract val finishedBus: Flow<Unit>

}

class OptionsReapplyViewModelImpl(
    private val remoteAppsRepository: RemoteAppsRepository,
    private val navigation: ContainerNavigation
): OptionsReapplyViewModel() {

    override val finishedBus = MutableSharedFlow<Unit>()

    private fun applyIcons() = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            remoteAppsRepository.getRemoteApps(true)
        }
        finishedBus.emit(Unit)
        navigation.navigateBack()
    }

    init {
        applyIcons()
    }

}