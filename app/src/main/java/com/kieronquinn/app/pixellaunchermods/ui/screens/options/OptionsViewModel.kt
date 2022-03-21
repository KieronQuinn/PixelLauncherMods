package com.kieronquinn.app.pixellaunchermods.ui.screens.options

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.pixellaunchermods.components.navigation.ContainerNavigation
import com.kieronquinn.app.pixellaunchermods.ui.base.settings.BaseSettingsViewModel
import kotlinx.coroutines.launch

abstract class OptionsViewModel: BaseSettingsViewModel() {

    abstract fun onDeferredRestartClicked()
    abstract fun onBackupRestoreClicked()
    abstract fun onReapplyClicked()
    abstract fun onAdvancedClicked()
    abstract fun onFaqClicked()
    abstract fun onAboutContributorsClicked()
    abstract fun onAboutDonateClicked()
    abstract fun onAboutGitHubClicked()
    abstract fun onAboutTwitterClicked()
    abstract fun onAboutXDAClicked()
    abstract fun onAboutLibrariesClicked()

}

class OptionsViewModelImpl(
    private val navigation: ContainerNavigation
): OptionsViewModel() {

    companion object {
        private const val LINK_TWITTER = "https://kieronquinn.co.uk/redirect/plm/twitter"
        private const val LINK_GITHUB = "https://kieronquinn.co.uk/redirect/plm/github"
        private const val LINK_XDA = "https://kieronquinn.co.uk/redirect/plm/xda"
        private const val LINK_DONATE = "https://kieronquinn.co.uk/redirect/plm/donate"
    }

    override fun onDeferredRestartClicked() {
        viewModelScope.launch {
            navigation.navigate(OptionsFragmentDirections.actionOptionsFragmentToDeferredRestartFragment())
        }
    }

    override fun onBackupRestoreClicked() {
        viewModelScope.launch {
            navigation.navigate(OptionsFragmentDirections.actionOptionsFragmentToBackupRestoreFragment())
        }
    }

    override fun onReapplyClicked() {
        viewModelScope.launch {
            navigation.navigate(OptionsFragmentDirections.actionOptionsFragmentToOptionsReapplyFragment())
        }
    }

    override fun onFaqClicked() {
        viewModelScope.launch {
            navigation.navigate(OptionsFragmentDirections.actionOptionsFragmentToOptionsFaqFragment())
        }
    }

    override fun onAdvancedClicked() {
        viewModelScope.launch {
            navigation.navigate(OptionsFragmentDirections.actionOptionsFragmentToOptionsAdvancedFragment())
        }
    }

    override fun onAboutContributorsClicked() {
        viewModelScope.launch {
            navigation.navigate(OptionsFragmentDirections.actionOptionsFragmentToContributorsFragment())
        }
    }

    override fun onAboutDonateClicked() {
        viewModelScope.launch {
            navigation.navigate(Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(LINK_DONATE)
            })
        }
    }

    override fun onAboutGitHubClicked() {
        viewModelScope.launch {
            navigation.navigate(Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(LINK_GITHUB)
            })
        }
    }

    override fun onAboutLibrariesClicked() {
        viewModelScope.launch {
            navigation.navigate(OptionsFragmentDirections.actionOptionsFragmentToOssLicensesMenuActivity())
        }
    }

    override fun onAboutTwitterClicked() {
        viewModelScope.launch {
            navigation.navigate(Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(LINK_TWITTER)
            })
        }
    }

    override fun onAboutXDAClicked() {
        viewModelScope.launch {
            navigation.navigate(Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(LINK_XDA)
            })
        }
    }

}