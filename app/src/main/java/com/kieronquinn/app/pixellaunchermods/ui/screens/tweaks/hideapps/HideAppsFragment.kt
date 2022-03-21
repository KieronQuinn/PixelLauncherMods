package com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.hideapps

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.elevation.ElevationOverlayProvider
import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.databinding.FragmentHideAppsBinding
import com.kieronquinn.app.pixellaunchermods.ui.base.BackAvailable
import com.kieronquinn.app.pixellaunchermods.ui.base.BoundFragment
import com.kieronquinn.app.pixellaunchermods.ui.base.ProvidesOverflow
import com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.hideapps.HideAppsViewModel.State
import com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.overlayapply.OverlayApplyFragment.Companion.REQUEST_KEY_TWEAKS
import com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.overlayapply.OverlayApplyFragment.Companion.RESULT_EXTRA_TWEAKS_WAS_SUCCESSFUL
import com.kieronquinn.app.pixellaunchermods.utils.extensions.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class HideAppsFragment: BoundFragment<FragmentHideAppsBinding>(FragmentHideAppsBinding::inflate), BackAvailable, ProvidesOverflow {

    private val viewModel by viewModel<HideAppsViewModel>()

    private val adapter by lazy {
        HideAppsAdapter(requireContext(), emptyList())
    }

    private val elevationOverlayProvider by lazy {
        ElevationOverlayProvider(requireContext())
    }

    private val searchBackground by lazy {
        elevationOverlayProvider.compositeOverlayWithThemeSurfaceColorIfNeeded(
            resources.getDimension(R.dimen.app_editor_header_elevation)
        )
    }

    private val saveModuleLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument()) {
        if(it != null){
            viewModel.saveModule(it)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupState()
        setupSearch()
        setupTweaksApplyResult()
        setupFab()
        setupSaveModule()
    }

    override fun inflateMenu(menuInflater: MenuInflater, menu: Menu) {
        menuInflater.inflate(R.menu.menu_hide_apps, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when(menuItem.itemId){
            R.id.menu_hide_apps_deselect_all -> adapter.deselectAll()
        }
        return true
    }

    private fun setupState() {
        handleState(viewModel.state.value)
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            viewModel.state.collect {
                handleState(it)
            }
        }
    }

    private fun setupSearch() = with(binding.hideAppsSearch) {
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
                binding.hideAppsLoading.isVisible = true
                binding.hideAppsLoaded.isVisible = false
                binding.hideAppsMagisk.root.isVisible = false
            }
            is State.Loaded -> {
                binding.hideAppsLoading.isVisible = false
                binding.hideAppsLoaded.isVisible = true
                binding.hideAppsMagisk.root.isVisible = false
                adapter.items = state.apps
                adapter.notifyDataSetChanged()
            }
            is State.ModuleRequired -> {
                binding.hideAppsLoading.isVisible = false
                binding.hideAppsLoaded.isVisible = false
                binding.hideAppsMagisk.root.isVisible = true
            }
        }
    }

    private fun setupRecyclerView() = with(binding.hideAppsRecyclerview) {
        layoutManager = LinearLayoutManager(context)
        adapter = this@HideAppsFragment.adapter
        val inset = resources.getDimension(R.dimen.bottom_nav_height_margins) +
                resources.getDimension(R.dimen.margin_16)
        applyBottomNavigationInset(inset)
    }

    private fun setupTweaksApplyResult() {
        setFragmentResultListener(REQUEST_KEY_TWEAKS){ _, result ->
            val wasSuccessful = result.getBoolean(RESULT_EXTRA_TWEAKS_WAS_SUCCESSFUL, false)
            if(wasSuccessful){
                viewModel.reload()
            }
        }
    }

    private fun setupFab() = with(binding.hideAppsSave) {
        applyBottomNavigationMargin()
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            onClicked().collect {
                viewModel.onSaveClicked()
            }
        }
    }

    private fun setupSaveModule() = viewLifecycleOwner.lifecycleScope.launchWhenResumed {
        binding.hideAppsMagisk.hideAppsMagiskSave.onClicked().collect {
            viewModel.onSaveModuleClicked(saveModuleLauncher)
        }
    }

}