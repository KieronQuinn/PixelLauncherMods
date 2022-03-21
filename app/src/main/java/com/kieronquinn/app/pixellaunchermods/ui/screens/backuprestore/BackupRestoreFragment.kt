package com.kieronquinn.app.pixellaunchermods.ui.screens.backuprestore

import androidx.activity.result.contract.ActivityResultContracts
import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.ui.base.BackAvailable
import com.kieronquinn.app.pixellaunchermods.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.pixellaunchermods.ui.base.settings.BaseSettingsFragment
import com.kieronquinn.app.pixellaunchermods.ui.base.settings.BaseSettingsViewModel.SettingsItem
import org.koin.androidx.viewmodel.ext.android.viewModel

class BackupRestoreFragment: BaseSettingsFragment(), BackAvailable {

    private val backupSelector = registerForActivityResult(ActivityResultContracts.CreateDocument()) {
        if(it != null){
            viewModel.onBackupSelected(it)
        }
    }

    private val restoreSelector = registerForActivityResult(ActivityResultContracts.OpenDocument()) {
        if(it != null){
            viewModel.onRestoreSelected(it)
        }
    }

    override val viewModel by viewModel<BackupRestoreViewModel>()

    override val items by lazy {
        listOf(
            SettingsItem.Text(
                icon = R.drawable.ic_backup_restore_backup,
                titleRes = R.string.options_backup_restore_backup,
                contentRes = R.string.options_backup_restore_backup_content,
                onClick = { viewModel.onBackupClicked(backupSelector) }
            ),
            SettingsItem.Text(
                icon = R.drawable.ic_backup_restore_restore,
                titleRes = R.string.options_backup_restore_restore,
                contentRes = R.string.options_backup_restore_restore_content,
                onClick = { viewModel.onRestoreClicked(restoreSelector) }
            )
        )
    }

    override fun createAdapter(items: List<SettingsItem>): BaseSettingsAdapter {
        return BackupRestoreAdapter()
    }

    private inner class BackupRestoreAdapter:
        BaseSettingsAdapter(requireContext(), binding.settingsBaseRecyclerView, items)

}