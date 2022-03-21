package com.kieronquinn.app.pixellaunchermods.ui.screens.reset.apply

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.databinding.FragmentResetApplyBinding
import com.kieronquinn.app.pixellaunchermods.ui.base.BackAvailable
import com.kieronquinn.app.pixellaunchermods.ui.base.BoundFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class ResetApplyFragment: BoundFragment<FragmentResetApplyBinding>(FragmentResetApplyBinding::inflate), BackAvailable {

    private val viewModel by viewModel<ResetApplyViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupComplete()
    }

    private fun setupComplete() = viewLifecycleOwner.lifecycleScope.launchWhenResumed {
        viewModel.completeBus.collect {
            if(it) viewModel
            val message = if(it) {
                R.string.reset_apply_toast_success
            } else {
                R.string.reset_apply_toast_failed
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            viewModel.close()
        }
    }

}