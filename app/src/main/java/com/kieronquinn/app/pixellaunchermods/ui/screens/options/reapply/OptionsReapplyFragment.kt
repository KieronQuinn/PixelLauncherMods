package com.kieronquinn.app.pixellaunchermods.ui.screens.options.reapply

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.databinding.FragmentOptionsReapplyBinding
import com.kieronquinn.app.pixellaunchermods.ui.base.BackAvailable
import com.kieronquinn.app.pixellaunchermods.ui.base.BoundFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class OptionsReapplyFragment: BoundFragment<FragmentOptionsReapplyBinding>(FragmentOptionsReapplyBinding::inflate), BackAvailable {

    private val viewModel by viewModel<OptionsReapplyViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupFinished()
    }

    private fun setupFinished() = viewLifecycleOwner.lifecycleScope.launchWhenResumed {
        viewModel.finishedBus.collect {
            Toast.makeText(requireContext(), R.string.options_reapply_finished, Toast.LENGTH_LONG).show()
        }
    }

}