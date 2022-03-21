package com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.deferredrestart

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.databinding.FragmentDeferredRestartBinding
import com.kieronquinn.app.pixellaunchermods.ui.base.BackAvailable
import com.kieronquinn.app.pixellaunchermods.ui.base.BoundFragment
import com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.deferredrestart.DeferredRestartViewModel.State
import com.kieronquinn.app.pixellaunchermods.utils.extensions.applyBottomNavigationInset
import org.koin.androidx.viewmodel.ext.android.viewModel

class DeferredRestartFragment: BoundFragment<FragmentDeferredRestartBinding>(FragmentDeferredRestartBinding::inflate), BackAvailable {

    private val viewModel by viewModel<DeferredRestartViewModel>()

    private val adapter by lazy {
        DeferredRestartAdapter(
            binding.deferredRestartRecyclerview,
            emptyList(),
            viewModel::onOptionClicked
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
        setupRecyclerView()
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
                binding.deferredRestartLoading.isVisible = true
                binding.deferredRestartRecyclerview.isVisible = false
            }
            is State.Loaded -> {
                binding.deferredRestartLoading.isVisible = false
                binding.deferredRestartRecyclerview.isVisible = true
                adapter.items = state.items
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun setupRecyclerView() = with(binding.deferredRestartRecyclerview) {
        layoutManager = LinearLayoutManager(context)
        adapter = this@DeferredRestartFragment.adapter
        applyBottomNavigationInset(resources.getDimension(R.dimen.margin_16))
    }

}