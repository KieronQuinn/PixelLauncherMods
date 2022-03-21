package com.kieronquinn.app.pixellaunchermods.ui.screens.update

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.pixellaunchermods.BuildConfig
import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.databinding.FragmentSettingsUpdateBinding
import com.kieronquinn.app.pixellaunchermods.ui.base.BackAvailable
import com.kieronquinn.app.pixellaunchermods.ui.base.BoundFragment
import com.kieronquinn.app.pixellaunchermods.ui.screens.update.SettingsUpdateViewModel.State
import com.kieronquinn.app.pixellaunchermods.utils.extensions.applyBottomNavigationInset
import com.kieronquinn.app.pixellaunchermods.utils.extensions.applyBottomNavigationMargin
import com.kieronquinn.app.pixellaunchermods.utils.extensions.onClicked
import io.noties.markwon.Markwon
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.math.roundToInt

class SettingsUpdateFragment: BoundFragment<FragmentSettingsUpdateBinding>(FragmentSettingsUpdateBinding::inflate), BackAvailable {

    private val viewModel by viewModel<SettingsUpdateViewModel>()
    private val args by navArgs<SettingsUpdateFragmentArgs>()
    private val markwon by inject<Markwon>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
        setupStartInstall()
        setupGitHubButton()
        setupFabState()
        setupFabClick()
        setupInsets()
        viewModel.setupWithRelease(args.release)
    }

    private fun setupInsets() {
        binding.settingsUpdateInfo.applyBottomNavigationInset(resources.getDimension(R.dimen.margin_16))
        binding.settingsUpdateFab.applyBottomNavigationMargin()
    }

    private fun setupStartInstall() = viewLifecycleOwner.lifecycleScope.launchWhenResumed {
        binding.settingsUpdateStartInstall.onClicked().collect {
            viewModel.startInstall()
        }
    }

    private fun setupGitHubButton() = viewLifecycleOwner.lifecycleScope.launchWhenResumed {
        binding.settingsUpdateDownloadBrowser.onClicked().collect {
            viewModel.onDownloadBrowserClicked(args.release.gitHubUrl)
        }
    }

    private fun setupState() {
        handleState(viewModel.state.value)
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            viewModel.state.collect {
                handleState(it)
            }
        }
    }

    private fun handleState(state: State){
        when(state){
            is State.Loading -> setupWithLoading()
            is State.Info -> setupWithInfo(state)
            is State.StartDownload -> setupWithStartDownload()
            is State.Downloading -> setupWithDownloading(state)
            is State.Done, is State.StartInstall -> setupWithDone()
            is State.Failed -> setupWithFailed()
        }
    }

    private fun setupWithLoading() {
        binding.settingsUpdateInfo.isVisible = false
        binding.settingsUpdateProgress.isVisible = false
        binding.settingsUpdateProgressIndeterminate.isVisible = true
        binding.settingsUpdateTitle.isVisible = true
        binding.settingsUpdateIcon.isVisible = false
        binding.settingsUpdateStartInstall.isVisible = false
        binding.settingsUpdateTitle.setText(R.string.settings_update_loading)
    }

    private fun setupWithInfo(info: State.Info){
        val release = info.release
        binding.settingsUpdateInfo.isVisible = true
        binding.settingsUpdateProgress.isVisible = false
        binding.settingsUpdateProgressIndeterminate.isVisible = false
        binding.settingsUpdateTitle.isVisible = false
        binding.settingsUpdateIcon.isVisible = false
        binding.settingsUpdateStartInstall.isVisible = false
        binding.settingsUpdateHeading.text = getString(R.string.settings_update_heading, release.versionName)
        binding.settingsUpdateSubheading.text = getString(R.string.settings_update_subheading, BuildConfig.VERSION_NAME)
        binding.settingsUpdateBody.text = markwon.toMarkdown(release.body)
        binding.settingsUpdateInfo.applyBottomNavigationInset()
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            binding.settingsUpdateDownloadBrowser.onClicked().collect {
                viewModel.onDownloadBrowserClicked(release.gitHubUrl)
            }
        }
    }

    private fun setupWithStartDownload() {
        binding.settingsUpdateInfo.isVisible = false
        binding.settingsUpdateProgress.isVisible = false
        binding.settingsUpdateProgressIndeterminate.isVisible = true
        binding.settingsUpdateTitle.isVisible = true
        binding.settingsUpdateIcon.isVisible = false
        binding.settingsUpdateStartInstall.isVisible = false
        binding.settingsUpdateTitle.setText(R.string.update_downloader_downloading_title)
    }

    private fun setupWithDownloading(state: State.Downloading) {
        binding.settingsUpdateInfo.isVisible = false
        binding.settingsUpdateProgress.isVisible = true
        binding.settingsUpdateProgressIndeterminate.isVisible = false
        binding.settingsUpdateTitle.isVisible = true
        binding.settingsUpdateIcon.isVisible = false
        binding.settingsUpdateStartInstall.isVisible = false
        binding.settingsUpdateProgress.progress = (state.progress * 100).roundToInt()
        binding.settingsUpdateTitle.setText(R.string.update_downloader_downloading_title)
    }

    private fun setupWithDone() {
        binding.settingsUpdateInfo.isVisible = false
        binding.settingsUpdateProgress.isVisible = false
        binding.settingsUpdateProgressIndeterminate.isVisible = false
        binding.settingsUpdateTitle.isVisible = true
        binding.settingsUpdateIcon.isVisible = true
        binding.settingsUpdateStartInstall.isVisible = true
        binding.settingsUpdateTitle.setText(R.string.settings_update_done)
        binding.settingsUpdateIcon.setImageResource(R.drawable.ic_update_download_done)
    }

    private fun setupWithFailed() {
        binding.settingsUpdateInfo.isVisible = false
        binding.settingsUpdateProgress.isVisible = false
        binding.settingsUpdateProgressIndeterminate.isVisible = false
        binding.settingsUpdateTitle.isVisible = true
        binding.settingsUpdateIcon.isVisible = true
        binding.settingsUpdateStartInstall.isVisible = true
        binding.settingsUpdateTitle.setText(R.string.update_downloader_downloading_failed)
        binding.settingsUpdateIcon.setImageResource(R.drawable.ic_error_circle)
    }

    private fun setupFabState() {
        handleFabState(viewModel.showFab.value)
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            viewModel.showFab.collect {
                handleFabState(it)
            }
        }
    }

    private fun handleFabState(showFab: Boolean){
        binding.settingsUpdateFab.isVisible = showFab
    }

    private fun setupFabClick() = viewLifecycleOwner.lifecycleScope.launchWhenResumed {
        binding.settingsUpdateFab.onClicked().collect {
            viewModel.startDownload()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onPause() {
        viewModel.onPause()
        super.onPause()
    }

}