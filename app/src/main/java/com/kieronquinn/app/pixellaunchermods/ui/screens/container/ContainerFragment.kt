package com.kieronquinn.app.pixellaunchermods.ui.screens.container

import android.annotation.SuppressLint
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.MenuInflater
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.google.android.material.elevation.ElevationOverlayProvider
import com.google.android.material.snackbar.Snackbar
import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.components.navigation.ContainerNavigation
import com.kieronquinn.app.pixellaunchermods.components.navigation.setupWithNavigation
import com.kieronquinn.app.pixellaunchermods.components.notifications.requestNotificationPermission
import com.kieronquinn.app.pixellaunchermods.databinding.FragmentContainerBinding
import com.kieronquinn.app.pixellaunchermods.repositories.RemoteAppsRepository
import com.kieronquinn.app.pixellaunchermods.ui.base.BackAvailable
import com.kieronquinn.app.pixellaunchermods.ui.base.BoundFragment
import com.kieronquinn.app.pixellaunchermods.ui.base.CanShowSnackbar
import com.kieronquinn.app.pixellaunchermods.ui.base.LockCollapsed
import com.kieronquinn.app.pixellaunchermods.ui.base.ProvidesBack
import com.kieronquinn.app.pixellaunchermods.ui.base.ProvidesOverflow
import com.kieronquinn.app.pixellaunchermods.ui.base.ProvidesTitle
import com.kieronquinn.app.pixellaunchermods.utils.extensions.awaitPost
import com.kieronquinn.app.pixellaunchermods.utils.extensions.collapsedState
import com.kieronquinn.app.pixellaunchermods.utils.extensions.getRememberedAppBarCollapsed
import com.kieronquinn.app.pixellaunchermods.utils.extensions.getTopFragment
import com.kieronquinn.app.pixellaunchermods.utils.extensions.onApplyInsets
import com.kieronquinn.app.pixellaunchermods.utils.extensions.onDestinationChanged
import com.kieronquinn.app.pixellaunchermods.utils.extensions.onNavDestinationSelected
import com.kieronquinn.app.pixellaunchermods.utils.extensions.onNavigationIconClicked
import com.kieronquinn.app.pixellaunchermods.utils.extensions.onSwipeDismissed
import com.kieronquinn.app.pixellaunchermods.utils.extensions.rememberAppBarCollapsed
import com.kieronquinn.app.pixellaunchermods.utils.extensions.setOnBackPressedCallback
import com.kieronquinn.app.pixellaunchermods.utils.extensions.setTypeface
import com.kieronquinn.app.pixellaunchermods.utils.extensions.whenResumed
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.math.roundToInt

class ContainerFragment: BoundFragment<FragmentContainerBinding>(FragmentContainerBinding::inflate) {

    private val googleSansMedium by lazy {
        ResourcesCompat.getFont(requireContext(), R.font.google_sans_text_medium)
    }

    private val navHostFragment by lazy {
        childFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
    }

    private val navController by lazy {
        navHostFragment.navController
    }

    private val elevationOverlayProvider by lazy {
        ElevationOverlayProvider(requireContext())
    }

    private val headerBackground by lazy {
        elevationOverlayProvider.compositeOverlayWithThemeSurfaceColorIfNeeded(
            resources.getDimension(R.dimen.app_editor_header_elevation)
        )
    }

    private val googleSansTextMedium by lazy {
        ResourcesCompat.getFont(requireContext(), R.font.google_sans_text_medium)
    }

    private val updateSnackbar by lazy {
        Snackbar.make(binding.root, getString(R.string.snackbar_update), Snackbar.LENGTH_INDEFINITE).apply {
            setTypeface(googleSansTextMedium)
            anchorView = binding.containerBottomNavigation
            isAnchorViewLayoutListenerEnabled = true
            (view.background as? GradientDrawable)?.cornerRadius = resources.getDimension(R.dimen.snackbar_corner_radius)
            setAction(R.string.snackbar_update_button){
                viewModel.onUpdateClicked()
            }
            onSwipeDismissed {
                viewModel.onUpdateDismissed()
            }
        }
    }

    private val navigation by inject<ContainerNavigation>()
    private val viewModel by viewModel<ContainerViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupStack()
        setupCollapsedState()
        setupBottomNavigation()
        setupNavigation()
        setupCollapsingToolbar()
        setupToolbar()
        setupBack()
        setupAppBar()
        setupApplyState()
        setupUpdateSnackbar()
        NavigationUI.setupWithNavController(binding.containerBottomNavigation, navController)
        binding.containerBottomNavigation.setOnItemSelectedListener {  item ->
            //Clear the back stack back to the root, to prevent going back between tabs
            navController.popBackStack(R.id.nav_graph_main, false)
            navController.onNavDestinationSelected(item)
        }
        requireActivity().requestNotificationPermission()
    }

    private fun setupBottomNavigation() {
        binding.containerBottomNavigation.onApplyInsets { view, insets ->
            val bottomNavHeight = resources.getDimension(R.dimen.bottom_nav_height).toInt()
            val bottomInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            view.updateLayoutParams<CoordinatorLayout.LayoutParams> {
                height = bottomNavHeight + bottomInsets
            }
            view.updatePadding(bottom = bottomInsets)
        }
        binding.containerBottomNavigation.run {
            setBackgroundColor(ColorUtils.setAlphaComponent(headerBackground, 235))
        }
    }

    private fun setupCollapsingToolbar() = with(binding.containerCollapsingToolbar) {
        setExpandedTitleTypeface(googleSansMedium)
        setCollapsedTitleTypeface(googleSansMedium)
    }

    private fun setupStack() = viewLifecycleOwner.lifecycleScope.launchWhenResumed {
        navController.onDestinationChanged().collect {
            binding.root.awaitPost()
            onTopFragmentChanged(navHostFragment.getTopFragment() ?: return@collect)
        }
    }

    private fun setupCollapsedState() = viewLifecycleOwner.lifecycleScope.launchWhenResumed {
        binding.containerAppBar.collapsedState().collect {
            navHostFragment.getTopFragment()?.rememberAppBarCollapsed(it)
        }
    }

    private fun setupToolbar() = with(binding.containerToolbar) {
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            onNavigationIconClicked().collect {
                (navHostFragment.getTopFragment() as? ProvidesBack)?.let {
                    if(it.onBackPressed()) return@collect
                }
                viewModel.onBackPressed()
            }
        }
    }

    @SuppressLint("RestrictedApi")
    private fun setupBack() {
        val callback = object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                (navHostFragment.getTopFragment() as? ProvidesBack)?.let {
                    if(!it.interceptBack()) return@let
                    if(it.onBackPressed()) return
                }
                if(!navController.popBackStack()) {
                    requireActivity().finish()
                }
            }
        }
        navController.setOnBackPressedCallback(callback)
        navController.enableOnBackPressed(shouldBackDispatcherBeEnabled())
        navController.setOnBackPressedDispatcher(requireActivity().onBackPressedDispatcher)
        whenResumed {
            navController.onDestinationChanged().collect {
                navController.enableOnBackPressed(shouldBackDispatcherBeEnabled())
            }
        }
    }

    private fun shouldBackDispatcherBeEnabled(): Boolean {
        val top = navHostFragment.getTopFragment()
        return top is ProvidesBack && top.interceptBack()
    }

    private fun setupAppBar() {
        binding.containerAppBar.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            binding.navHostFragment.updatePadding(bottom = appBarLayout.totalScrollRange + verticalOffset)
        }
    }

    private fun setupApplyState() {
        handleApplyState(viewModel.iconApplyProgress.value)
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            viewModel.iconApplyProgress.collect {
                handleApplyState(it)
            }
        }
    }

    private fun handleApplyState(iconApplyProgress: RemoteAppsRepository.IconApplyProgress?) {
        binding.containerLoading.isVisible = iconApplyProgress != null
        if(iconApplyProgress != null){
            binding.containerLoadingLabel.setText(iconApplyProgress.titleRes)
            iconApplyProgress.contentRes?.let {
                binding.containerLoadingContent.isVisible = true
                binding.containerLoadingContent.setText(it)
            } ?: run {
                binding.containerLoadingContent.isVisible = false
            }
            val progress = when(iconApplyProgress){
                is RemoteAppsRepository.IconApplyProgress.ApplyingIcons -> null
                is RemoteAppsRepository.IconApplyProgress.UpdatingConfiguration -> {
                    iconApplyProgress.progress
                }
                is RemoteAppsRepository.IconApplyProgress.UpdatingIconPacks -> {
                    iconApplyProgress.progress
                }
            }
            binding.containerLoadingProgress.isIndeterminate = progress == null
            if(progress != null){
                binding.containerLoadingProgress.progress = (100 * progress).roundToInt()
            }
        }
    }

    private fun onTopFragmentChanged(topFragment: Fragment){
        val backIcon = if(topFragment is BackAvailable){
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_back)
        } else null
        if(topFragment is ProvidesOverflow){
            setupMenu(topFragment)
        }else{
            setupMenu(null)
        }
        if(topFragment is LockCollapsed) {
            binding.containerAppBar.setExpanded(false)
        }else {
            binding.containerAppBar.setExpanded(!topFragment.getRememberedAppBarCollapsed())
        }
        (topFragment as? ProvidesTitle)?.let {
            val label = it.getTitle()
            if(label == null || label.isBlank()) return@let
            binding.containerCollapsingToolbar.title = label
            binding.containerToolbar.title = label
        }
        binding.containerToolbar.navigationIcon = backIcon
        viewModel.setCanShowSnackbar(topFragment is CanShowSnackbar)
    }

    private fun setupNavigation() = viewLifecycleOwner.lifecycleScope.launchWhenResumed {
        launch {
            navHostFragment.setupWithNavigation(navigation)
        }
        launch {
            navController.onDestinationChanged().collect {
                val label = it.label
                if(label == null || label.isBlank()) return@collect
                binding.containerCollapsingToolbar.title = label
                binding.containerToolbar.title = label
            }
        }
    }

    private fun setupMenu(menuProvider: ProvidesOverflow?){
        val menu = binding.containerToolbar.menu
        val menuInflater = MenuInflater(requireContext())
        menu.clear()
        menuProvider?.inflateMenu(menuInflater, menu)
        binding.containerToolbar.setOnMenuItemClickListener {
            menuProvider?.onMenuItemSelected(it) ?: false
        }
    }

    private fun setupUpdateSnackbar() {
        handleUpdateSnackbar(viewModel.showUpdateSnackbar.value)
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            viewModel.showUpdateSnackbar.collect {
                handleUpdateSnackbar(it)
            }
        }
    }

    private fun handleUpdateSnackbar(show: Boolean){
        if(show){
            updateSnackbar.show()
        }else{
            updateSnackbar.dismiss()
        }
    }

}