package com.kieronquinn.app.pixellaunchermods.ui.screens.apps

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.databinding.FragmentAppsBinding
import com.kieronquinn.app.pixellaunchermods.ui.base.BoundFragment
import com.kieronquinn.app.pixellaunchermods.ui.base.CanShowSnackbar
import com.kieronquinn.app.pixellaunchermods.utils.extensions.applyBottomNavigationInset
import com.kieronquinn.app.pixellaunchermods.utils.extensions.awaitPost
import com.kieronquinn.app.pixellaunchermods.utils.extensions.isDarkMode
import org.koin.androidx.viewmodel.ext.android.viewModel

class AppsFragment: BoundFragment<FragmentAppsBinding>(FragmentAppsBinding::inflate), CanShowSnackbar {

    private val viewModel by viewModel<AppsViewModel>()

    private val adapter by lazy {
        AppsAdapter(requireContext(), emptyList(), false, viewModel::onItemClicked).apply {
            stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupState()
    }

    override fun onResume() {
        super.onResume()
        viewModel.updateThemedIconsState(requireContext().isDarkMode)
        viewModel.onResume()
    }

    private fun setupRecyclerView() {
        with(binding.appsRecyclerview){
            adapter = this@AppsFragment.adapter
            applyBottomNavigationInset(resources.getDimension(R.dimen.margin_16))
        }
        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            binding.root.awaitPost()
            binding.appsRecyclerview.layoutManager = createGridLayoutManager()
        }
    }

    private fun createGridLayoutManager(): GridLayoutManager {
        val spanSize = getColumnCount()
        return GridLayoutManager(requireContext(), spanSize).apply {
            spanSizeLookup = object: GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return when(adapter.getItemViewType(position)){
                        AppsViewModel.Item.Type.HEADER.ordinal -> spanSize
                        else -> 1
                    }
                }
            }
        }
    }

    private fun getColumnCount(): Int {
        val width = binding.root.measuredWidth
        val columnWidth = resources.getDimension(R.dimen.item_app_width)
        return (width / columnWidth).toInt()
    }

    private fun setupState() {
        handleState(viewModel.state.value)
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            viewModel.state.collect {
                handleState(it)
            }
        }
    }

    private fun handleState(state: AppsViewModel.State) {
        binding.appsLoading.isVisible = state is AppsViewModel.State.Loading
        binding.appsRecyclerview.isVisible = state is AppsViewModel.State.Loaded
        binding.appsError.isVisible = state is AppsViewModel.State.Error
        (state as? AppsViewModel.State.Loaded)?.let {
            adapter.items = it.apps
            adapter.notifyDataSetChanged()
        }
    }

}