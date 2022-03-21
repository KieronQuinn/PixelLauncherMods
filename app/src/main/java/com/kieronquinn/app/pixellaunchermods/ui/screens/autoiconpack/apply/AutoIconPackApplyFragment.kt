package com.kieronquinn.app.pixellaunchermods.ui.screens.autoiconpack.apply

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.pixellaunchermods.databinding.FragmentAutoIconPackApplyBinding
import com.kieronquinn.app.pixellaunchermods.ui.base.BackAvailable
import com.kieronquinn.app.pixellaunchermods.ui.base.BoundFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.math.roundToInt

class AutoIconPackApplyFragment: BoundFragment<FragmentAutoIconPackApplyBinding>(FragmentAutoIconPackApplyBinding::inflate), BackAvailable {

    private val viewModel by viewModel<AutoIconPackApplyViewModel>()
    private val args by navArgs<AutoIconPackApplyFragmentArgs>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
        viewModel.setupWithIconPackPackages(args.iconPacks.toList())
    }

    private fun setupState() {
        handleState(viewModel.state.value)
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            viewModel.state.collect {
                handleState(it)
            }
        }
    }

    private fun handleState(state: AutoIconPackApplyViewModel.State) {
        if(state !is AutoIconPackApplyViewModel.State.Applying) return
        binding.autoIconPackApplyingLoadingProgress.progress = (state.progress * 100).roundToInt()
    }

}