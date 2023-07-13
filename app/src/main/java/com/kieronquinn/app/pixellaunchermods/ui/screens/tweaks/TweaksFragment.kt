package com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.ui.base.CanShowSnackbar
import com.kieronquinn.app.pixellaunchermods.ui.base.ProvidesOverflow
import com.kieronquinn.app.pixellaunchermods.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.pixellaunchermods.ui.base.settings.BaseSettingsFragment
import com.kieronquinn.app.pixellaunchermods.ui.base.settings.BaseSettingsViewModel.SettingsItem
import org.koin.androidx.viewmodel.ext.android.viewModel

class TweaksFragment: BaseSettingsFragment(), CanShowSnackbar, ProvidesOverflow {

    private val saveModuleLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("*/*")
    ) {
        if(it != null){
            viewModel.saveModule(it)
        }
    }

    override val viewModel by viewModel<TweaksViewModel>()

    override val items by lazy {
        listOf(
            SettingsItem.Text(
                icon = R.drawable.ic_tweaks_resize_widgets,
                titleRes = R.string.tweaks_resize_widgets_title,
                contentRes = R.string.tweaks_resize_widgets_content,
                onClick = viewModel::onWidgetResizeClicked
            ),
            SettingsItem.Text(
                icon = R.drawable.ic_tweaks_hide_apps,
                titleRes = R.string.tweaks_hide_apps,
                contentRes = R.string.tweaks_hide_apps_content,
                onClick = viewModel::onHideAppsClicked
            ),
            SettingsItem.Text(
                icon = R.drawable.ic_tweaks_widget_replacement,
                titleRes = R.string.tweaks_widget_replacement,
                contentRes = R.string.tweaks_widget_replacement_content,
                onClick = viewModel::onWidgetReplacementClicked
            ),
            SettingsItem.Text(
                icon = R.drawable.ic_tweaks_recents,
                titleRes = R.string.tweaks_overlay,
                contentRes = R.string.tweaks_overlay_content,
                onClick = viewModel::onRecentsClicked
            ),
            SettingsItem.Switch(
                icon = R.drawable.ic_tweaks_hide_clock,
                titleRes = R.string.tweaks_hide_clock,
                contentRes = R.string.tweaks_hide_clock_content,
                setting = viewModel.hideClock
            )
        )
    }

    override fun createAdapter(items: List<SettingsItem>): BaseSettingsAdapter {
        return TweaksAdapter()
    }

    override fun inflateMenu(menuInflater: MenuInflater, menu: Menu) {
        menuInflater.inflate(R.menu.menu_tweaks, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when(menuItem.itemId) {
            R.id.menu_tweaks_save_module -> {
                viewModel.onSaveModuleClicked(saveModuleLauncher)
            }
        }
        return true
    }

    private inner class TweaksAdapter: BaseSettingsAdapter(requireContext(), binding.settingsBaseRecyclerView, items)

}