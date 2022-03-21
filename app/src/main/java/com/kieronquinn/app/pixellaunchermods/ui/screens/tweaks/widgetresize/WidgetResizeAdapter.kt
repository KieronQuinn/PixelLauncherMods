package com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.widgetresize

import android.appwidget.AppWidgetHostView
import android.content.Context
import android.os.Bundle
import android.util.SizeF
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.databinding.ItemWidgetResizeTargetShortcutBinding
import com.kieronquinn.app.pixellaunchermods.databinding.ItemWidgetResizeTargetSpaceBinding
import com.kieronquinn.app.pixellaunchermods.databinding.ItemWidgetResizeTargetWidgetBinding
import com.kieronquinn.app.pixellaunchermods.model.icon.ApplicationIcon
import com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.widgetresize.WidgetResizeViewModel.Target
import com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.widgetresize.WidgetResizeViewModel.Target.Type
import com.kieronquinn.app.pixellaunchermods.ui.views.LifecycleAwareRecyclerView
import com.kieronquinn.app.pixellaunchermods.utils.extensions.onClicked
import com.kieronquinn.app.pixellaunchermods.utils.recyclerview.SpanSize
import kotlinx.coroutines.flow.collect

class WidgetResizeAdapter(
    recyclerView: LifecycleAwareRecyclerView,
    var items: List<Target>,
    var selectedWidgetId: Int?,
    private val onWidgetClicked: (Target.Widget) -> Unit,
    private val loadWidget: suspend (Context, Target.Widget) -> AppWidgetHostView
): LifecycleAwareRecyclerView.Adapter<WidgetResizeAdapter.ViewHolder>(recyclerView) {

    private val layoutInflater =
        recyclerView.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val glide = Glide.with(recyclerView.context)

    override fun getItemViewType(position: Int): Int {
        return items[position].type.ordinal
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when(Type.values()[viewType]){
            Type.SHORTCUT -> ViewHolder.Shortcut(ItemWidgetResizeTargetShortcutBinding.inflate(layoutInflater, parent, false))
            Type.WIDGET -> ViewHolder.Widget(ItemWidgetResizeTargetWidgetBinding.inflate(layoutInflater, parent, false))
            Type.SPACE -> ViewHolder.Space(ItemWidgetResizeTargetSpaceBinding.inflate(layoutInflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when(holder){
            is ViewHolder.Shortcut -> holder.binding.setup(items[position] as Target.Shortcut)
            is ViewHolder.Widget -> holder.binding.setup(items[position] as Target.Widget, holder.lifecycleScope)
            is ViewHolder.Space -> {} //Nothing to do
        }
    }

    private fun ItemWidgetResizeTargetWidgetBinding.setup(widget: Target.Widget, scope: LifecycleCoroutineScope) {
        widgetResizeTargetWidgetTouch.setOnTouchListener { view, motionEvent ->
            onWidgetClicked(widget)
            true
        }
        if(selectedWidgetId != null && widget.appWidgetId == selectedWidgetId){
            root.setBackgroundResource(R.drawable.widget_background_selected)
        }else root.background = null
        scope.launchWhenCreated {
            val widgetView = loadWidget(root.context, widget)
            widgetView.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            widgetView.updateAppWidgetSize(
                Bundle.EMPTY,
                listOf(SizeF(root.measuredWidth.toFloat(), root.measuredHeight.toFloat()))
            )
            widgetView.alpha = if(selectedWidgetId != null && widget.appWidgetId != selectedWidgetId) 0.5f else 1f
            (widgetView.parent as? ViewGroup)?.removeView(widgetView)
            root.addView(widgetView, 0)
        }
    }

    private fun ItemWidgetResizeTargetShortcutBinding.setup(shortcut: Target.Shortcut) {
        root.alpha = if(selectedWidgetId != null) 0.5f else 1f
        appLabel.text = shortcut.label
        glide.load(shortcut.applicationInfo?.let {
            ApplicationIcon(it, shrinkNonAdaptiveIcons = false, mono = false) })
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .into(appIcon)
    }

    fun getSpan(position: Int): SpanSize {
        val item = items[position]
        return SpanSize(item.spanX, item.spanY)
    }

    sealed class ViewHolder(open val binding: ViewBinding): LifecycleAwareRecyclerView.ViewHolder(binding.root) {
        data class Shortcut(override val binding: ItemWidgetResizeTargetShortcutBinding): ViewHolder(binding)
        data class Widget(override val binding: ItemWidgetResizeTargetWidgetBinding): ViewHolder(binding)
        data class Space(override val binding: ItemWidgetResizeTargetSpaceBinding): ViewHolder(binding)
    }

}