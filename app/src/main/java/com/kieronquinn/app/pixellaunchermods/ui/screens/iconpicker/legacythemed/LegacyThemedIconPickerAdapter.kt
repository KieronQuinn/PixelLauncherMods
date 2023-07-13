package com.kieronquinn.app.pixellaunchermods.ui.screens.iconpicker.legacythemed

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.kieronquinn.app.pixellaunchermods.databinding.ItemIconBinding
import com.kieronquinn.app.pixellaunchermods.databinding.ItemIconLegacyThemedHeaderBinding
import com.kieronquinn.app.pixellaunchermods.model.icon.LegacyThemedIcon
import com.kieronquinn.app.pixellaunchermods.ui.screens.iconpicker.legacythemed.LegacyThemedIconPickerViewModel.Item

class LegacyThemedIconPickerAdapter(
    context: Context,
    var items: List<Item>,
    private val onItemClicked: (LegacyThemedIcon) -> Unit
): RecyclerView.Adapter<LegacyThemedIconPickerAdapter.ViewHolder>() {

    init {
        setHasStableIds(true)
    }

    private val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val glide = Glide.with(context)

    override fun getItemCount() = items.size

    override fun getItemId(position: Int): Long {
        return when(val item = items[position]){
            is Item.Icon -> item.icon.resourceId.toLong()
            is Item.Header -> -1
        }
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].type.ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when(Item.ItemType.values()[viewType]){
            Item.ItemType.ICON -> ViewHolder.Icon(ItemIconBinding.inflate(layoutInflater, parent, false))
            Item.ItemType.HEADER -> ViewHolder.Header(ItemIconLegacyThemedHeaderBinding.inflate(layoutInflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        when(holder){
            is ViewHolder.Icon -> holder.binding.setup(item as Item.Icon)
            else -> {
                //No-op
            }
        }
    }

    private fun ItemIconBinding.setup(item: Item.Icon) {
        with(iconIcon){
            glide.load(item.icon)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(this)
            setOnClickListener {
                onItemClicked(item.icon)
            }
        }
    }

    sealed class ViewHolder(open val binding: ViewBinding): RecyclerView.ViewHolder(binding.root) {
        data class Icon(override val binding: ItemIconBinding): ViewHolder(binding)
        data class Header(override val binding: ItemIconLegacyThemedHeaderBinding): ViewHolder(binding)
    }

}