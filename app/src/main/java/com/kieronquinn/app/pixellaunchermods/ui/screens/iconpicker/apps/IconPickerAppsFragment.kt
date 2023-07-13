package com.kieronquinn.app.pixellaunchermods.ui.screens.iconpicker.apps

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.elevation.ElevationOverlayProvider
import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.databinding.FragmentIconPickerAppsBinding
import com.kieronquinn.app.pixellaunchermods.ui.base.BackAvailable
import com.kieronquinn.app.pixellaunchermods.ui.screens.iconpicker.BasePickerFragment
import com.kieronquinn.app.pixellaunchermods.ui.screens.iconpicker.apps.IconPickerAppsViewModel.State
import com.kieronquinn.app.pixellaunchermods.utils.extensions.applyBottomNavigationInset
import com.kieronquinn.app.pixellaunchermods.utils.extensions.hideIme
import com.kieronquinn.app.pixellaunchermods.utils.extensions.onChanged
import com.kieronquinn.app.pixellaunchermods.utils.extensions.onClicked
import com.kieronquinn.app.pixellaunchermods.utils.extensions.onEditorActionSent
import org.koin.androidx.viewmodel.ext.android.viewModel

class IconPickerAppsFragment: BasePickerFragment<FragmentIconPickerAppsBinding>(FragmentIconPickerAppsBinding::inflate), BackAvailable {

    override val viewModel by viewModel<IconPickerAppsViewModel>()
    private val args by navArgs<IconPickerAppsFragmentArgs>()

    private val adapter by lazy {
        IconPickerAppsAdapter(
            binding.iconPickerAppsRecyclerview,
            emptyList(),
            false,
            viewModel::onShrinkIconsChanged,
            viewModel::onAppClicked
        )
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
        setupState()
        setupSearch()
        setupRecyclerView()
        viewModel.setupWithConfig(args.mono)
    }

    override fun onDestroyView() {
        binding.iconPickerAppsRecyclerview.adapter = null
        super.onDestroyView()
    }

    private fun setupRecyclerView() = with(binding.iconPickerAppsRecyclerview) {
        layoutManager = LinearLayoutManager(context)
        adapter = this@IconPickerAppsFragment.adapter
        applyBottomNavigationInset(resources.getDimension(R.dimen.margin_16))
    }

    private fun setupSearch() = with(binding.iconPickerSearch) {
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
                binding.iconPickerAppsLoading.isVisible = true
                binding.iconPickerAppsRecyclerview.isVisible = false
                binding.iconPickerSearch.root.isVisible = false
            }
            is State.Loaded -> {
                binding.iconPickerAppsLoading.isVisible = false
                binding.iconPickerAppsRecyclerview.isVisible = true
                binding.iconPickerSearch.root.isVisible = true
                adapter.items = state.items
                adapter.mono = state.mono
                adapter.notifyDataSetChanged()
            }
        }
    }

}