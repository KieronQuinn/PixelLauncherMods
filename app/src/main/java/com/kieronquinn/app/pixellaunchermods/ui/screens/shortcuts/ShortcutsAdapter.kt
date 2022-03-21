package com.kieronquinn.app.pixellaunchermods.ui.screens.shortcuts

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.kieronquinn.app.pixellaunchermods.databinding.ItemAppBinding
import com.kieronquinn.app.pixellaunchermods.model.remote.RemoteAppOptions
import com.kieronquinn.app.pixellaunchermods.repositories.RemoteAppsRepository.Shortcut

class ShortcutsAdapter(
    context: Context,
    var items: List<Shortcut>,
    var themedIconsEnabled: Boolean,
    private val onItemClicked: (Shortcut) -> Unit
) : RecyclerView.Adapter<ShortcutsAdapter.ViewHolder>() {

    init {
        setHasStableIds(true)
    }

    private val layoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    private val glide = Glide.with(context)

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.App(ItemAppBinding.inflate(layoutInflater, parent, false))
    }

    override fun getItemId(position: Int): Long {
        return items[position].hashCode().toLong()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when(holder){
            is ViewHolder.App -> holder.binding.applyRemoteApp(items[position])
        }
    }

    private fun ItemAppBinding.applyRemoteApp(item: Shortcut) {
        appLabel.text = item.label
        //Pass the focus down to the pressed state of the icon for the scale animation
        root.setOnFocusChangeListener { _, isFocused ->
            appIcon.isPressed = isFocused
        }
        root.setOnClickListener {
            onItemClicked(item)
        }
        val glideLoadObject: Any = when (item) {
            is Shortcut.AppShortcut -> {
                RemoteAppOptions(item.shortcut, themedIconsEnabled)
            }
            is Shortcut.LegacyShortcut -> item.shortcut
        }
        glide.load(glideLoadObject)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .into(appIcon)
    }

    sealed class ViewHolder(open val binding: ViewBinding): RecyclerView.ViewHolder(binding.root) {
        data class App(override val binding: ItemAppBinding): ViewHolder(binding)
    }

}