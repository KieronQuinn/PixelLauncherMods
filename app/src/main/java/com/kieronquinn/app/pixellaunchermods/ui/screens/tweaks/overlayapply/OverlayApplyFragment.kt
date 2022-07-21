package com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.overlayapply

import android.os.Bundle
import android.view.View
import android.widget.ScrollView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.databinding.FragmentTweaksApplyBinding
import com.kieronquinn.app.pixellaunchermods.ui.base.BackAvailable
import com.kieronquinn.app.pixellaunchermods.ui.base.BoundFragment
import com.kieronquinn.app.pixellaunchermods.ui.base.LockCollapsed
import com.kieronquinn.app.pixellaunchermods.ui.base.ProvidesBack
import com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.overlayapply.OverlayApplyViewModel.State
import com.kieronquinn.app.pixellaunchermods.utils.extensions.onClicked
import org.koin.androidx.viewmodel.ext.android.viewModel

class OverlayApplyFragment: BoundFragment<FragmentTweaksApplyBinding>(FragmentTweaksApplyBinding::inflate), BackAvailable, LockCollapsed, ProvidesBack {

    companion object {
        const val REQUEST_KEY_TWEAKS = "tweaks"
        const val RESULT_EXTRA_TWEAKS_WAS_SUCCESSFUL = "success"
    }

    private val saveLogLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument()) {
        if(it != null){
            viewModel.saveLog(it)
        }
    }

    private val viewModel by viewModel<OverlayApplyViewModel>()
    private val args by navArgs<OverlayApplyFragmentArgs>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.isNestedScrollingEnabled = false
        setupState()
        setupConsole()
        setupSaveConsole()
        setupClose()
        viewModel.setConfig(
            args.components,
            args.widgetReplacement?.widgetReplacement,
            args.recentsTransparency?.toFloatOrNull()
        )
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
            is State.Applying -> {
                binding.tweaksApplyTitle.setText(R.string.overlay_apply_applying)
                binding.tweaksApplyIcon.isVisible = false
                binding.tweaksApplyProgress.isVisible = true
                binding.tweaksApplyClose.isVisible = false
                binding.tweaksApplySaveLog.isVisible = false
            }
            is State.Finished -> {
                if(state.success) {
                    binding.tweaksApplyTitle.setText(R.string.overlay_apply_success)
                }else{
                    binding.tweaksApplyTitle.setText(R.string.tweaks_apply_failed)
                }
                if(state.success){
                    binding.tweaksApplyIcon.setImageResource(R.drawable.ic_check_circle)
                }else{
                    binding.tweaksApplyIcon.setImageResource(R.drawable.ic_error_circle)
                }
                binding.tweaksApplyIcon.isVisible = true
                binding.tweaksApplyProgress.isVisible = false
                binding.tweaksApplyClose.isVisible = true
                binding.tweaksApplySaveLog.isVisible = !state.success
            }
        }
    }

    private fun setupConsole() {
        handleConsoleUpdate(viewModel.consoleLines.value)
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            viewModel.consoleLines.collect {
                handleConsoleUpdate(it)
            }
        }
    }

    private fun setupSaveConsole() {
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            binding.tweaksApplySaveLog.onClicked().collect {
                viewModel.onSaveLogClicked(saveLogLauncher)
            }
        }
    }

    private fun setupClose() {
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            binding.tweaksApplyClose.onClicked().collect {
                onBackPressed()
            }
        }
    }

    override fun onBackPressed(): Boolean {
        setFragmentResult(REQUEST_KEY_TWEAKS,
            bundleOf(RESULT_EXTRA_TWEAKS_WAS_SUCCESSFUL to viewModel.getSuccessfulState())
        )
        viewModel.onBackPressed()
        return true
    }

    private fun handleConsoleUpdate(lines: List<String>) {
        binding.tweaksApplyConsole.text = lines.joinToString("\n")
        binding.tweaksApplyConsoleContainer.fullScroll(ScrollView.FOCUS_DOWN)
    }

}