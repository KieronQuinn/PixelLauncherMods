package com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.recents

import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.databinding.FragmentSettingsTweaksRecentsBinding
import com.kieronquinn.app.pixellaunchermods.repositories.SettingsRepository.FakePixelLauncherModsSetting
import com.kieronquinn.app.pixellaunchermods.ui.base.BackAvailable
import com.kieronquinn.app.pixellaunchermods.ui.base.BoundFragment
import com.kieronquinn.app.pixellaunchermods.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.pixellaunchermods.ui.base.settings.BaseSettingsViewModel
import com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.overlayapply.OverlayApplyFragment
import com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.recents.RecentsTweaksViewModel.State
import com.kieronquinn.app.pixellaunchermods.utils.extensions.applyBottomNavigationInset
import com.kieronquinn.app.pixellaunchermods.utils.extensions.applyBottomNavigationMargin
import com.kieronquinn.app.pixellaunchermods.utils.extensions.onClicked
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*

class RecentsTweaksFragment: BoundFragment<FragmentSettingsTweaksRecentsBinding>(FragmentSettingsTweaksRecentsBinding::inflate), BackAvailable {

    private val viewModel by viewModel<RecentsTweaksViewModel>()

    private val saveModuleLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument()) {
        if(it != null){
            viewModel.saveModule(it)
        }
    }

    private val items by lazy {
        listOf(
            BaseSettingsViewModel.SettingsItem.Slider(
                icon = R.drawable.ic_tweaks_recents_transparency,
                titleRes = R.string.tweaks_recents_background_transparency,
                contentRes = R.string.tweaks_recents_background_transparency_content,
                min = 0f,
                max = 100f,
                setting = FakePixelLauncherModsSetting(
                    viewModel.transparency, viewModel::onTransparencyChanged
                ),
                labelFormatter = {
                    val value = String.format(Locale.getDefault(), "%.0f", it)
                    getString(R.string.tweaks_recents_formatter, value)
                }
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
                binding.tweaksRecentsLoading.isVisible = true
                binding.tweaksRecentsLoaded.isVisible = false
                binding.hideAppsMagisk.root.isVisible = false
            }
            is State.Loaded -> {
                binding.tweaksRecentsLoading.isVisible = false
                binding.tweaksRecentsLoaded.isVisible = true
                binding.hideAppsMagisk.root.isVisible = false
                binding.tweaksRecentsRecyclerView.adapter?.notifyDataSetChanged()
            }
            is State.ModuleRequired -> {
                binding.tweaksRecentsLoading.isVisible = false
                binding.tweaksRecentsLoaded.isVisible = false
                binding.hideAppsMagisk.root.isVisible = true
            }
        }
    }

    private fun setupRecyclerView() = with(binding.tweaksRecentsRecyclerView) {
        layoutManager = LinearLayoutManager(context)
        adapter = TweaksRecentsAdapter()
        applyBottomNavigationInset()
    }

    override fun onDestroyView() {
        binding.tweaksRecentsRecyclerView.adapter = null
        super.onDestroyView()
    }

    private fun setupTweaksApplyResult() {
        setFragmentResultListener(OverlayApplyFragment.REQUEST_KEY_TWEAKS){ _, result ->
            val wasSuccessful = result.getBoolean(OverlayApplyFragment.RESULT_EXTRA_TWEAKS_WAS_SUCCESSFUL, false)
            if(wasSuccessful){
                viewModel.reload()
            }
        }
    }

    private fun setupFab() = with(binding.tweaksRecentsSave) {
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

    private inner class TweaksRecentsAdapter:
        BaseSettingsAdapter(requireContext(), binding.tweaksRecentsRecyclerView, items)

}