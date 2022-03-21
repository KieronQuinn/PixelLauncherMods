package com.kieronquinn.app.pixellaunchermods.ui.screens.iconpicker.legacythemed

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.elevation.ElevationOverlayProvider
import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.databinding.FragmentIconPickerLegacyThemedBinding
import com.kieronquinn.app.pixellaunchermods.ui.base.BackAvailable
import com.kieronquinn.app.pixellaunchermods.ui.base.ProvidesTitle
import com.kieronquinn.app.pixellaunchermods.ui.screens.iconpicker.BasePickerFragment
import com.kieronquinn.app.pixellaunchermods.ui.screens.iconpicker.legacythemed.LegacyThemedIconPickerViewModel.State
import com.kieronquinn.app.pixellaunchermods.utils.extensions.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class LegacyThemedIconPickerFragment: BasePickerFragment<FragmentIconPickerLegacyThemedBinding>(FragmentIconPickerLegacyThemedBinding::inflate), BackAvailable, ProvidesTitle {

    override val viewModel by viewModel<LegacyThemedIconPickerViewModel>()
    private val args by navArgs<LegacyThemedIconPickerFragmentArgs>()

    private val adapter by lazy {
        LegacyThemedIconPickerAdapter(requireContext(), emptyList(), viewModel::onIconClicked)
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
        setupRecyclerView()
        setupSearch()
        viewModel.setupWithConfig(args.mono, args.lawnicons)
    }

    private fun setupState() {
        handleState(viewModel.state.value)
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            viewModel.state.collect {
                handleState(it)
            }
        }
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

    private fun handleState(state: State) {
        when(state){
            is State.Loading -> {
                binding.iconPickerLegacyThemedLoading.isVisible = true
                binding.iconPickerLegacyThemedRecyclerview.isVisible = false
            }
            is State.Loaded -> {
                binding.iconPickerLegacyThemedLoading.isVisible = false
                binding.iconPickerLegacyThemedRecyclerview.isVisible = true
                adapter.items = state.icons
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun setupRecyclerView() = with(binding.iconPickerLegacyThemedRecyclerview) {
        adapter = this@LegacyThemedIconPickerFragment.adapter
        applyBottomNavigationInset(resources.getDimension(R.dimen.margin_16))
        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            binding.root.awaitPost()
            layoutManager = createGridLayoutManager()
        }
    }

    private fun createGridLayoutManager(): GridLayoutManager {
        val spanSize = getColumnCount()
        return GridLayoutManager(requireContext(), spanSize).apply {
            spanSizeLookup = object: GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return when(adapter.getItemViewType(position)){
                        LegacyThemedIconPickerViewModel.Item.ItemType.HEADER.ordinal -> spanSize
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

    override fun getTitle(): CharSequence {
        return if(args.lawnicons){
            getString(R.string.app_editor_iconpicker_lawnicons_title)
        }else{
            getString(R.string.app_editor_iconpicker_legacy_themed_title)
        }
    }

}