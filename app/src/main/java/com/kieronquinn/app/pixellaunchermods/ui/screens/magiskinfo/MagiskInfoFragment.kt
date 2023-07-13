package com.kieronquinn.app.pixellaunchermods.ui.screens.magiskinfo

import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.databinding.FragmentMagiskInfoBinding
import com.kieronquinn.app.pixellaunchermods.ui.base.BackAvailable
import com.kieronquinn.app.pixellaunchermods.ui.base.BoundFragment
import com.kieronquinn.app.pixellaunchermods.utils.extensions.applyBottomNavigationInset
import com.kieronquinn.app.pixellaunchermods.utils.extensions.onClicked
import org.koin.androidx.viewmodel.ext.android.viewModel

class MagiskInfoFragment: BoundFragment<FragmentMagiskInfoBinding>(FragmentMagiskInfoBinding::inflate), BackAvailable {

    private val saveModuleLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("*/*")) {
        if(it != null){
            viewModel.saveModule(it)
        }
    }

    private val viewModel by viewModel<MagiskInfoViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSaveModule()
        setupInsets()
    }

    private fun setupInsets() = with(binding.root) {
        applyBottomNavigationInset(resources.getDimension(R.dimen.margin_16))
    }

    private fun setupSaveModule() = viewLifecycleOwner.lifecycleScope.launchWhenResumed {
        binding.magiskInfoIncludeHideAppsMagisk.hideAppsMagiskSave.onClicked().collect {
            viewModel.onSaveModuleClicked(saveModuleLauncher)
        }
    }

}