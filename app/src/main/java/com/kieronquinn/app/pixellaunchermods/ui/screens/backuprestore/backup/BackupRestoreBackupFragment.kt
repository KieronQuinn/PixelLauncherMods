package com.kieronquinn.app.pixellaunchermods.ui.screens.backuprestore.backup

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.databinding.FragmentBackupRestoreBackupBinding
import com.kieronquinn.app.pixellaunchermods.ui.base.BackAvailable
import com.kieronquinn.app.pixellaunchermods.ui.base.BoundFragment
import com.kieronquinn.app.pixellaunchermods.ui.screens.backuprestore.backup.BackupRestoreBackupViewModel.State
import com.kieronquinn.app.pixellaunchermods.utils.extensions.applyBottomNavigationInset
import com.kieronquinn.app.pixellaunchermods.utils.extensions.onClicked
import org.koin.androidx.viewmodel.ext.android.viewModel

class BackupRestoreBackupFragment: BoundFragment<FragmentBackupRestoreBackupBinding>(FragmentBackupRestoreBackupBinding::inflate), BackAvailable {

    private val viewModel by viewModel<BackupRestoreBackupViewModel>()
    private val args by navArgs<BackupRestoreBackupFragmentArgs>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
        setupClose()
        binding.root.applyBottomNavigationInset(resources.getDimension(R.dimen.margin_16))
        viewModel.setBackupUri(args.uri)
    }

    private fun setupState() {
        handleState(viewModel.state.value)
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            viewModel.state.collect {
                handleState(it)
            }
        }
    }

    private fun handleState(state: State) {
        when(state){
            is State.BackingUp -> {
                binding.backupRestoreBackupProgress.isVisible = true
                binding.backupRestoreBackupClose.isVisible = false
                binding.backupRestoreBackupIcon.isVisible = false
                binding.backupRestoreBackupTitle.setText(R.string.backup_restore_backup_creating)
            }
            is State.Finished -> {
                binding.backupRestoreBackupProgress.isVisible = false
                binding.backupRestoreBackupClose.isVisible = true
                binding.backupRestoreBackupIcon.isVisible = true
                if(state.success){
                    binding.backupRestoreBackupTitle.setText(R.string.backup_restore_backup_creating_success)
                    binding.backupRestoreBackupIcon.setImageResource(R.drawable.ic_check_circle)
                }else{
                    binding.backupRestoreBackupTitle.setText(R.string.backup_restore_backup_creating_failed)
                    binding.backupRestoreBackupIcon.setImageResource(R.drawable.ic_error_circle)
                }
            }
        }
    }

    private fun setupClose() = viewLifecycleOwner.lifecycleScope.launchWhenResumed {
        binding.backupRestoreBackupClose.onClicked().collect {
            viewModel.onCloseClicked()
        }
    }

}