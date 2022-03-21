package com.kieronquinn.app.pixellaunchermods.ui.screens.autoiconpack

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.databinding.FragmentAutoIconPackBinding
import com.kieronquinn.app.pixellaunchermods.ui.base.BackAvailable
import com.kieronquinn.app.pixellaunchermods.ui.base.BoundFragment
import com.kieronquinn.app.pixellaunchermods.ui.base.LockCollapsed
import com.kieronquinn.app.pixellaunchermods.ui.screens.autoiconpack.AutoIconPackViewModel.State
import com.kieronquinn.app.pixellaunchermods.utils.extensions.applyBottomNavigationInset
import com.kieronquinn.app.pixellaunchermods.utils.extensions.applyBottomNavigationMargin
import com.kieronquinn.app.pixellaunchermods.utils.extensions.onClicked
import org.koin.androidx.viewmodel.ext.android.viewModel
import androidx.recyclerview.widget.ItemTouchHelper as RecyclerViewItemTouchHelper

class AutoIconPackFragment: BoundFragment<FragmentAutoIconPackBinding>(FragmentAutoIconPackBinding::inflate), BackAvailable, LockCollapsed {

    private val adapter by lazy {
        AutoIconPackAdapter(requireContext(), ArrayList(), itemTouchHelper::startDrag)
    }

    private val itemTouchHelper by lazy {
        RecyclerViewItemTouchHelper(ItemTouchHelper())
    }

    private val viewModel by viewModel<AutoIconPackViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupState()
        setupFab()
        setupToast()
    }

    private fun setupRecyclerView() = with(binding.autoIconPackRecyclerview) {
        layoutManager = LinearLayoutManager(context)
        adapter = this@AutoIconPackFragment.adapter
        val inset = resources.getDimension(R.dimen.bottom_nav_height_margins) +
                resources.getDimension(R.dimen.margin_16)
        itemTouchHelper.attachToRecyclerView(this)
        applyBottomNavigationInset(inset)
    }

    private fun setupFab() = with(binding.autoIconPackFab) {
        applyBottomNavigationMargin()
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            onClicked().collect {
                viewModel.onApplyClicked()
            }
        }
    }

    private fun setupToast() = viewLifecycleOwner.lifecycleScope.launchWhenResumed {
        viewModel.toastBus.collect {
            Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
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
        when(state) {
            is State.Loading -> {
                binding.autoIconPackLoading.isVisible = true
                binding.autoIconPackError.isVisible = false
                binding.autoIconPackRecyclerview.isVisible = false
                binding.autoIconPackFab.isVisible = false
            }
            is State.Empty -> {
                binding.autoIconPackLoading.isVisible = false
                binding.autoIconPackError.isVisible = true
                binding.autoIconPackRecyclerview.isVisible = false
                binding.autoIconPackFab.isVisible = false
            }
            is State.Loaded -> {
                binding.autoIconPackLoading.isVisible = false
                binding.autoIconPackError.isVisible = false
                binding.autoIconPackRecyclerview.isVisible = true
                binding.autoIconPackFab.isVisible = true
                adapter.items = state.items
                adapter.notifyDataSetChanged()
            }
        }
    }

    private inner class ItemTouchHelper: RecyclerViewItemTouchHelper.Callback() {

        override fun isLongPressDragEnabled() = false
        override fun isItemViewSwipeEnabled() = false

        override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ): Int {
            return when(viewHolder){
                is AutoIconPackAdapter.ViewHolder.IconPack -> {
                    RecyclerViewItemTouchHelper.UP or RecyclerViewItemTouchHelper.DOWN
                }
                else -> 0
            }.run {
                makeMovementFlags(this, 0)
            }
        }

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            if(target is AutoIconPackAdapter.ViewHolder.Header){
                return false // Can't move the header
            }
            adapter.onRowMoved(viewHolder.absoluteAdapterPosition, target.absoluteAdapterPosition)
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            //No-op
        }

        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            super.onSelectedChanged(viewHolder, actionState)
            if(actionState != RecyclerViewItemTouchHelper.ACTION_STATE_IDLE){
                adapter.onRowSelected(viewHolder as? AutoIconPackAdapter.ViewHolder ?: return)
            }
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            adapter.onRowCleared(viewHolder as? AutoIconPackAdapter.ViewHolder ?: return)
        }

    }

}