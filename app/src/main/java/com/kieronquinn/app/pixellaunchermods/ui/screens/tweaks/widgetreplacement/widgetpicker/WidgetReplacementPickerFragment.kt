package com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.widgetreplacement.widgetpicker

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.elevation.ElevationOverlayProvider
import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.databinding.FragmentWidgetReplacementPickerBinding
import com.kieronquinn.app.pixellaunchermods.model.tweaks.WidgetReplacement
import com.kieronquinn.app.pixellaunchermods.ui.base.BackAvailable
import com.kieronquinn.app.pixellaunchermods.ui.base.BoundFragment
import com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.widgetreplacement.widgetpicker.WidgetReplacementPickerViewModel.State
import com.kieronquinn.app.pixellaunchermods.utils.extensions.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class WidgetReplacementPickerFragment: BoundFragment<FragmentWidgetReplacementPickerBinding>(FragmentWidgetReplacementPickerBinding::inflate), BackAvailable {

    companion object {
        const val REQUEST_KEY_WIDGET_PROVIDER_PICKER = "widget_provider_picker"
        const val RESULT_EXTRA_WAS_CHANGED = "was_changed"
    }

    private val viewModel by viewModel<WidgetReplacementPickerViewModel>()

    private val configurationLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        val appWidgetId = it.data?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
        if(appWidgetId == null || appWidgetId == -1) return@registerForActivityResult
        if(it.resultCode == Activity.RESULT_OK){
            viewModel.onWidgetConfigured(requireContext(), null, appWidgetId)
            sendResult(true)
        }else{
            viewModel.onWidgetCancelled(appWidgetId)
            sendResult(false)
        }
    }

    private val bindLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val appWidgetId = it.data?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
        if(appWidgetId == null || appWidgetId == -1) return@registerForActivityResult
        if(it.resultCode == Activity.RESULT_OK){
            viewModel.onWidgetBound(requireContext(), null, configurationLauncher, appWidgetId)
            sendResult(true)
        }else{
            viewModel.onWidgetCancelled(appWidgetId)
            sendResult(false)
        }
    }

    private val adapter by lazy {
        WidgetReplacementPickerAdapter(
            binding.widgetReplacementPickerRecyclerview,
            emptyList(),
            0,
            viewModel::onAppClicked,
            ::onWidgetClicked
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
        setupRecyclerView()
        setupState()
        setupSearch()
        setupErrorBus()
    }

    override fun onDestroyView() {
        binding.widgetReplacementPickerRecyclerview.adapter = null
        super.onDestroyView()
    }

    private fun setupState(){
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
                binding.widgetReplacementPickerLoading.isVisible = true
                binding.widgetReplacementPickerLoaded.isVisible = false
            }
            is State.Loaded -> {
                val widgetHeight = when(state.replacement){
                    WidgetReplacement.TOP -> resources.getDimension(R.dimen.widget_preview_height_top)
                    WidgetReplacement.BOTTOM -> resources.getDimension(R.dimen.widget_preview_height_bottom)
                    else -> 0f
                }.toInt()
                binding.widgetReplacementPickerLoading.isVisible = false
                binding.widgetReplacementPickerLoaded.isVisible = true
                adapter.items = state.items
                adapter.widgetHeight = widgetHeight
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun setupSearch() = with(binding.widgetReplacementPickerSearch) {
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

    private fun setupRecyclerView() = with(binding.widgetReplacementPickerRecyclerview) {
        layoutManager = LinearLayoutManager(context)
        adapter = this@WidgetReplacementPickerFragment.adapter
        val inset = resources.getDimension(R.dimen.bottom_nav_height_margins) +
                resources.getDimension(R.dimen.margin_16)
        applyBottomNavigationInset(inset)
    }

    private fun onWidgetClicked(info: AppWidgetProviderInfo) {
        viewModel.onWidgetClicked(requireContext(), info, configurationLauncher, bindLauncher)
    }

    private fun sendResult(wasChanged: Boolean) {
        setFragmentResult(
            REQUEST_KEY_WIDGET_PROVIDER_PICKER,
            bundleOf(RESULT_EXTRA_WAS_CHANGED to wasChanged)
        )
    }

    private fun setupErrorBus() = viewLifecycleOwner.lifecycleScope.launchWhenResumed {
        viewModel.errorBus.collect {
            Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
        }
    }

}