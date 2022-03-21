package com.kieronquinn.app.pixellaunchermods.ui.screens.autoiconpack

import android.R.attr.data
import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.kieronquinn.app.pixellaunchermods.databinding.ItemAutoIconPackBinding
import com.kieronquinn.app.pixellaunchermods.databinding.ItemAutoIconPackHeaderBinding
import com.kieronquinn.app.pixellaunchermods.databinding.ItemAutoIconPackLineBinding
import com.kieronquinn.app.pixellaunchermods.ui.screens.autoiconpack.AutoIconPackViewModel.Item
import java.util.*
import kotlin.collections.ArrayList


class AutoIconPackAdapter(
    context: Context,
    var items: ArrayList<Item>,
    private val onHandleDrag: (ViewHolder) -> Unit
): RecyclerView.Adapter<AutoIconPackAdapter.ViewHolder>() {

    private val layoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    private val glide = Glide.with(context)

    override fun getItemViewType(position: Int): Int {
        return items[position].type.ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when(Item.ItemType.values()[viewType]){
            Item.ItemType.HEADER -> {
                ViewHolder.Header(ItemAutoIconPackHeaderBinding.inflate(layoutInflater, parent, false))
            }
            Item.ItemType.PACK -> {
                ViewHolder.IconPack(ItemAutoIconPackBinding.inflate(layoutInflater, parent, false))
            }
            Item.ItemType.LINE -> {
                ViewHolder.Line(ItemAutoIconPackLineBinding.inflate(layoutInflater, parent, false))
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if(holder is ViewHolder.IconPack){
            holder.binding.setup(items[position] as Item.Pack, holder)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun ItemAutoIconPackBinding.setup(item: Item.Pack, viewHolder: ViewHolder) {
        val iconPack = item.iconPack
        itemAutoIconPackName.text = iconPack.label
        itemAutoIconPackHandle.setOnTouchListener { view, motionEvent ->
            if(motionEvent.action == MotionEvent.ACTION_DOWN) {
                onHandleDrag(viewHolder)
            }
            true
        }
        glide.load(iconPack.iconPackIcon)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .into(itemAutoIconPackIcon)
    }

    override fun getItemCount() = items.size

    fun onRowMoved(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(items, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(items, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)
    }

    fun onRowSelected(viewHolder: ViewHolder) {
        viewHolder.binding.root.alpha = 0.5f
    }

    fun onRowCleared(viewHolder: ViewHolder) {
        viewHolder.binding.root.alpha = 1f
    }

    sealed class ViewHolder(open val binding: ViewBinding) : RecyclerView.ViewHolder(binding.root) {
        data class Header(override val binding: ItemAutoIconPackHeaderBinding) : ViewHolder(binding)
        data class IconPack(override val binding: ItemAutoIconPackBinding) : ViewHolder(binding)
        data class Line(override val binding: ItemAutoIconPackLineBinding) : ViewHolder(binding)
    }

}