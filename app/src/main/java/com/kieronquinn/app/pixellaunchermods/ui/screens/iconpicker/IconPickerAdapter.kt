package com.kieronquinn.app.pixellaunchermods.ui.screens.iconpicker

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.databinding.ItemIconPickerHeaderBinding
import com.kieronquinn.app.pixellaunchermods.databinding.ItemIconSourceBinding
import com.kieronquinn.app.pixellaunchermods.ui.screens.iconpicker.IconPickerViewModel.Source

class IconPickerAdapter(
    context: Context,
    var items: List<Source>,
    var mono: Boolean,
    val onIconClicked: (Source) -> Unit,
    val onSourceClicked: (Source) -> Unit,
): RecyclerView.Adapter<IconPickerAdapter.ViewHolder>() {

    private val layoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    private val glide = Glide.with(context)

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when(Source.Type.values()[viewType]){
            Source.Type.HEADER -> ViewHolder.Header(ItemIconPickerHeaderBinding.inflate(layoutInflater, parent, false))
            Source.Type.SOURCE -> ViewHolder.Source(ItemIconSourceBinding.inflate(layoutInflater, parent, false))
        }
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].type.ordinal
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val binding = holder.binding
        if(binding is ItemIconSourceBinding){
            when(item) {
                is Source.IconPackSource -> binding.setupIconSource(item)
                is Source.Apps -> binding.setupApps(item)
                is Source.File -> binding.setupFile()
                is Source.LegacyThemedIcons -> binding.setupLegacyThemedIcons(item)
                is Source.Lawnicons -> binding.setupLawnicons(item)
            }
        }
    }

    private fun ItemIconSourceBinding.setupIconSource(item: Source.IconPackSource){
        itemIconSourceMore.isVisible = item.icon != null
        itemIconSourceName.text = item.iconPack.label
        root.setOnClickListener {
            if(item.icon != null) {
                onIconClicked(item)
            }else{
                onSourceClicked(item)
            }
        }
        itemIconSourceMore.setOnClickListener {
            onSourceClicked(item)
        }
        //Default shrinking to true
        glide.load(item.icon ?: item.iconPackIcon)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(false)
            .into(itemIconSourceIcon)
    }

    private fun ItemIconSourceBinding.setupApps(item: Source.Apps){
        itemIconSourceMore.isVisible = true
        itemIconSourceName.setText(R.string.app_editor_iconpicker_apps)
        root.setOnClickListener { onIconClicked(item) }
        itemIconSourceMore.setOnClickListener { onSourceClicked(item) }
        glide.load(item.packageIcon.applicationIcon)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(false)
            .into(itemIconSourceIcon)
    }

    private fun ItemIconSourceBinding.setupLegacyThemedIcons(item: Source.LegacyThemedIcons){
        itemIconSourceMore.isVisible = item.legacyThemedIcon != null
        itemIconSourceName.setText(R.string.app_editor_iconpicker_legacy_themed_title)
        root.setOnClickListener {
            if(item.legacyThemedIcon != null) {
                onIconClicked(item)
            }else{
                onSourceClicked(item)
            }
        }
        itemIconSourceMore.setOnClickListener {
            onSourceClicked(item)
        }
        glide.load(item.legacyThemedIcon ?: item.applicationIcon)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(false)
            .into(itemIconSourceIcon)
    }

    private fun ItemIconSourceBinding.setupLawnicons(item: Source.Lawnicons){
        itemIconSourceMore.isVisible = item.legacyThemedIcon != null
        itemIconSourceName.setText(R.string.app_editor_iconpicker_lawnicons_title)
        root.setOnClickListener {
            if(item.legacyThemedIcon != null) {
                onIconClicked(item)
            }else{
                onSourceClicked(item)
            }
        }
        itemIconSourceMore.setOnClickListener {
            onSourceClicked(item)
        }
        glide.load(item.legacyThemedIcon ?: item.applicationIcon)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(false)
            .into(itemIconSourceIcon)
    }

    private fun ItemIconSourceBinding.setupFile(){
        itemIconSourceMore.isVisible = false
        itemIconSourceName.setText(R.string.app_editor_iconpicker_files)
        itemIconSourceIcon.setImageResource(R.drawable.ic_icon_source_file)
        root.setOnClickListener { onSourceClicked(Source.File) }
    }

    sealed class ViewHolder(open val binding: ViewBinding) : RecyclerView.ViewHolder(binding.root) {
        data class Header(override val binding: ItemIconPickerHeaderBinding): ViewHolder(binding)
        data class Source(override val binding: ItemIconSourceBinding): ViewHolder(binding)
    }

}