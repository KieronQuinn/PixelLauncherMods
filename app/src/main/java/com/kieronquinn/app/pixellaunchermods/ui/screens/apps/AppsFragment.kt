package com.kieronquinn.app.pixellaunchermods.ui.screens.apps

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.elevation.ElevationOverlayProvider
import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.databinding.FragmentAppsBinding
import com.kieronquinn.app.pixellaunchermods.ui.base.BoundFragment
import com.kieronquinn.app.pixellaunchermods.ui.base.CanShowSnackbar
import com.kieronquinn.app.pixellaunchermods.ui.base.Root
import com.kieronquinn.app.pixellaunchermods.utils.extensions.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class AppsFragment: BoundFragment<FragmentAppsBinding>(FragmentAppsBinding::inflate), CanShowSnackbar, Root {

    private val viewModel by viewModel<AppsViewModel>()

    private val adapter by lazy {
        AppsAdapter(requireContext(), emptyList(), viewModel::onItemClicked).apply {
            stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }
    }

    private val elevationOverlayProvider by lazy {
        ElevationOverlayProvider(requireContext())
    }

    private val searchBackground by lazy {
        elevationOverlayProvider.compositeOverlayWithThemeSurfaceColorIfNeeded(
            resources.getDimension(R.dimen.app_editor_header_elevation)
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupState()
        setupSearch()
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

    private fun setupSearch() = with(binding.appsSearch) {
        searchBox.backgroundTintList = ColorStateList.valueOf(searchBackground)
        searchBox.setText(viewModel.getSearchTerm(), TextView.BufferType.EDITABLE)
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            searchBox.onChanged().collect {
                viewModel.setSearchTerm(it)
            }
        }
        searchClear.isVisible = viewModel.searchShowClear.value
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            viewModel.searchShowClear.collect {
                searchClear.isVisible = it
            }
        }
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            searchClear.onClicked().collect {
                searchBox.text?.clear()
            }
        }
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            searchBox.onEditorActionSent(EditorInfo.IME_ACTION_SEARCH).collect {
                searchBox.hideIme()
            }
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
        binding.appsSearch.root.isVisible = state is AppsViewModel.State.Loaded
        binding.appsError.isVisible = state is AppsViewModel.State.Error
        (state as? AppsViewModel.State.Loaded)?.let {
            adapter.items = it.apps
            adapter.notifyDataSetChanged()
        }
    }

}