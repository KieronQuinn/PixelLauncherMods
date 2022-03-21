package com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.widgetresize

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.pixellaunchermods.databinding.FragmentWidgetResizePageBinding
import com.kieronquinn.app.pixellaunchermods.ui.base.BoundFragment
import com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.widgetresize.WidgetResizeViewModel.State
import com.kieronquinn.app.pixellaunchermods.utils.extensions.awaitPost
import com.kieronquinn.app.pixellaunchermods.utils.recyclerview.SpannedGridLayoutManager
import org.koin.androidx.viewmodel.ext.android.viewModel

class WidgetResizePageFragment: BoundFragment<FragmentWidgetResizePageBinding>(FragmentWidgetResizePageBinding::inflate) {

    companion object {
        private const val ARGUMENT_PAGE = "page"
        private const val ARGUMENT_SPAN_X = "span_x"
        private const val ARGUMENT_SPAN_Y = "span_y"

        fun newInstance(page: Int, spanX: Int, spanY: Int): WidgetResizePageFragment {
            return WidgetResizePageFragment().apply {
                arguments = bundleOf(
                    ARGUMENT_PAGE to page,
                    ARGUMENT_SPAN_X to spanX,
                    ARGUMENT_SPAN_Y to spanY
                )
            }
        }
    }

    private val viewModel by lazy {
        val parentViewModel by requireParentFragment().viewModel<WidgetResizeViewModel>()
        parentViewModel
    }

    private val adapter by lazy {
        WidgetResizeAdapter(
            binding.widgetResizePageGrid,
            emptyList(),
            null,
            viewModel::onWidgetClicked,
            viewModel::loadWidget
        )
    }

    private val layoutManager by lazy {
        SpannedGridLayoutManager(
            orientation = SpannedGridLayoutManager.Orientation.VERTICAL,
            spans = spanX
        ).apply {
            spanSizeLookup = SpannedGridLayoutManager.SpanSizeLookup(adapter::getSpan)
        }
    }

    private val page by lazy {
        requireArguments().getInt(ARGUMENT_PAGE)
    }

    private val spanX by lazy {
        requireArguments().getInt(ARGUMENT_SPAN_X)
    }

    private val spanY by lazy {
        requireArguments().getInt(ARGUMENT_SPAN_Y)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupTargets()
    }

    override fun onDestroyView() {
        binding.widgetResizePageGrid.adapter = null
        super.onDestroyView()
    }

    private fun setupRecyclerView() = with(binding.widgetResizePageGrid) {
        adapter = this@WidgetResizePageFragment.adapter
        layoutManager = this@WidgetResizePageFragment.layoutManager
    }

    private fun setupTargets() = viewLifecycleOwner.lifecycleScope.launchWhenResumed {
        binding.widgetResizePageGrid.awaitPost()
        viewModel.state.collect {
            val state = (it as? State.Loaded) ?: return@collect
            val page = state.pages[page]
            adapter.items = page ?: emptyList()
            adapter.selectedWidgetId = state.selectedWidget?.appWidgetId
            layoutManager.itemHeight = (binding.widgetResizePageGrid.measuredHeight / spanY.toFloat()).toInt()
            adapter.notifyDataSetChanged()
        }
    }

}