package com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.widgetresize

import android.os.Bundle
import android.util.Log
import android.view.MenuInflater
import android.view.View
import androidx.activity.addCallback
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel
import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.databinding.FragmentWidgetResizeBinding
import com.kieronquinn.app.pixellaunchermods.databinding.IncludeWidgetResizeSheetBinding
import com.kieronquinn.app.pixellaunchermods.ui.base.BoundFragment
import com.kieronquinn.app.pixellaunchermods.ui.base.ProvidesBack
import com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.widgetresize.WidgetResizeViewModel.State
import com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.widgetresize.WidgetResizeViewModel.Target
import com.kieronquinn.app.pixellaunchermods.utils.extensions.onApplyInsets
import com.kieronquinn.app.pixellaunchermods.utils.extensions.onNavigationIconClicked
import kotlinx.coroutines.flow.collect
import org.koin.androidx.viewmodel.ext.android.viewModel

class WidgetResizeFragment: BoundFragment<FragmentWidgetResizeBinding>(FragmentWidgetResizeBinding::inflate) {

    private val viewModel by viewModel<WidgetResizeViewModel>()

    private val menuInflater by lazy {
        MenuInflater(requireContext())
    }

    private val pagerAdapter by lazy {
        WidgetResizePagerAdapter(this, 0, 0, 0)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCard()
        setupInsets()
        setupToolbar()
        setupBack()
        setupViewPager()
        setupState()
        setupViewPagerIndicator()
        setupInput()
        setupSave()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.startListening()
    }

    override fun onDestroy() {
        viewModel.stopListening()
        super.onDestroy()
    }

    private fun setupBack() {
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            viewModel.onBackPressed()
        }
    }

    private fun setupCard() = with(binding.widgetResizeSheet){
        val cornerRadius = resources.getDimension(R.dimen.margin_16)
        shapeAppearanceModel = ShapeAppearanceModel().toBuilder().apply {
            setTopLeftCorner(CornerFamily.ROUNDED, cornerRadius)
            setTopRightCorner(CornerFamily.ROUNDED, cornerRadius)
        }.build()
    }

    private fun setupInsets() {
        val bottomPadding = resources.getDimension(R.dimen.margin_16).toInt()
        binding.widgetResizeSheetInclude.root.onApplyInsets { view, insets ->
            view.updatePadding(
                bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom + bottomPadding
            )
        }
        binding.widgetResizeViewpager.onApplyInsets { view, insets ->
            view.updatePadding(
                top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            )
        }
    }

    private fun setupToolbar() = with(binding.widgetResizeSheetInclude.widgetResizeSheetToolbar){
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            onNavigationIconClicked().collect {
                viewModel.onBackPressed()
            }
        }
    }

    private fun setupViewPager() = with(binding.widgetResizeViewpager) {
        adapter = pagerAdapter
    }

    private fun setupViewPagerIndicator() = with(binding.widgetResizeViewpagerIndicator) {
        setViewPager(binding.widgetResizeViewpager)
        setOnPageChangeListener(object: ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                viewModel.onWidgetClicked(null)
            }
        })
    }

    private fun setupInput() = with(binding.widgetResizeSheetInclude) {
        widgetResizeSheetResizeHorizontal.onPlusClicked = viewModel::onWidgetPlusXClicked
        widgetResizeSheetResizeHorizontal.onMinusClicked = viewModel::onWidgetMinusXClicked
        widgetResizeSheetResizeVertical.onPlusClicked = viewModel::onWidgetPlusYClicked
        widgetResizeSheetResizeVertical.onMinusClicked = viewModel::onWidgetMinusYClicked
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
                binding.widgetResizeLoading.isVisible = true
                binding.widgetResizeViewpager.isVisible = false
            }
            is State.Loaded -> {
                binding.widgetResizeLoading.isVisible = false
                binding.widgetResizeViewpager.isVisible = true
                binding.widgetResizeSheetInclude.setupWidgetSizeInput(state.selectedWidget)
                pagerAdapter.pageCount = state.pages.size
                pagerAdapter.spanX = state.gridSize.x
                pagerAdapter.spanY = state.gridSize.y
                pagerAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun IncludeWidgetResizeSheetBinding.setupWidgetSizeInput(widget: Target.Widget?) {
        if(widget == null){
            tweaksResizeWidgetsSheetSelected.isVisible = false
            tweaksResizeWidgetsSheetUnselected.isVisible = true
            return
        }
        tweaksResizeWidgetsSheetSelected.isVisible = true
        tweaksResizeWidgetsSheetUnselected.isVisible = false
        widgetResizeSheetResizeHorizontal.plusEnabled = widget.canExpandX
        widgetResizeSheetResizeHorizontal.minusEnabled = widget.canShrinkX
        widgetResizeSheetResizeVertical.plusEnabled = widget.canExpandY
        widgetResizeSheetResizeVertical.minusEnabled = widget.canShrinkY
    }

    private fun setupSave() {
        handleSave(viewModel.hasChanges.value)
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            viewModel.hasChanges.collect {
                handleSave(it)
            }
        }
    }

    private fun handleSave(isEnabled: Boolean) = with(binding.widgetResizeSheetInclude.widgetResizeSheetToolbar) {
        menu.clear()
        if(isEnabled){
            menuInflater.inflate(R.menu.menu_widget_resize, menu)
        }
        setOnMenuItemClickListener {
            when(it.itemId){
                R.id.menu_widget_resize_save -> viewModel.commitChanges()
            }
            true
        }
    }

}