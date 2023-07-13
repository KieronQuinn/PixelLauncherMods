package com.kieronquinn.app.pixellaunchermods.ui.screens.backuprestore.restore

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.databinding.FragmentBackupRestoreRestoreBinding
import com.kieronquinn.app.pixellaunchermods.ui.base.BackAvailable
import com.kieronquinn.app.pixellaunchermods.ui.base.BoundFragment
import com.kieronquinn.app.pixellaunchermods.ui.screens.backuprestore.restore.BackupRestoreRestoreViewModel.State
import com.kieronquinn.app.pixellaunchermods.utils.extensions.applyBottomNavigationInset
import com.kieronquinn.app.pixellaunchermods.utils.extensions.onClicked
import org.koin.androidx.viewmodel.ext.android.viewModel

class BackupRestoreRestoreFragment: BoundFragment<FragmentBackupRestoreRestoreBinding>(FragmentBackupRestoreRestoreBinding::inflate), BackAvailable {

    private val viewModel by viewModel<BackupRestoreRestoreViewModel>()
    private val args by navArgs<BackupRestoreRestoreFragmentArgs>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
        setupClose()
        setupRestoreTweaks()
        setupMagisk()
        setupIssues()
        binding.root.applyBottomNavigationInset(resources.getDimension(R.dimen.margin_16))
        viewModel.setRestoreUri(args.uri)
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
            is State.RestoringBackup -> {
                binding.backupRestoreRestoreProgress.isVisible = true
                binding.backupRestoreRestoreClose.isVisible = false
                binding.backupRestoreRestoreIcon.isVisible = false
                binding.backupRestoreRestoreIssues.isVisible = false
                binding.backupRestoreRestoreMagisk.isVisible = false
                binding.backupRestoreRestoreTweaks.isVisible = false
                binding.backupRestoreRestoreTitle.setText(R.string.backup_restore_restore_restoring)
            }
            is State.Finished -> {
                binding.backupRestoreRestoreProgress.isVisible = false
                binding.backupRestoreRestoreClose.isVisible = true
                binding.backupRestoreRestoreIcon.isVisible = true
                if(state.result != null){
                    val hasIssues = state.result.restoreIssues.isNotEmpty()
                    val hasActions = state.result.overlayActions.any { it.isValid() }
                    val issueCount = state.result.restoreIssues.size
                    binding.backupRestoreRestoreTitle.setText(R.string.backup_restore_restore_restoring_success)
                    binding.backupRestoreRestoreIcon.setImageResource(R.drawable.ic_check_circle)
                    binding.backupRestoreRestoreIssues.isVisible = hasIssues
                    binding.backupRestoreRestoreIssuesTitle.text = getString(
                        R.string.backup_restore_restore_issues_title, issueCount
                    )
                    binding.backupRestoreRestoreIssuesContent.text = resources.getQuantityString(
                        R.plurals.backup_restore_restore_issues_content, issueCount, issueCount
                    )
                    binding.backupRestoreRestoreMagisk.isVisible = !state.overlayInstalled && hasActions
                    binding.backupRestoreRestoreTweaks.isVisible = state.overlayInstalled && hasActions
                }else{
                    binding.backupRestoreRestoreTitle.setText(R.string.backup_restore_restore_restoring_failed)
                    binding.backupRestoreRestoreIcon.setImageResource(R.drawable.ic_error_circle)
                    binding.backupRestoreRestoreIssues.isVisible = false
                    binding.backupRestoreRestoreMagisk.isVisible = false
                    binding.backupRestoreRestoreTweaks.isVisible = false
                }
            }
        }
    }

    private fun setupRestoreTweaks() = viewLifecycleOwner.lifecycleScope.launchWhenResumed {
        binding.backupRestoreRestoreTweaks.onClicked().collect {
            viewModel.onRestoreTweaksClicked()
        }
    }

    private fun setupMagisk() = viewLifecycleOwner.lifecycleScope.launchWhenResumed {
        binding.backupRestoreRestoreMagisk.onClicked().collect {
            viewModel.onMagiskClicked()
        }
    }

    private fun setupIssues() = viewLifecycleOwner.lifecycleScope.launchWhenResumed {
        binding.backupRestoreRestoreIssues.onClicked().collect {
            viewModel.onIssuesClicked()
        }
    }

    private fun setupClose() = viewLifecycleOwner.lifecycleScope.launchWhenResumed {
        binding.backupRestoreRestoreClose.onClicked().collect {
            viewModel.onCloseClicked()
        }
    }

}