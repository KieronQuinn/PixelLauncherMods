package com.kieronquinn.app.pixellaunchermods.ui.screens.iconpicker.pack

import android.app.Activity
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.elevation.ElevationOverlayProvider
import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.databinding.FragmentIconPickerPackBinding
import com.kieronquinn.app.pixellaunchermods.ui.base.BackAvailable
import com.kieronquinn.app.pixellaunchermods.ui.base.LockCollapsed
import com.kieronquinn.app.pixellaunchermods.ui.base.ProvidesOverflow
import com.kieronquinn.app.pixellaunchermods.ui.base.ProvidesTitle
import com.kieronquinn.app.pixellaunchermods.ui.screens.iconpicker.BasePickerFragment
import com.kieronquinn.app.pixellaunchermods.ui.screens.iconpicker.pack.IconPickerPackViewModel.State
import com.kieronquinn.app.pixellaunchermods.utils.extensions.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class IconPickerPackFragment: BasePickerFragment<FragmentIconPickerPackBinding>(FragmentIconPickerPackBinding::inflate), BackAvailable, ProvidesTitle, LockCollapsed, ProvidesOverflow {

    override val viewModel by viewModel<IconPickerPackViewModel>()
    private val args by navArgs<IconPickerPackFragmentArgs>()

    private val externalPicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        if(it.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val resultIntent = it.data ?: return@registerForActivityResult
        viewModel.onExternalResult(resultIntent.data ?: return@registerForActivityResult)
    }

    private val adapter by lazy {
        IconPickerPackAdapter(requireContext(), emptyList(), viewModel::onIconClicked)
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
        viewModel.setupWithConfig(args.iconPack.packageName, args.mono)
    }

    private fun setupRecyclerView() {
        with(binding.iconPickerPackRecyclerview){
            adapter = this@IconPickerPackFragment.adapter
            applyBottomNavigationInset(resources.getDimension(R.dimen.margin_16))
            setPopUpTypeface(ResourcesCompat.getFont(context, R.font.google_sans_text_medium))
        }
        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            binding.root.awaitPost()
            binding.iconPickerPackRecyclerview.layoutManager = createGridLayoutManager()
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
                binding.iconPickerPackLoading.isVisible = true
                binding.iconPickerPackRecyclerview.isVisible = false
                binding.iconPickerSearch.root.isVisible = false
            }
            is State.Loaded -> {
                binding.iconPickerPackLoading.isVisible = false
                binding.iconPickerPackRecyclerview.isVisible = true
                binding.iconPickerSearch.root.isVisible = true
                adapter.items = state.items
                adapter.notifyDataSetChanged()
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
        searchClear.isVisible = viewModel.showClose.value
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            viewModel.showClose.collect {
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
                        IconPickerPackViewModel.Item.ItemType.CATEGORY.ordinal -> spanSize
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
        return args.iconPack.label
    }

    override fun inflateMenu(menuInflater: MenuInflater, menu: Menu) {
        if(args.iconPack.externalIntent != null){
            menuInflater.inflate(R.menu.menu_icon_picker_pack, menu)
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when(menuItem.itemId){
            R.id.app_editor_iconpicker_pack_open_external -> {
                viewModel.onLaunchExternalClicked(externalPicker, args.iconPack.externalIntent)
                true
            }
            else -> false
        }
    }

}