package com.kieronquinn.app.pixellaunchermods.ui.screens.iconpicker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.kieronquinn.app.pixellaunchermods.ui.base.BoundFragment

abstract class BasePickerFragment<V: ViewBinding>(inflate: (LayoutInflater, ViewGroup?, Boolean) -> V): BoundFragment<V>(inflate) {

    companion object {
        const val KEY_RESULT_ICON_NORMAL = "icon_normal"
        const val KEY_RESULT_EXTRA_NORMAL_RESULT = "result"
        const val KEY_RESULT_ICON_THEMED = "icon_themed"
        const val KEY_RESULT_EXTRA_THEMED_RESULT = "result"
    }

    abstract val viewModel: BasePickerViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBitmapResultListener()
    }

    private fun setupBitmapResultListener() = viewLifecycleOwner.lifecycleScope.launchWhenResumed {
        viewModel.iconPickerResultBus.collect {
            if(viewModel.isMono()){
                setFragmentResult(KEY_RESULT_ICON_THEMED, bundleOf(KEY_RESULT_EXTRA_THEMED_RESULT to it))
            }else{
                setFragmentResult(KEY_RESULT_ICON_NORMAL, bundleOf(KEY_RESULT_EXTRA_NORMAL_RESULT to it))
            }
            viewModel.navigateUpToAppEditor()
        }
    }

}