package com.kieronquinn.app.pixellaunchermods.ui.screens.reset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.pixellaunchermods.components.navigation.ContainerNavigation
import com.kieronquinn.app.pixellaunchermods.repositories.RemoteAppsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class ResetInfoViewModel: ViewModel() {

    abstract val state: StateFlow<State>
    abstract fun onContinueClicked()

    sealed class State {
        object Loading: State()
        data class Loaded(val legacyShortcutName: String?): State()
    }

}

class ResetInfoViewModelImpl(
    private val remoteAppsRepository: RemoteAppsRepository,
    private val navigation: ContainerNavigation
): ResetInfoViewModel() {

    override val state = flow {
        val firstLabel = remoteAppsRepository.getRemoteShortcuts().firstOrNull {
            it is RemoteAppsRepository.Shortcut.LegacyShortcut && it.label != null
        }?.label
        emit(State.Loaded(firstLabel))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun onContinueClicked() {
        viewModelScope.launch {
            navigation.navigate(ResetInfoFragmentDirections.actionResetInfoFragmentToResetApplyFragment())
        }
    }

}