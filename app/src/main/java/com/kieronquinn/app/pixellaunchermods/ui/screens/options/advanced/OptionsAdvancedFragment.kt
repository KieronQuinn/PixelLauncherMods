package com.kieronquinn.app.pixellaunchermods.ui.screens.options.advanced

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.ui.base.BackAvailable
import com.kieronquinn.app.pixellaunchermods.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.pixellaunchermods.ui.base.settings.BaseSettingsFragment
import com.kieronquinn.app.pixellaunchermods.ui.base.settings.BaseSettingsViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class OptionsAdvancedFragment: BaseSettingsFragment(), BackAvailable {

    override val viewModel by viewModel<OptionsAdvancedViewModel>()

    override val items by lazy {
        listOf(
            BaseSettingsViewModel.SettingsItem.Text(
                icon = R.drawable.ic_options_advanced_restart,
                titleRes = R.string.options_advanced_restart,
                contentRes = R.string.options_advanced_restart_content,
                onClick = viewModel::onRestartClicked
            ),
            BaseSettingsViewModel.SettingsItem.Switch(
                icon = R.drawable.ic_options_advanced_shortcut,
                titleRes = R.string.options_advanced_shortcut,
                contentRes = R.string.options_advanced_shortcut_content,
                setting = viewModel.suppressShortcutSetting
            ),
            BaseSettingsViewModel.SettingsItem.Text(
                icon = R.drawable.ic_options_advanced_reset,
                titleRes = R.string.options_advanced_reset,
                contentRes = R.string.options_advanced_reset_content,
                onClick = viewModel::onResetClicked
            )
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRestartBus()
    }

    private fun setupRestartBus() = viewLifecycleOwner.lifecycleScope.launchWhenResumed {
        viewModel.restartBus.collect {
            Toast.makeText(requireContext(), R.string.options_advanced_restart_finished, Toast.LENGTH_LONG).show()
        }
    }

    override fun createAdapter(items: List<BaseSettingsViewModel.SettingsItem>): BaseSettingsAdapter {
        return OptionsAdvancedAdapter()
    }

    private inner class OptionsAdvancedAdapter: BaseSettingsAdapter(requireContext(), binding.settingsBaseRecyclerView, items)

}