package com.kieronquinn.app.pixellaunchermods.ui.screens.root

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.components.navigation.RootNavigation
import com.kieronquinn.app.pixellaunchermods.components.navigation.setupWithNavigation
import com.kieronquinn.app.pixellaunchermods.databinding.FragmentRootBinding
import com.kieronquinn.app.pixellaunchermods.ui.base.BoundFragment
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class RootFragment: BoundFragment<FragmentRootBinding>(FragmentRootBinding::inflate) {

    private val navHostFragment by lazy {
        childFragmentManager.findFragmentById(R.id.nav_host_fragment_root) as NavHostFragment
    }

    private val navigation by inject<RootNavigation>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupNavigation()
    }

    private fun setupNavigation() = viewLifecycleOwner.lifecycleScope.launchWhenResumed {
        launch {
            navHostFragment.setupWithNavigation(navigation)
        }
    }

}