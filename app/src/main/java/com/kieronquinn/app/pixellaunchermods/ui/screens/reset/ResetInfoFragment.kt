package com.kieronquinn.app.pixellaunchermods.ui.screens.reset

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.databinding.FragmentResetInfoBinding
import com.kieronquinn.app.pixellaunchermods.ui.base.BackAvailable
import com.kieronquinn.app.pixellaunchermods.ui.base.BoundFragment
import com.kieronquinn.app.pixellaunchermods.ui.base.LockCollapsed
import com.kieronquinn.app.pixellaunchermods.ui.screens.reset.ResetInfoViewModel.State
import com.kieronquinn.app.pixellaunchermods.utils.extensions.applyBottomNavigationMargin
import com.kieronquinn.app.pixellaunchermods.utils.extensions.onClicked
import org.koin.androidx.viewmodel.ext.android.viewModel

class ResetInfoFragment: BoundFragment<FragmentResetInfoBinding>(FragmentResetInfoBinding::inflate), BackAvailable, LockCollapsed {

    private val viewModel by viewModel<ResetInfoViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
        setupScroll()
        setupContinue()
    }

    private fun setupScroll() = with(binding.resetInfoLoaded){
        isNestedScrollingEnabled = false
        applyBottomNavigationMargin(resources.getDimension(R.dimen.margin_16))
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
                binding.resetInfoLoading.isVisible = true
                binding.resetInfoLoaded.isVisible = false
            }
            is State.Loaded -> {
                with(binding.resetInfoNotIncludedContent){
                    val extra = if(state.legacyShortcutName != null){
                        getString(
                            R.string.reset_info_not_included_content_extra_shortcut,
                            state.legacyShortcutName
                        )
                    }else{
                        ""
                    }
                    text = getString(R.string.reset_info_not_included_content, extra).trim()
                }
                binding.resetInfoLoading.isVisible = false
                binding.resetInfoLoaded.isVisible = true
            }
        }
    }

    private fun setupContinue() = viewLifecycleOwner.lifecycleScope.launchWhenResumed {
        binding.resetInfoContinue.onClicked().collect {
            viewModel.onContinueClicked()
        }
    }

}