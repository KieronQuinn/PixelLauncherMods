package com.kieronquinn.app.pixellaunchermods.ui.screens.iconpicker

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.databinding.FragmentIconPickerBinding
import com.kieronquinn.app.pixellaunchermods.ui.base.BackAvailable
import com.kieronquinn.app.pixellaunchermods.ui.base.ProvidesOverflow
import com.kieronquinn.app.pixellaunchermods.ui.screens.iconpicker.IconPickerViewModel.State
import com.kieronquinn.app.pixellaunchermods.utils.extensions.applyBottomNavigationInset
import org.koin.androidx.viewmodel.ext.android.viewModel

class IconPickerFragment: BasePickerFragment<FragmentIconPickerBinding>(FragmentIconPickerBinding::inflate), BackAvailable, ProvidesOverflow {

    override val viewModel by viewModel<IconPickerViewModel>()
    private val args by navArgs<IconPickerFragmentArgs>()

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()){
        viewModel.onImageUriReturned(it ?: return@registerForActivityResult)
    }

    private val adapter by lazy {
        IconPickerAdapter(
            requireContext(),
            emptyList(),
            false,
            viewModel::onIconClicked,
            viewModel::onSourceClicked
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupState()
        setupImportBus()
        viewModel.setConfig(args.componentName, args.mono)
    }

    private fun setupRecyclerView() = with(binding.iconPickerRecyclerview) {
        layoutManager = LinearLayoutManager(context)
        adapter = this@IconPickerFragment.adapter
        applyBottomNavigationInset(
            resources.getDimension(R
                .dimen.margin_16)
        )
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
                binding.iconPickerLoading.isVisible = true
                binding.iconPickerRecyclerview.isVisible = false
            }
            is State.Loaded -> {
                binding.iconPickerLoading.isVisible = false
                binding.iconPickerRecyclerview.isVisible = true
                adapter.items = state.iconPacks
                adapter.mono = state.mono
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun setupImportBus() = viewLifecycleOwner.lifecycleScope.launchWhenResumed {
        viewModel.importImageBus.collect {
            imagePicker.launch("image/*")
        }
    }

    override fun inflateMenu(menuInflater: MenuInflater, menu: Menu) {
        if(args.mono){
            menuInflater.inflate(R.menu.menu_icon_picker, menu)
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when(menuItem.itemId){
            R.id.app_editor_iconpicker_remove -> viewModel.onClearClicked()
        }
        return true
    }

}