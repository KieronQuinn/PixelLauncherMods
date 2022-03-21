package com.kieronquinn.app.pixellaunchermods.ui.screens.backuprestore

import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.pixellaunchermods.components.navigation.ContainerNavigation
import com.kieronquinn.app.pixellaunchermods.repositories.BackupRestoreRepository
import com.kieronquinn.app.pixellaunchermods.ui.base.settings.BaseSettingsViewModel
import kotlinx.coroutines.launch

abstract class BackupRestoreViewModel: BaseSettingsViewModel() {

    abstract fun onBackupClicked(launcher: ActivityResultLauncher<String>)
    abstract fun onRestoreClicked(launcher: ActivityResultLauncher<Array<String>>)

    abstract fun onBackupSelected(uri: Uri)
    abstract fun onRestoreSelected(uri: Uri)

}

class BackupRestoreViewModelImpl(
    private val backupRestoreRepository: BackupRestoreRepository,
    private val navigation: ContainerNavigation
): BackupRestoreViewModel() {

    override fun onBackupClicked(launcher: ActivityResultLauncher<String>) {
        launcher.launch(backupRestoreRepository.generateFilename())
    }

    override fun onBackupSelected(uri: Uri) {
        viewModelScope.launch {
            navigation.navigate(
                BackupRestoreFragmentDirections.actionBackupRestoreFragmentToBackupRestoreBackupFragment(uri)
            )
        }
    }

    override fun onRestoreClicked(launcher: ActivityResultLauncher<Array<String>>) {
        launcher.launch(backupRestoreRepository.getMimeTypes())
    }

    override fun onRestoreSelected(uri: Uri) {
        viewModelScope.launch {
            navigation.navigate(
                BackupRestoreFragmentDirections.actionBackupRestoreFragmentToBackupRestoreRestoreFragment(uri)
            )
        }
    }

}