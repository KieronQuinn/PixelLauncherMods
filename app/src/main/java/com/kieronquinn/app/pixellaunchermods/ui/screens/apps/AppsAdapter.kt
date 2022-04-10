package com.kieronquinn.app.pixellaunchermods.ui.screens.apps

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.kieronquinn.app.pixellaunchermods.databinding.ItemAppBinding
import com.kieronquinn.app.pixellaunchermods.databinding.ItemAppHeaderBinding
import com.kieronquinn.app.pixellaunchermods.model.remote.RemoteAppOptions
import com.kieronquinn.app.pixellaunchermods.ui.screens.apps.AppsViewModel.Item

class AppsAdapter(
    context: Context,
    var items: List<Item>,
    private val onItemClicked: (Item) -> Unit
) : RecyclerView.Adapter<AppsAdapter.ViewHolder>() {

    init {
        setHasStableIds(true)
    }

    private val layoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    private val glide = Glide.with(context)

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when(Item.Type.values()[viewType]){
            Item.Type.HEADER ->
                ViewHolder.Header(ItemAppHeaderBinding.inflate(layoutInflater, parent, false))
            Item.Type.APP ->
                ViewHolder.App(ItemAppBinding.inflate(layoutInflater, parent, false))
        }
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].type.ordinal
    }

    override fun getItemId(position: Int): Long {
        return items[position].hashCode().toLong()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when(holder){
            is ViewHolder.Header -> holder.binding.applyHeader(items[position])
            is ViewHolder.App -> holder.binding.applyRemoteApp(items[position] as Item.App)
        }
    }

    private fun ItemAppHeaderBinding.applyHeader(item: Item) {
        itemAppHeaderIconPack.setOnClickListener {
            onItemClicked(item)
        }
    }

    private fun ItemAppBinding.applyRemoteApp(item: Item.App) {
        val app = item.app
        appLabel.text = app.label
        //Pass the focus down to the pressed state of the icon for the scale animation
        root.setOnFocusChangeListener { _, isFocused ->
            appIcon.isPressed = isFocused
        }
        root.setOnClickListener {
            onItemClicked(item)
        }
        glide.load(RemoteAppOptions(app, false))
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .into(appIcon)
    }

    sealed class ViewHolder(open val binding: ViewBinding): RecyclerView.ViewHolder(binding.root) {
        data class Header(override val binding: ItemAppHeaderBinding): ViewHolder(binding)
        data class App(override val binding: ItemAppBinding): ViewHolder(binding)
    }

}