package com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.widgetreplacement

import android.appwidget.AppWidgetHostView
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.databinding.ItemSettingsTextItemBinding
import com.kieronquinn.app.pixellaunchermods.databinding.ItemTweaksWidgetReplacementIncompatibleBinding
import com.kieronquinn.app.pixellaunchermods.databinding.ItemTweaksWidgetReplacementInfoBinding
import com.kieronquinn.app.pixellaunchermods.databinding.ItemTweaksWidgetReplacementPreviewBinding
import com.kieronquinn.app.pixellaunchermods.databinding.ItemTweaksWidgetReplacementSwitchBinding
import com.kieronquinn.app.pixellaunchermods.model.tweaks.WidgetReplacement
import com.kieronquinn.app.pixellaunchermods.model.tweaks.WidgetReplacementOptions
import com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.widgetreplacement.WidgetReplacementViewModel.Item
import com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.widgetreplacement.WidgetReplacementViewModel.WidgetPosition
import com.kieronquinn.app.pixellaunchermods.ui.views.LifecycleAwareRecyclerView
import com.kieronquinn.app.pixellaunchermods.utils.extensions.onClicked
import com.kieronquinn.app.pixellaunchermods.utils.widget.PreviewAppWidgetHostView
import kotlinx.coroutines.launch

class WidgetReplacementAdapter(
    recyclerView: RecyclerView,
    var items: List<Item>,
    private val getWidgetView: suspend (WidgetPosition) -> AppWidgetHostView?,
    private val onSwitchChanged: (Boolean) -> Unit,
    private val onSelectClicked: () -> Unit,
    private val onReconfigureClicked: () -> Unit
): LifecycleAwareRecyclerView.Adapter<WidgetReplacementAdapter.ViewHolder>(recyclerView) {

    init {
        setHasStableIds(true)
    }

    private val layoutInflater =
        recyclerView.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val glide = Glide.with(recyclerView.context)

    override fun getItemCount() = items.size

    override fun getItemId(position: Int): Long {
        return items[position].type.ordinal.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].type.ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when(Item.Type.values()[viewType]){
            Item.Type.SWITCH -> ViewHolder.Switch(
                ItemTweaksWidgetReplacementSwitchBinding.inflate(layoutInflater, parent, false)
            )
            Item.Type.PREVIEW -> ViewHolder.Preview(
                ItemTweaksWidgetReplacementPreviewBinding.inflate(layoutInflater, parent, false)
            )
            Item.Type.PROVIDER_PICKER -> ViewHolder.ProviderPicker(
                ItemSettingsTextItemBinding.inflate(layoutInflater, parent, false)
            )
            Item.Type.PROVIDER_RECONFIGURE -> ViewHolder.ProviderReconfigure(
                ItemSettingsTextItemBinding.inflate(layoutInflater, parent, false)
            )
            Item.Type.INFO -> ViewHolder.Info(
                ItemTweaksWidgetReplacementInfoBinding.inflate(layoutInflater, parent, false)
            )
            Item.Type.INCOMPATIBLE -> ViewHolder.Incompatible(
                ItemTweaksWidgetReplacementIncompatibleBinding.inflate(layoutInflater, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when(holder){
            is ViewHolder.Switch -> holder.binding.setup(items[position] as Item.Switch, holder)
            is ViewHolder.Preview -> holder.binding.setup(items[position] as Item.Preview, holder)
            is ViewHolder.ProviderPicker -> holder.binding.setup(items[position] as Item.ProviderPicker, holder)
            is ViewHolder.ProviderReconfigure -> holder.binding.setup(items[position] as Item.ProviderReconfigure, holder)
            is ViewHolder.Info -> {} //Nothing to do
            is ViewHolder.Incompatible -> {} //Nothing to do
        }
    }

    private fun ItemTweaksWidgetReplacementSwitchBinding.setup(item: Item.Switch, holder: ViewHolder) {
        tweaksWidgetReplacementSwitch.isChecked = item.enabled
        holder.lifecycleScope.launchWhenResumed {
            tweaksWidgetReplacementSwitch.onClicked().collect {
                onSwitchChanged(tweaksWidgetReplacementSwitch.isChecked)
            }
        }
    }

    private fun ItemTweaksWidgetReplacementPreviewBinding.setup(item: Item.Preview, holder: ViewHolder) {
        holder.lifecycleScope.launchWhenResumed {
            val widgetViewBottom = getWidgetView(WidgetPosition.BOTTOM) as? PreviewAppWidgetHostView
            if(widgetViewBottom != null){
                launch {
                    widgetViewBottom.onChanged().collect {
                        glide.load(WidgetReplacementOptions(widgetViewBottom, WidgetReplacement.BOTTOM))
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .into(itemTweaksWidgetReplacementPreviewBottomWidgetContainer)
                    }
                }
            }
        }
    }

    private fun ItemSettingsTextItemBinding.setup(item: Item.ProviderPicker, holder: ViewHolder) {
        itemSettingsTextTitle.setText(R.string.tweaks_widget_replacement_provider)
        val provider = item.provider?.loadLabel(root.context.packageManager)
        itemSettingsTextContent.text = if(provider != null){
            root.context.getString(R.string.tweaks_widget_replacement_provider_content_set, provider)
        }else{
            root.context.getString(R.string.tweaks_widget_replacement_provider_content_unset)
        }
        itemSettingsTextContent.isVisible = true
        itemSettingsTextIcon.setImageResource(R.drawable.ic_tweaks_widget_replacement)
        holder.lifecycleScope.launchWhenResumed {
            root.onClicked().collect {
                onSelectClicked()
            }
        }
    }

    private fun ItemSettingsTextItemBinding.setup(item: Item.ProviderReconfigure, holder: ViewHolder) {
        itemSettingsTextTitle.setText(R.string.tweaks_widget_replacement_provider_reconfigure)
        itemSettingsTextContent.setText(R.string.tweaks_widget_replacement_provider_reconfigure_content)
        itemSettingsTextContent.isVisible = true
        itemSettingsTextIcon.setImageResource(R.drawable.ic_tweaks_widget_replacement_reconfigure)
        holder.lifecycleScope.launchWhenResumed {
            root.onClicked().collect {
                onReconfigureClicked()
            }
        }
    }

    sealed class ViewHolder(open val binding: ViewBinding): LifecycleAwareRecyclerView.ViewHolder(binding.root) {
        data class Switch(override val binding: ItemTweaksWidgetReplacementSwitchBinding): ViewHolder(binding)
        data class Preview(override val binding: ItemTweaksWidgetReplacementPreviewBinding): ViewHolder(binding)
        data class ProviderPicker(override val binding: ItemSettingsTextItemBinding): ViewHolder(binding)
        data class ProviderReconfigure(override val binding: ItemSettingsTextItemBinding): ViewHolder(binding)
        data class Info(override val binding: ItemTweaksWidgetReplacementInfoBinding): ViewHolder(binding)
        data class Incompatible(override val binding: ItemTweaksWidgetReplacementIncompatibleBinding): ViewHolder(binding)
    }

}