package com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.widgetreplacement.widgetpicker

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RemoteViews
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.databinding.ItemTweaksWidgetReplacementPickerHeaderBinding
import com.kieronquinn.app.pixellaunchermods.databinding.ItemWidgetReplacementPickerAppBinding
import com.kieronquinn.app.pixellaunchermods.databinding.ItemWidgetReplacementPickerWidgetImageBinding
import com.kieronquinn.app.pixellaunchermods.databinding.ItemWidgetReplacementPickerWidgetLayoutBinding
import com.kieronquinn.app.pixellaunchermods.model.icon.ApplicationIcon
import com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.widgetreplacement.widgetpicker.WidgetReplacementPickerViewModel.Item
import com.kieronquinn.app.pixellaunchermods.ui.views.LifecycleAwareRecyclerView
import com.kieronquinn.app.pixellaunchermods.utils.extensions.onClicked

class WidgetReplacementPickerAdapter(
    recyclerView: RecyclerView,
    var items: List<Item>,
    var widgetHeight: Int,
    private val onAppClicked: (Item.App) -> Array<Int>,
    private val onWidgetClicked: (AppWidgetProviderInfo) -> Unit
): LifecycleAwareRecyclerView.Adapter<WidgetReplacementPickerAdapter.ViewHolder>(recyclerView) {

    init {
        setHasStableIds(true)
    }

    private val layoutInflater = recyclerView.context
        .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val glide = Glide.with(recyclerView.context)

    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemId(position: Int): Long {
        return items[position].id.hashCode().toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].type.ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when(Item.Type.values()[viewType]){
            Item.Type.HEADER -> ViewHolder.Header(
                ItemTweaksWidgetReplacementPickerHeaderBinding.inflate(layoutInflater, parent, false)
            )
            Item.Type.APP -> ViewHolder.App(
                ItemWidgetReplacementPickerAppBinding.inflate(layoutInflater, parent, false)
            )
            Item.Type.WIDGET_IMAGE -> ViewHolder.WidgetImage(
                ItemWidgetReplacementPickerWidgetImageBinding.inflate(layoutInflater, parent, false)
            )
            Item.Type.WIDGET_LAYOUT -> ViewHolder.WidgetLayout(
                ItemWidgetReplacementPickerWidgetLayoutBinding.inflate(layoutInflater, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when(holder){
            is ViewHolder.App -> holder.binding.setup(items[position] as Item.App, holder)
            is ViewHolder.WidgetImage -> holder.binding.setup(items[position] as Item.WidgetImage, holder)
            is ViewHolder.WidgetLayout -> holder.binding.setup(items[position] as Item.WidgetLayout, holder)
            is ViewHolder.Header -> {} //Nothing to do
        }
    }

    private fun ItemWidgetReplacementPickerAppBinding.setup(app: Item.App, holder: ViewHolder) {
        val context = root.context
        itemWidgetReplacementPickerAppLabel.text = app.label
        itemWidgetReplacementPickerAppCount.text = context.resources.getQuantityString(
            R.plurals.widget_replacement_picker_item_app_count, app.widgetCount, app.widgetCount
        )
        glide.load(ApplicationIcon(app.appInfo, true, mono = false))
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(false)
            .into(itemWidgetReplacementPickerAppIcon)
        itemWidgetReplacementPickerAppExpand.rotation = if(app.isOpen) 180f else 0f
        itemWidgetReplacementPickerAppExpand.setOnClickListener { root.callOnClick() }
        holder.lifecycleScope.launchWhenResumed {
            root.onClicked().collect {
                toggleItem(app, itemWidgetReplacementPickerAppExpand)
            }
        }
    }

    private fun ItemWidgetReplacementPickerWidgetImageBinding.setup(widget: Item.WidgetImage, holder: ViewHolder) {
        val isVisible = widget.parent.isOpen
        root.isVisible = isVisible
        root.updateLayoutParams<RecyclerView.LayoutParams> {
            height = if (isVisible) RecyclerView.LayoutParams.WRAP_CONTENT else 0
        }
        if(!isVisible) return
        itemWidgetReplacementPickerWidgetImage.updateLayoutParams<ConstraintLayout.LayoutParams> {
            matchConstraintMaxHeight = widgetHeight
        }
        itemWidgetReplacementPickerWidgetLabel.text = widget.label
        itemWidgetReplacementPickerWidgetDescription.let {
            if(widget.description != null){
                it.isVisible = true
                it.text = widget.description
            }else{
                it.isVisible = false
            }
        }
        glide.load(widget.info)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(false)
            .into(itemWidgetReplacementPickerWidgetImage)
        holder.lifecycleScope.launchWhenResumed {
            root.onClicked().collect {
                onWidgetClicked(widget.info)
            }
        }
    }

    private fun ItemWidgetReplacementPickerWidgetLayoutBinding.setup(widget: Item.WidgetLayout, holder: ViewHolder) {
        val isVisible = widget.parent.isOpen
        root.isVisible = isVisible
        root.updateLayoutParams<RecyclerView.LayoutParams> {
            height = if (isVisible) RecyclerView.LayoutParams.WRAP_CONTENT else 0
        }
        if(!isVisible) return
        itemWidgetReplacementPickerWidgetLayout.updateLayoutParams<ConstraintLayout.LayoutParams> {
            matchConstraintMaxHeight = widgetHeight
        }
        itemWidgetReplacementPickerWidgetLabel.text = widget.label
        itemWidgetReplacementPickerWidgetDescription.let {
            if(widget.description != null){
                it.isVisible = true
                it.text = widget.description
            }else{
                it.isVisible = false
            }
        }
        itemWidgetReplacementPickerWidgetLayout.run {
            removeAllViews()
            val info = widget.info
            val hostView = AppWidgetHostView(root.context.applicationContext)
            hostView.setAppWidget(-1, info)
            hostView.updateAppWidget(RemoteViews(info.activityInfo.packageName, info.previewLayout))
            addView(hostView)
        }
        holder.lifecycleScope.launchWhenResumed {
            root.onClicked().collect {
                onWidgetClicked(widget.info)
            }
        }
    }

    private fun toggleItem(item: Item.App, expandIcon: View) {
        val itemsToUpdate = onAppClicked(item)
        if (itemsToUpdate.isNotEmpty()) {
            item.isOpen = !item.isOpen
            if (item.isOpen) {
                expandIcon.animate().rotation(180f).start()
            } else {
                expandIcon.animate().rotation(0f).start()
            }
        }
        itemsToUpdate.forEach {
            notifyItemChanged(it)
        }
    }

    sealed class ViewHolder(open val binding: ViewBinding): LifecycleAwareRecyclerView.ViewHolder(binding.root) {
        data class Header(override val binding: ItemTweaksWidgetReplacementPickerHeaderBinding): ViewHolder(binding)
        data class App(override val binding: ItemWidgetReplacementPickerAppBinding): ViewHolder(binding)
        data class WidgetImage(override val binding: ItemWidgetReplacementPickerWidgetImageBinding): ViewHolder(binding)
        data class WidgetLayout(override val binding: ItemWidgetReplacementPickerWidgetLayoutBinding): ViewHolder(binding)
    }

}