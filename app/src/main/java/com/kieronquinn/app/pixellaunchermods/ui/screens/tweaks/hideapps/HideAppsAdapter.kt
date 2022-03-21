package com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.hideapps

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kieronquinn.app.pixellaunchermods.databinding.ItemHideAppBinding
import com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.hideapps.HideAppsViewModel.HiddenApp

class HideAppsAdapter(
    context: Context,
    var items: List<HiddenApp>
): RecyclerView.Adapter<HideAppsAdapter.ViewHolder>() {

    init {
        setHasStableIds(true)
    }

    private val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    private val glide = Glide.with(context)

    override fun getItemCount() = items.size

    override fun getItemId(position: Int): Long {
        return items[position].launcherApp.componentName.hashCode().toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemHideAppBinding.inflate(layoutInflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = with(holder.binding) {
        val item = items[position]
        itemHideAppName.text = item.launcherApp.label
        glide.load(item.launcherApp).into(itemHideAppIcon)
        itemHideAppSwitch.setOnCheckedChangeListener(null)
        itemHideAppSwitch.isChecked = item.hidden
        itemHideAppSwitch.setOnCheckedChangeListener { _, b ->
            item.hidden = b
        }
        root.setOnClickListener {
            itemHideAppSwitch.toggle()
        }
    }

    fun deselectAll() {
        items.forEachIndexed { index, hiddenApp ->
            if(hiddenApp.hidden) {
                hiddenApp.hidden = false
                notifyItemChanged(index)
            }
        }
    }

    class ViewHolder(val binding: ItemHideAppBinding): RecyclerView.ViewHolder(binding.root)

}