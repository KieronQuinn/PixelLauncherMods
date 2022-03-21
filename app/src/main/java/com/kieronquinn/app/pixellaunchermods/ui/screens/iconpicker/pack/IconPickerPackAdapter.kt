package com.kieronquinn.app.pixellaunchermods.ui.screens.iconpicker.pack

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.kieronquinn.app.pixellaunchermods.databinding.ItemIconPackCategoryBinding
import com.kieronquinn.app.pixellaunchermods.databinding.ItemIconPackIconBinding
import com.kieronquinn.app.pixellaunchermods.ui.screens.iconpicker.pack.IconPickerPackViewModel.Item
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView.SectionedAdapter

class IconPickerPackAdapter(
    context: Context,
    var items: List<Item>,
    private val onIconClicked: (Item.Icon) -> Unit
): RecyclerView.Adapter<IconPickerPackAdapter.ViewHolder>(), SectionedAdapter {

    init {
        setHasStableIds(true)
    }

    private val layoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    private val glide = Glide.with(context)

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int): Int {
        return items[position].itemType.ordinal
    }

    override fun getSectionName(position: Int): String {
        return items[position].section
    }

    override fun getItemId(position: Int): Long {
        return when(val item = items[position]) {
            is Item.Category -> "CATEGORY:${item.iconPackIconCategory.name}"
            is Item.Icon -> item.iconPackIcon.iconPackIcon.resourceName
        }.hashCode().toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when(Item.ItemType.values()[viewType]){
            Item.ItemType.ICON -> {
                ViewHolder.Icon(
                    ItemIconPackIconBinding.inflate(layoutInflater, parent, false)
                )
            }
            Item.ItemType.CATEGORY -> {
                ViewHolder.Category(
                    ItemIconPackCategoryBinding.inflate(layoutInflater, parent, false)
                )
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when(holder){
            is ViewHolder.Icon -> holder.binding.setup(items[position] as Item.Icon)
            is ViewHolder.Category -> holder.binding.setup(items[position] as Item.Category)
        }
    }

    private fun ItemIconPackIconBinding.setup(icon: Item.Icon) {
        glide.load(icon.iconPackIcon)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(false)
            .into(iconPackIcon)
        iconPackIcon.setOnClickListener {
            onIconClicked(icon)
        }
    }

    private fun ItemIconPackCategoryBinding.setup(category: Item.Category) {
        iconPackCategory.text = category.iconPackIconCategory.name
    }

    sealed class ViewHolder(open val binding: ViewBinding): RecyclerView.ViewHolder(binding.root) {
        data class Icon(override val binding: ItemIconPackIconBinding): ViewHolder(binding)
        data class Category(override val binding: ItemIconPackCategoryBinding): ViewHolder(binding)
    }
}