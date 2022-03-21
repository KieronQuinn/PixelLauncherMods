package com.kieronquinn.app.pixellaunchermods.ui.screens.options

import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.ui.base.CanShowSnackbar
import com.kieronquinn.app.pixellaunchermods.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.pixellaunchermods.ui.base.settings.BaseSettingsFragment
import com.kieronquinn.app.pixellaunchermods.ui.base.settings.BaseSettingsViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class OptionsFragment: BaseSettingsFragment(), CanShowSnackbar {

    override val viewModel by viewModel<OptionsViewModel>()

    override val items by lazy {
        listOf(
            BaseSettingsViewModel.SettingsItem.Text(
                icon = R.drawable.ic_tweaks_deferred_restart,
                titleRes = R.string.tweaks_deferred_restart,
                contentRes = R.string.tweaks_deferred_restart_content,
                onClick = viewModel::onDeferredRestartClicked
            ),
            BaseSettingsViewModel.SettingsItem.Text(
                icon = R.drawable.ic_options_backup_restore_backup,
                titleRes = R.string.options_backup_restore,
                contentRes = R.string.options_backup_restore_content,
                onClick = viewModel::onBackupRestoreClicked
            ),
            BaseSettingsViewModel.SettingsItem.Text(
                icon = R.drawable.ic_options_reapply,
                titleRes = R.string.options_reapply_icons,
                contentRes = R.string.options_reapply_icons_content,
                onClick = viewModel::onReapplyClicked
            ),
            BaseSettingsViewModel.SettingsItem.Text(
                icon = R.drawable.ic_options_advanced,
                titleRes = R.string.options_advanced,
                contentRes = R.string.options_advanced_content,
                onClick = viewModel::onAdvancedClicked
            ),
            BaseSettingsViewModel.SettingsItem.Text(
                icon = R.drawable.ic_faq,
                titleRes = R.string.options_faq,
                contentRes = R.string.options_faq_content,
                onClick = viewModel::onFaqClicked
            ),
            BaseSettingsViewModel.SettingsItem.About(
                onContributorsClicked = viewModel::onAboutContributorsClicked,
                onDonateClicked = viewModel::onAboutDonateClicked,
                onGitHubClicked = viewModel::onAboutGitHubClicked,
                onLibrariesClicked = viewModel::onAboutLibrariesClicked,
                onTwitterClicked = viewModel::onAboutTwitterClicked,
                onXdaClicked = viewModel::onAboutXDAClicked
            )
        )
    }

    override fun createAdapter(items: List<BaseSettingsViewModel.SettingsItem>): BaseSettingsAdapter {
        return OptionsAdapter()
    }

    private inner class OptionsAdapter: BaseSettingsAdapter(requireContext(), binding.settingsBaseRecyclerView, items)

}