package com.kieronquinn.app.pixellaunchermods.ui.screens.backuprestore.restore

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.pixellaunchermods.components.navigation.ContainerNavigation
import com.kieronquinn.app.pixellaunchermods.model.tweaks.ParceledWidgetReplacement
import com.kieronquinn.app.pixellaunchermods.repositories.BackupRestoreRepository
import com.kieronquinn.app.pixellaunchermods.repositories.BackupRestoreRepository.OverlayAction
import com.kieronquinn.app.pixellaunchermods.repositories.BackupRestoreRepository.RestoreResult
import com.kieronquinn.app.pixellaunchermods.repositories.OverlayRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class BackupRestoreRestoreViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun setRestoreUri(uri: Uri)
    abstract fun onCloseClicked()
    abstract fun onRestoreTweaksClicked()
    abstract fun onMagiskClicked()
    abstract fun onIssuesClicked()

    sealed class State {
        object RestoringBackup: State()
        data class Finished(val result: RestoreResult?, val overlayInstalled: Boolean): State()
    }

}

class BackupRestoreRestoreViewModelImpl(
    backupRestoreRepository: BackupRestoreRepository,
    private val overlayRepository: OverlayRepository,
    private val navigation: ContainerNavigation
): BackupRestoreRestoreViewModel() {

    private val restoreUri = MutableStateFlow<Uri?>(null)

    override val state = restoreUri.filterNotNull().map {
        val isOverlayInstalled = overlayRepository.isOverlayInstalled()
        State.Finished(backupRestoreRepository.restoreBackup(it), isOverlayInstalled)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.RestoringBackup)

    override fun setRestoreUri(uri: Uri) {
        viewModelScope.launch {
            restoreUri.emit(uri)
        }
    }

    override fun onCloseClicked() {
        viewModelScope.launch {
            navigation.navigateBack()
        }
    }

    override fun onRestoreTweaksClicked() {
        val actions = (state.value as? State.Finished)?.result?.overlayActions ?: return
        viewModelScope.launch {
            val components = actions.firstOrNull {
                it is OverlayAction.CommitHiddenApps
            } as? OverlayAction.CommitHiddenApps
            val widgetReplacement = actions.firstOrNull {
                it is OverlayAction.CommitWidgetReplacement
            } as? OverlayAction.CommitWidgetReplacement
            val overlayTweaks = actions.firstOrNull {
                it is OverlayAction.CommitOverlayTweaks
            } as? OverlayAction.CommitOverlayTweaks
            navigation.navigate(
                BackupRestoreRestoreFragmentDirections.actionBackupRestoreRestoreFragmentToOverlayApplyFragment(
                    components?.components?.toTypedArray(), widgetReplacement?.widgetReplacement?.run {
                        ParceledWidgetReplacement(this)
                    },
                    overlayTweaks?.transparency?.toString(),
                    overlayTweaks?.disableWallpaperScrim?.toString(),
                    overlayTweaks?.disableWallpaperRegionColours?.toString(),
                    overlayTweaks?.disableSmartspace?.toString()
                )
            )
        }
    }

    override fun onMagiskClicked() {
        viewModelScope.launch {
            navigation.navigate(BackupRestoreRestoreFragmentDirections.actionBackupRestoreRestoreFragmentToMagiskInfoFragment())
        }
    }

    override fun onIssuesClicked() {
        val issues = (state.value as? State.Finished)?.result?.restoreIssues ?: return
        viewModelScope.launch {
            navigation.navigate(BackupRestoreRestoreFragmentDirections.actionBackupRestoreRestoreFragmentToBackupRestoreIssuesFragment(
                issues.toTypedArray())
            )
        }
    }

}