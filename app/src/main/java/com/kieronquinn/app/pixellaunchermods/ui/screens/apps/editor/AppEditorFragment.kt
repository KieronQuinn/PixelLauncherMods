package com.kieronquinn.app.pixellaunchermods.ui.screens.apps.editor

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
import com.kieronquinn.app.pixellaunchermods.databinding.FragmentAppEditorBinding
import com.kieronquinn.app.pixellaunchermods.model.editor.AppEditorInfo
import com.kieronquinn.app.pixellaunchermods.model.remote.RemoteApp
import com.kieronquinn.app.pixellaunchermods.model.remote.RemoteAppOptions
import com.kieronquinn.app.pixellaunchermods.ui.base.*
import com.kieronquinn.app.pixellaunchermods.ui.screens.apps.editor.AppEditorViewModel.State
import com.kieronquinn.app.pixellaunchermods.ui.screens.iconpicker.BasePickerFragment.Companion.KEY_RESULT_EXTRA_NORMAL_RESULT
import com.kieronquinn.app.pixellaunchermods.ui.screens.iconpicker.BasePickerFragment.Companion.KEY_RESULT_EXTRA_THEMED_RESULT
import com.kieronquinn.app.pixellaunchermods.ui.screens.iconpicker.BasePickerFragment.Companion.KEY_RESULT_ICON_NORMAL
import com.kieronquinn.app.pixellaunchermods.ui.screens.iconpicker.BasePickerFragment.Companion.KEY_RESULT_ICON_THEMED
import com.kieronquinn.app.pixellaunchermods.utils.extensions.applyBottomNavigationMargin
import com.kieronquinn.app.pixellaunchermods.utils.extensions.onChanged
import com.kieronquinn.app.pixellaunchermods.utils.extensions.onClicked
import org.koin.androidx.viewmodel.ext.android.viewModel

class AppEditorFragment: BoundFragment<FragmentAppEditorBinding>(FragmentAppEditorBinding::inflate), LockCollapsed, BackAvailable, ProvidesTitle, ProvidesOverflow {

    private val args by navArgs<AppEditorFragmentArgs>()
    private val viewModel by viewModel<AppEditorViewModel>()

    private val glide by lazy {
        Glide.with(requireContext())
    }

    private val remoteApp by lazy {
        args.remoteApp
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
        viewModel.setupWithRemoteApp(remoteApp)
    }

    private fun setupHeader() {
        binding.appEditorCollapsingToolbar.backgroundTintList =
            ColorStateList.valueOf(headerBackground)
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            binding.appEditorIconContainer.appEditorIconThemed.onClicked().collect {
                viewModel.onThemedIconClick()
            }
        }
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            binding.appEditorIconContainer.appEditorIconAddThemed.onClicked().collect {
                viewModel.onThemedIconClick()
            }
        }
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            binding.appEditorIconContainer.appEditorIconNormal.onClicked().collect {
                viewModel.onNormalIconClick()
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
                binding.appEditorContent.isVisible = false
                binding.appEditorAppbar.isVisible = false
                binding.appEditorLoading.isVisible = true
                binding.appEditorLoadingLabel.setText(R.string.app_editor_loading)
            }
            is State.Loaded -> {
                binding.appEditorContent.isVisible = true
                binding.appEditorAppbar.isVisible = true
                binding.appEditorLoading.isVisible = false
                setupWithAppEditorInfo(state.appEditorInfo, state.areThemedIconsEnabled, state.nativeMonoIcons)
            }
            is State.Saving -> {
                binding.appEditorContent.isVisible = false
                binding.appEditorAppbar.isVisible = false
                binding.appEditorLoading.isVisible = true
                binding.appEditorLoadingLabel.setText(R.string.app_editor_saving)
            }
            is State.Resetting -> {
                binding.appEditorContent.isVisible = false
                binding.appEditorAppbar.isVisible = false
                binding.appEditorLoading.isVisible = true
                binding.appEditorLoadingLabel.setText(R.string.app_editor_resetting)
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
            binding.appEditorFab.visibility = View.VISIBLE
            binding.appEditorFab.show()
        }else{
            binding.appEditorFab.hide()
        }
    }

    private fun setupWithAppEditorInfo(appEditorInfo: AppEditorInfo, themedIconsEnabled: Boolean, nativeMonoIcons: Boolean) = with(binding) {
        appEditorEdit.setText(appEditorInfo.label, TextView.BufferType.EDITABLE)
        val isIconDynamic = appEditorInfo.isDynamic()
        val hasThemedIcon = appEditorInfo.hasMonoIcon(nativeMonoIcons)
        val showThemedIcon = hasThemedIcon && themedIconsEnabled
        with(appEditorIconContainer){
            appEditorNormal.isVisible = true
            val normalLabel = if(appEditorInfo.iconType == RemoteApp.Type.APP_SHORTCUT){
                R.string.app_editor_normal_icon_shortcut
            }else{
                R.string.app_editor_normal_icon
            }
            appEditorLabelNormal.setText(normalLabel)
            val themedLabel = if(appEditorInfo.iconType == RemoteApp.Type.APP_SHORTCUT){
                R.string.app_editor_themed_icon_shortcut
            }else{
                R.string.app_editor_themed_icon
            }
            appEditorLabelThemed.setText(themedLabel)
            appEditorThemed.isVisible = showThemedIcon && hasThemedIcon
            appEditorAddThemed.isVisible = themedIconsEnabled && !showThemedIcon
            appEditorDynamicWarning.isVisible = isIconDynamic
        }
        val remoteApp = appEditorInfo.toRemoteApp(nativeMonoIcons)
        glide.load(RemoteAppOptions(remoteApp, false))
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(appEditorIconContainer.appEditorIconNormal)
        if(showThemedIcon) {
            glide.load(RemoteAppOptions(remoteApp, true))
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(appEditorIconContainer.appEditorIconThemed)
        }else{
            appEditorIconContainer.appEditorIconThemed.setImageDrawable(null)
        }
    }

    private fun setupInput() {
        val isLabelMutable = !args.remoteApp.isLabelImmutable()
        binding.appEditorInput.run {
            isEnabled = isLabelMutable
            helperText = if(!isLabelMutable) {
                getString(R.string.app_editor_app_shortcut_label_warning)
            } else null
        }
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            binding.appEditorEdit.onChanged().collect {
                viewModel.onLabelChanged(it)
            }
        }
    }

    private fun setupFab() = with(binding.appEditorFab) {
        applyBottomNavigationMargin(resources.getDimension(R.dimen.margin_16))
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            onClicked().collect {
                viewModel.onSaveClicked()
            }
        }
    }

    private fun setupResultListeners() {
        setFragmentResultListener(KEY_RESULT_ICON_THEMED) { _, bundle ->
            viewModel.onThemedIconChanged(
                bundle.getParcelable(KEY_RESULT_EXTRA_THEMED_RESULT)
            )
        }
        setFragmentResultListener(KEY_RESULT_ICON_NORMAL) { _, bundle ->
            viewModel.onNormalIconChanged(bundle.getParcelable(KEY_RESULT_EXTRA_NORMAL_RESULT)
                    ?: return@setFragmentResultListener)
        }
    }

    override fun getTitle(): CharSequence {
        return remoteApp.label
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