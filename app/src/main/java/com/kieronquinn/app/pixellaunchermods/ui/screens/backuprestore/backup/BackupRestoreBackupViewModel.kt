package com.kieronquinn.app.pixellaunchermods.ui.screens.backuprestore.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.pixellaunchermods.components.navigation.ContainerNavigation
import com.kieronquinn.app.pixellaunchermods.repositories.BackupRestoreRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

abstract class BackupRestoreBackupViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun setBackupUri(uri: Uri)
    abstract fun onCloseClicked()

    sealed class State {
        object BackingUp: State()
        data class Finished(val success: Boolean): State()
    }

}

class BackupRestoreBackupViewModelImpl(
    backupRestoreRepository: BackupRestoreRepository,
    private val navigation: ContainerNavigation
): BackupRestoreBackupViewModel() {

    private val backupUri = MutableStateFlow<Uri?>(null)

    override val state = backupUri.filterNotNull().map {
        State.Finished(backupRestoreRepository.createBackup(it))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.BackingUp)

    override fun setBackupUri(uri: Uri) {
        viewModelScope.launch {
            backupUri.emit(uri)
        }
    }

    override fun onCloseClicked() {
        viewModelScope.launch {
            navigation.navigateBack()
        }
    }

}