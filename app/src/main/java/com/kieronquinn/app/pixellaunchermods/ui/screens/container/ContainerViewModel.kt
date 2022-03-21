package com.kieronquinn.app.pixellaunchermods.ui.screens.container

import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.components.navigation.ContainerNavigation
import com.kieronquinn.app.pixellaunchermods.repositories.RemoteAppsRepository
import com.kieronquinn.app.pixellaunchermods.repositories.RemoteAppsRepository.IconApplyProgress
import com.kieronquinn.app.pixellaunchermods.repositories.UpdateRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

abstract class ContainerViewModel: ViewModel() {

    abstract val showUpdateSnackbar: StateFlow<Boolean>
    abstract val iconApplyProgress: StateFlow<IconApplyProgress?>
    abstract fun onBackPressed()
    abstract fun setCanShowSnackbar(showSnackbar: Boolean)
    abstract fun onUpdateClicked()
    abstract fun onUpdateDismissed()

}

class ContainerViewModelImpl(
    private val navigation: ContainerNavigation,
    remoteAppsRepository: RemoteAppsRepository,
    updateRepository: UpdateRepository
): ContainerViewModel() {

    private val canShowSnackbar = MutableStateFlow(false)
    private val hasDismissedSnackbar = MutableStateFlow(false)

    private val gitHubUpdate = flow {
        emit(updateRepository.getUpdate())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    override val iconApplyProgress = remoteAppsRepository.iconApplyProgress.map {
        it.filter()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, remoteAppsRepository.iconApplyProgress.value.filter())

    override val showUpdateSnackbar = combine(canShowSnackbar, gitHubUpdate, hasDismissedSnackbar){ canShow, update, dismissed ->
        canShow && update != null && !dismissed
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /**
     *  The UI should not be blocked for icons applying, only for icon packs (when not triggered
     *  by the UI) and config changes
     */
    private fun IconApplyProgress?.filter(): IconApplyProgress? {
        return when (this) {
            is IconApplyProgress.ApplyingIcons -> null
            is IconApplyProgress.UpdatingIconPacks -> if(this.fromUI) null else this
            else -> this
        }
    }

    override fun onBackPressed() {
        viewModelScope.launch {
            navigation.navigateBack()
        }
    }

    override fun setCanShowSnackbar(showSnackbar: Boolean) {
        viewModelScope.launch {
            canShowSnackbar.emit(showSnackbar)
        }
    }

    override fun onUpdateClicked() {
        val release = gitHubUpdate.value ?: return
        viewModelScope.launch {
            navigation.navigate(R.id.action_global_settingsUpdateFragment, bundleOf("release" to release))
        }
    }

    override fun onUpdateDismissed() {
        viewModelScope.launch {
            hasDismissedSnackbar.emit(true)
        }
    }

}