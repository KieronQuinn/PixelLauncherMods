package com.kieronquinn.app.pixellaunchermods.ui.screens.options.contributors

import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.ui.base.BackAvailable
import com.kieronquinn.app.pixellaunchermods.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.pixellaunchermods.ui.base.settings.BaseSettingsFragment
import com.kieronquinn.app.pixellaunchermods.ui.base.settings.BaseSettingsViewModel.SettingsItem
import com.kieronquinn.app.pixellaunchermods.utils.extensions.getResourceIdArray
import org.koin.androidx.viewmodel.ext.android.viewModel

class ContributorsFragment: BaseSettingsFragment(), BackAvailable {

    override val viewModel by viewModel<ContributorsViewModel>()

    override val items by lazy {
        listOf(
            SettingsItem.Text(
                icon = R.drawable.ic_contributions_icons,
                titleRes = R.string.about_contributors_icons,
                contentRes = R.string.about_contributors_icons_content,
                linkClicked = viewModel::onLinkClicked
            ),
            SettingsItem.Text(
                icon = R.drawable.ic_contributions_build,
                titleRes = R.string.about_contributors_overlay,
                contentRes = R.string.about_contributors_overlay_content,
                linkClicked = viewModel::onLinkClicked
            ),
            SettingsItem.Header(R.string.contributors_translators),
            *getTranslatorsList().toTypedArray()
        )
    }

    override fun createAdapter(items: List<SettingsItem>): BaseSettingsAdapter {
        return ContributorsAdapter()
    }

    private fun getTranslatorsList(): List<SettingsItem.Text> {
        val headings = resources.getResourceIdArray(R.array.about_translators_headings)
        val content = resources.getResourceIdArray(R.array.about_translators_content)
        val flags = resources.getResourceIdArray(R.array.about_translators_flags)
        return headings.mapIndexed { index, resource ->
            SettingsItem.Text(
                icon = flags[index],
                titleRes = resource,
                contentRes = content[index]
            )
        }
    }

    inner class ContributorsAdapter: BaseSettingsAdapter(requireContext(), binding.settingsBaseRecyclerView, items)

}