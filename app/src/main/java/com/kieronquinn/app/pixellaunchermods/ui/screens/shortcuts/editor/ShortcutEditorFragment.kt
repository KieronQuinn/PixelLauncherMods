package com.kieronquinn.app.pixellaunchermods.ui.screens.shortcuts.editor

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.elevation.ElevationOverlayProvider
import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.databinding.FragmentShortcutEditorBinding
import com.kieronquinn.app.pixellaunchermods.model.editor.ShortcutEditorInfo
import com.kieronquinn.app.pixellaunchermods.ui.base.*
import com.kieronquinn.app.pixellaunchermods.ui.screens.iconpicker.BasePickerFragment
import com.kieronquinn.app.pixellaunchermods.ui.screens.shortcuts.editor.ShortcutEditorViewModel.State
import com.kieronquinn.app.pixellaunchermods.utils.extensions.onChanged
import com.kieronquinn.app.pixellaunchermods.utils.extensions.onClicked
import org.koin.androidx.viewmodel.ext.android.viewModel

class ShortcutEditorFragment: BoundFragment<FragmentShortcutEditorBinding>(FragmentShortcutEditorBinding::inflate),
    LockCollapsed, BackAvailable, ProvidesTitle, ProvidesOverflow {

    private val args by navArgs<ShortcutEditorFragmentArgs>()
    private val viewModel by viewModel<ShortcutEditorViewModel>()

    private val glide by lazy {
        Glide.with(requireContext())
    }

    private val remoteFavourite by lazy {
        args.remoteFavourite
    }

    private val elevationOverlayProvider by lazy {
        ElevationOverlayProvider(requireContext())
    }

    private val headerBackground by lazy {
        elevationOverlayProvider.compositeOverlayWithThemeSurfaceColorIfNeeded(
            resources.getDimension(R.dimen.app_editor_header_elevation)
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
        setupHeader()
        setupFabVisibility()
        setupInput()
        setupFab()
        setupResultListeners()
        viewModel.setupWithRemoteFavourite(remoteFavourite)
    }

    private fun setupHeader() {
        binding.appEditorShortcutCollapsingToolbar.backgroundTintList =
            ColorStateList.valueOf(headerBackground)
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            binding.appEditorShortcutIconContainer.appEditorShortcutIconNormal.onClicked().collect {
                viewModel.onIconClick()
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
                binding.appEditorShortcutContent.isVisible = false
                binding.appEditorShortcutAppbar.isVisible = false
                binding.appEditorShortcutLoading.isVisible = true
                binding.appEditorShortcutLoadingLabel.setText(R.string.app_editor_loading_shortcut)
            }
            is State.Loaded -> {
                binding.appEditorShortcutContent.isVisible = true
                binding.appEditorShortcutAppbar.isVisible = true
                binding.appEditorShortcutLoading.isVisible = false
                setupWithShortcutEditorInfo(state.shortcutEditorInfo)
            }
            is State.Saving -> {
                binding.appEditorShortcutContent.isVisible = false
                binding.appEditorShortcutAppbar.isVisible = false
                binding.appEditorShortcutLoading.isVisible = true
                binding.appEditorShortcutLoadingLabel.setText(R.string.app_editor_saving_shortcut)
            }
            is State.Resetting -> {
                binding.appEditorShortcutContent.isVisible = false
                binding.appEditorShortcutAppbar.isVisible = false
                binding.appEditorShortcutLoading.isVisible = true
                binding.appEditorShortcutLoadingLabel.setText(R.string.app_editor_resetting_shortcut)
            }
        }
    }

    private fun setupFabVisibility(){
        handleFabVisibility(viewModel.fabVisible.value)
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            viewModel.fabVisible.collect {
                handleFabVisibility(it)
            }
        }
    }

    private fun handleFabVisibility(visible: Boolean){
        if(visible){
            binding.appEditorShortcutFab.visibility = View.VISIBLE
            binding.appEditorShortcutFab.show()
        }else{
            binding.appEditorShortcutFab.hide()
        }
    }

    private fun setupWithShortcutEditorInfo(shortcutEditorInfo: ShortcutEditorInfo) = with(binding) {
        if(appEditorShortcutEdit.text.isNullOrEmpty()) {
            appEditorShortcutEdit.setText(shortcutEditorInfo.title, TextView.BufferType.EDITABLE)
        }
        val remoteFavourite = shortcutEditorInfo.toRemoteFavourite()
        glide.load(remoteFavourite)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(appEditorShortcutIconContainer.appEditorShortcutIconNormal)
    }

    private fun setupInput() {
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            binding.appEditorShortcutEdit.onChanged().collect {
                viewModel.onLabelChanged(it)
            }
        }
    }

    private fun setupFab() = viewLifecycleOwner.lifecycleScope.launchWhenResumed {
        binding.appEditorShortcutFab.onClicked().collect {
            viewModel.onSaveClicked()
        }
    }

    private fun setupResultListeners() {
        setFragmentResultListener(BasePickerFragment.KEY_RESULT_ICON_THEMED) { _, bundle ->
            viewModel.onIconChanged(
                bundle.getParcelable(BasePickerFragment.KEY_RESULT_EXTRA_THEMED_RESULT)
                    ?: return@setFragmentResultListener
            )
        }
        setFragmentResultListener(BasePickerFragment.KEY_RESULT_ICON_NORMAL) { _, bundle ->
            viewModel.onIconChanged(
                bundle.getParcelable(BasePickerFragment.KEY_RESULT_EXTRA_NORMAL_RESULT)
                ?: return@setFragmentResultListener
            )
        }
    }

    override fun getTitle(): CharSequence {
        return remoteFavourite.title ?: ""
    }

    override fun inflateMenu(menuInflater: MenuInflater, menu: Menu) {
        return menuInflater.inflate(R.menu.menu_app_editor, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when(menuItem.itemId){
            R.id.menu_app_editor_reset -> {
                viewModel.onResetClicked()
                return true
            }
        }
        return false
    }

}