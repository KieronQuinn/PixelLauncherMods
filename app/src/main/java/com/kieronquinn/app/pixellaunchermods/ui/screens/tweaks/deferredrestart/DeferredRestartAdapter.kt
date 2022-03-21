package com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.deferredrestart

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.kieronquinn.app.pixellaunchermods.databinding.ItemDeferredRestartBinding
import com.kieronquinn.app.pixellaunchermods.databinding.ItemDeferredRestartHeaderBinding
import com.kieronquinn.app.pixellaunchermods.repositories.SettingsRepository
import com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.deferredrestart.DeferredRestartViewModel.Item
import com.kieronquinn.app.pixellaunchermods.ui.views.LifecycleAwareRecyclerView
import com.kieronquinn.app.pixellaunchermods.utils.extensions.onClicked

class DeferredRestartAdapter(
    recyclerView: RecyclerView,
    var items: List<Item>,
    val onItemClicked: (SettingsRepository.DeferredRestartMode) -> Unit
): LifecycleAwareRecyclerView.Adapter<DeferredRestartAdapter.ViewHolder>(recyclerView) {

    init {
        setHasStableIds(true)
    }

    private val layoutInflater =
        recyclerView.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getItemId(position: Int): Long {
        return when(val item = items[position]){
            is Item.Option -> {
                item.mode.ordinal.toLong()
            }
            is Item.Header -> {
                -1L
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].type.ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when(Item.Type.values()[viewType]){
            Item.Type.HEADER -> {
                ViewHolder.Header(ItemDeferredRestartHeaderBinding.inflate(layoutInflater, parent, false))
            }
            Item.Type.OPTION -> {
                ViewHolder.Item(ItemDeferredRestartBinding.inflate(layoutInflater, parent, false))
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when(holder){
            is ViewHolder.Item -> holder.binding.setup(items[position] as Item.Option, holder)
            is ViewHolder.Header -> {} //Nothing to do
        }
    }

    private fun ItemDeferredRestartBinding.setup(item: Item.Option, holder: ViewHolder) {
        itemDeferredRestartTitle.setText(item.mode.titleRes)
        itemDeferredRestartContent.setText(item.mode.contentRes)
        itemDeferredRestartRadio.isChecked = item.isSelected
        itemDeferredRestartRadio.setOnClickListener { root.callOnClick() }
        holder.lifecycleScope.launchWhenResumed {
            root.onClicked().collect {
                onItemClicked(item.mode)
            }
        }
    }

    sealed class ViewHolder(open val binding: ViewBinding): LifecycleAwareRecyclerView.ViewHolder(binding.root) {
        data class Header(override val binding: ItemDeferredRestartHeaderBinding): ViewHolder(binding)
        data class Item(override val binding: ItemDeferredRestartBinding): ViewHolder(binding)
    }

}