package com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.widgetreplacement

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.databinding.FragmentTweaksWidgetReplacementBinding
import com.kieronquinn.app.pixellaunchermods.ui.base.BackAvailable
import com.kieronquinn.app.pixellaunchermods.ui.base.BoundFragment
import com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.overlayapply.OverlayApplyFragment
import com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.widgetreplacement.WidgetReplacementViewModel.State
import com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.widgetreplacement.widgetpicker.WidgetReplacementPickerFragment
import com.kieronquinn.app.pixellaunchermods.utils.extensions.applyBottomNavigationInset
import com.kieronquinn.app.pixellaunchermods.utils.extensions.applyBottomNavigationMargin
import com.kieronquinn.app.pixellaunchermods.utils.extensions.onClicked
import com.kieronquinn.app.pixellaunchermods.widget.ProxyWidget
import org.koin.androidx.viewmodel.ext.android.viewModel

class WidgetReplacementFragment: BoundFragment<FragmentTweaksWidgetReplacementBinding>(FragmentTweaksWidgetReplacementBinding::inflate), BackAvailable {

    private val viewModel by viewModel<WidgetReplacementViewModel>()

    private val adapter by lazy {
        WidgetReplacementAdapter(
            binding.tweaksWidgetReplacementRecyclerview,
            emptyList(),
            viewModel::getWidgetView,
            viewModel::onSwitchStateChanged,
            viewModel::onSelectClicked
        ) { viewModel.onReconfigureClicked(configurationLauncher) }
    }

    private val saveModuleLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("*/*")) {
        if(it != null){
            viewModel.saveModule(it)
        }
    }

    private val configurationLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        if(it.resultCode == Activity.RESULT_OK){
            viewModel.reload()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupState()
        setupFab()
        setupTweaksApplyResult()
        setupSaveModule()
        setupProviderChangeResult()
        ProxyWidget.sendUpdate(requireContext())
    }

    override fun onDestroyView() {
        binding.tweaksWidgetReplacementRecyclerview.adapter = null
        super.onDestroyView()
    }

    private fun setupRecyclerView() = with(binding.tweaksWidgetReplacementRecyclerview) {
        layoutManager = LinearLayoutManager(context)
        adapter = this@WidgetReplacementFragment.adapter
        val inset = resources.getDimension(R.dimen.bottom_nav_height_margins) +
                resources.getDimension(R.dimen.margin_16)
        applyBottomNavigationInset(inset)
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
                binding.tweaksWidgetReplacementLoading.isVisible = true
                binding.tweaksWidgetReplacementLoaded.isVisible = false
                binding.tweaksWidgetReplacementMagisk.root.isVisible = false
            }
            is State.Loaded -> {
                binding.tweaksWidgetReplacementLoading.isVisible = false
                binding.tweaksWidgetReplacementLoaded.isVisible = true
                binding.tweaksWidgetReplacementMagisk.root.isVisible = false
                binding.tweaksWidgetReplacementSave.isVisible = state.showFab
                adapter.items = state.items
                adapter.notifyDataSetChanged()
            }
            is State.ModuleRequired -> {
                binding.tweaksWidgetReplacementLoading.isVisible = false
                binding.tweaksWidgetReplacementLoaded.isVisible = false
                binding.tweaksWidgetReplacementMagisk.root.isVisible = true
            }
        }
    }

    private fun setupFab() = with(binding.tweaksWidgetReplacementSave) {
        applyBottomNavigationMargin()
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            onClicked().collect {
                viewModel.onSaveClicked()
            }
        }
    }

    private fun setupTweaksApplyResult() {
        setFragmentResultListener(OverlayApplyFragment.REQUEST_KEY_TWEAKS){ _, result ->
            val wasSuccessful = result.getBoolean(OverlayApplyFragment.RESULT_EXTRA_TWEAKS_WAS_SUCCESSFUL, false)
            if(wasSuccessful){
                viewModel.reload()
            }
        }
    }

    private fun setupProviderChangeResult() {
        setFragmentResultListener(WidgetReplacementPickerFragment.REQUEST_KEY_WIDGET_PROVIDER_PICKER){ _, result ->
            val wasChanged = result.getBoolean(WidgetReplacementPickerFragment.RESULT_EXTRA_WAS_CHANGED, false)
            if(wasChanged){
                viewModel.reload()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.startListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopListening()
    }

    private fun setupSaveModule() = viewLifecycleOwner.lifecycleScope.launchWhenResumed {
        binding.tweaksWidgetReplacementMagisk.hideAppsMagiskSave.onClicked().collect {
            viewModel.onSaveModuleClicked(saveModuleLauncher)
        }
    }

}