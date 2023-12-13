package com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.overlay

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.databinding.FragmentSettingsTweaksOverlayBinding
import com.kieronquinn.app.pixellaunchermods.repositories.SettingsRepository.FakePixelLauncherModsSetting
import com.kieronquinn.app.pixellaunchermods.ui.base.BackAvailable
import com.kieronquinn.app.pixellaunchermods.ui.base.BoundFragment
import com.kieronquinn.app.pixellaunchermods.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.pixellaunchermods.ui.base.settings.BaseSettingsViewModel
import com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.overlay.OverlayTweaksViewModel.State
import com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.overlayapply.OverlayApplyFragment.Companion.REQUEST_KEY_TWEAKS
import com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.overlayapply.OverlayApplyFragment.Companion.RESULT_EXTRA_TWEAKS_WAS_SUCCESSFUL
import com.kieronquinn.app.pixellaunchermods.utils.extensions.applyBottomNavigationInset
import com.kieronquinn.app.pixellaunchermods.utils.extensions.applyBottomNavigationMargin
import com.kieronquinn.app.pixellaunchermods.utils.extensions.onClicked
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Locale

class OverlayTweaksFragment: BoundFragment<FragmentSettingsTweaksOverlayBinding>(FragmentSettingsTweaksOverlayBinding::inflate), BackAvailable {

    private val viewModel by viewModel<OverlayTweaksViewModel>()

    private val saveModuleLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("*/*")) {
        if(it != null){
            viewModel.saveModule(it)
        }
    }

    private val items by lazy {
        listOf(
            BaseSettingsViewModel.SettingsItem.Slider(
                icon = R.drawable.ic_tweaks_recents_transparency,
                titleRes = R.string.tweaks_overlay_background_transparency,
                contentRes = R.string.tweaks_overlay_background_transparency_content,
                min = 0f,
                max = 100f,
                setting = FakePixelLauncherModsSetting(
                    viewModel.transparency, viewModel::onTransparencyChanged
                ),
                labelFormatter = {
                    val value = String.format(Locale.getDefault(), "%.0f", it)
                    getString(R.string.tweaks_overlay_formatter, value)
                }
            ),
            BaseSettingsViewModel.SettingsItem.Switch(
                icon = R.drawable.ic_tweaks_wallpaper_scrim,
                titleRes = R.string.tweaks_overlay_wallpaper_scrim,
                contentRes = if(Build.VERSION.SDK_INT >= 34) {
                    R.string.tweaks_overlay_wallpaper_scrim_content_disabled
                }else{
                    R.string.tweaks_overlay_wallpaper_scrim_content
                },
                setting = FakePixelLauncherModsSetting(
                    viewModel.wallpaperScrim, viewModel::onWallpaperScrimChanged
                ),
                isEnabled = { Build.VERSION.SDK_INT < 34 }
            ),
            BaseSettingsViewModel.SettingsItem.Switch(
                icon = R.drawable.ic_tweaks_wallpaper_region_colours,
                titleRes = R.string.tweaks_overlay_wallpaper_region_colours,
                contentRes = R.string.tweaks_overlay_wallpaper_region_colours_content,
                setting = FakePixelLauncherModsSetting(
                    viewModel.wallpaperRegionColours, viewModel::onWallpaperRegionColoursChanged
                )
            ),
            BaseSettingsViewModel.SettingsItem.Switch(
                icon = R.drawable.ic_tweaks_smartspace,
                titleRes = R.string.tweaks_overlay_disable_smartspace,
                contentRes = R.string.tweaks_overlay_disable_smartspace_content,
                setting = FakePixelLauncherModsSetting(
                    viewModel.smartspace, viewModel::onDisableSmartspaceChanged
                )
            )
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupTweaksApplyResult()
        setupFab()
        setupSaveModule()
        setupState()
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
            is State.Loading -> {
                binding.tweaksOverlayLoading.isVisible = true
                binding.tweaksOverlayLoaded.isVisible = false
                binding.hideAppsMagisk.root.isVisible = false
            }
            is State.Loaded -> {
                binding.tweaksOverlayLoading.isVisible = false
                binding.tweaksOverlayLoaded.isVisible = true
                binding.hideAppsMagisk.root.isVisible = false
                binding.tweaksOverlayRecyclerView.adapter?.notifyDataSetChanged()
            }
            is State.ModuleRequired -> {
                binding.tweaksOverlayLoading.isVisible = false
                binding.tweaksOverlayLoaded.isVisible = false
                binding.hideAppsMagisk.root.isVisible = true
            }
        }
    }

    private fun setupRecyclerView() = with(binding.tweaksOverlayRecyclerView) {
        layoutManager = LinearLayoutManager(context)
        adapter = TweaksOverlayAdapter()
        applyBottomNavigationInset()
    }

    override fun onDestroyView() {
        binding.tweaksOverlayRecyclerView.adapter = null
        super.onDestroyView()
    }

    private fun setupTweaksApplyResult() {
        setFragmentResultListener(REQUEST_KEY_TWEAKS){ _, result ->
            val wasSuccessful = result.getBoolean(RESULT_EXTRA_TWEAKS_WAS_SUCCESSFUL, false)
            if(wasSuccessful){
                viewModel.reload()
            }
        }
    }

    private fun setupFab() = with(binding.tweaksOverlaySave) {
        applyBottomNavigationMargin()
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            onClicked().collect {
                viewModel.onSaveClicked()
            }
        }
    }

    private fun setupSaveModule() = viewLifecycleOwner.lifecycleScope.launchWhenResumed {
        binding.hideAppsMagisk.hideAppsMagiskSave.onClicked().collect {
            viewModel.onSaveModuleClicked(saveModuleLauncher)
        }
    }

    private inner class TweaksOverlayAdapter:
        BaseSettingsAdapter(requireContext(), binding.tweaksOverlayRecyclerView, items)

}