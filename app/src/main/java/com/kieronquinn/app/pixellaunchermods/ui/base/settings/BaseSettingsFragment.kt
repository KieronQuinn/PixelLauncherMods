package com.kieronquinn.app.pixellaunchermods.ui.base.settings

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.kieronquinn.app.pixellaunchermods.databinding.FragmentSettingsBaseBinding
import com.kieronquinn.app.pixellaunchermods.ui.base.BoundFragment
import com.kieronquinn.app.pixellaunchermods.ui.base.settings.BaseSettingsViewModel.SettingsItem
import com.kieronquinn.app.pixellaunchermods.utils.extensions.applyBottomNavigationInset

abstract class BaseSettingsFragment: BoundFragment<FragmentSettingsBaseBinding>(FragmentSettingsBaseBinding::inflate) {

    abstract val viewModel: BaseSettingsViewModel
    abstract val items: List<SettingsItem>

    abstract fun createAdapter(items: List<SettingsItem>): BaseSettingsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    private fun setupRecyclerView() = with(binding.root) {
        layoutManager = LinearLayoutManager(context)
        adapter = createAdapter(items)
        applyBottomNavigationInset()
    }

    override fun onDestroyView() {
        binding.settingsBaseRecyclerView.adapter = null
        super.onDestroyView()
    }

}