package com.kieronquinn.app.pixellaunchermods.ui.base.settings

import android.content.Context
import android.text.Html
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.kieronquinn.app.pixellaunchermods.BuildConfig
import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.databinding.*
import com.kieronquinn.app.pixellaunchermods.repositories.SettingsRepository
import com.kieronquinn.app.pixellaunchermods.repositories.invert
import com.kieronquinn.app.pixellaunchermods.ui.base.settings.BaseSettingsViewModel.SettingsItem
import com.kieronquinn.app.pixellaunchermods.ui.base.settings.BaseSettingsViewModel.SettingsItem.SettingsItemType
import com.kieronquinn.app.pixellaunchermods.ui.views.LifecycleAwareRecyclerView
import com.kieronquinn.app.pixellaunchermods.utils.extensions.addRipple
import com.kieronquinn.app.pixellaunchermods.utils.extensions.onChanged
import com.kieronquinn.app.pixellaunchermods.utils.extensions.onClicked
import com.kieronquinn.app.pixellaunchermods.utils.extensions.removeRipple
import me.saket.bettermovementmethod.BetterLinkMovementMethod

abstract class BaseSettingsAdapter(
    context: Context,
    recyclerView: RecyclerView,
    private val _items: List<SettingsItem>
) : LifecycleAwareRecyclerView.Adapter<BaseSettingsAdapter.ViewHolder>(recyclerView) {

    init {
        setHasStableIds(true)
    }

    private val googleSansTextMedium by lazy {
        ResourcesCompat.getFont(recyclerView.context, R.font.google_sans_text_medium)
    }

    private fun filterItems(): List<SettingsItem> {
        return _items.filter { it.isVisible() }
    }

    private var items = filterItems()

    private val layoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int): Int {
        return items[position].type.ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (SettingsItemType.fromViewType(viewType)) {
            SettingsItemType.TEXT -> ViewHolder.Text(
                ItemSettingsTextItemBinding.inflate(
                    layoutInflater,
                    parent,
                    false
                )
            )
            SettingsItemType.SWITCH -> ViewHolder.Switch(
                ItemSettingsSwitchItemBinding.inflate(
                    layoutInflater,
                    parent,
                    false
                )
            )
            SettingsItemType.ABOUT -> ViewHolder.About(
                ItemSettingsAboutBinding.inflate(
                    layoutInflater,
                    parent,
                    false
                )
            )
            SettingsItemType.HEADER -> ViewHolder.Header(
                ItemSettingsHeaderBinding.inflate(
                    layoutInflater,
                    parent,
                    false
                )
            )
            SettingsItemType.SLIDER -> ViewHolder.Slider(
                ItemSettingsSliderItemBinding.inflate(
                    layoutInflater,
                    parent,
                    false
                )
            )
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is ViewHolder.Text -> holder.binding.setupTextItem(
                item as SettingsItem.Text,
                holder.lifecycleScope
            )
            is ViewHolder.Switch -> holder.binding.setupSwitchItem(
                item as SettingsItem.Switch,
                holder.lifecycleScope
            )
            is ViewHolder.About -> holder.binding.setupAboutItem(
                item as SettingsItem.About,
                holder.lifecycleScope
            )
            is ViewHolder.Slider -> holder.binding.setupSliderItem(
                item as SettingsItem.Slider,
                holder.lifecycleScope
            )
            is ViewHolder.Header -> holder.binding.setupHeaderItem(
                item as SettingsItem.Header
            )
        }
    }

    override fun getItemId(position: Int): Long {
        return items[position].hashCode().toLong()
    }

    private fun ItemSettingsTextItemBinding.setupTextItem(
        item: SettingsItem.Text,
        scope: LifecycleCoroutineScope
    ) {
        val context = root.context
        itemSettingsTextTitle.text = when {
            item.title != null -> item.title
            item.titleRes != null -> context.getText(item.titleRes)
            else -> null
        }
        if (item.linkClicked != null) {
            itemSettingsTextContent.text = when {
                item.content != null -> Html.fromHtml(
                    item.content.invoke().toString(),
                    Html.FROM_HTML_MODE_LEGACY
                )
                item.contentRes != null -> Html.fromHtml(
                    context.getText(item.contentRes).toString(), Html.FROM_HTML_MODE_LEGACY
                )
                else -> null
            }
            Linkify.addLinks(itemSettingsTextContent, Linkify.ALL)
            itemSettingsTextContent.isVisible = !itemSettingsTextContent.text.isNullOrEmpty()
            itemSettingsTextContent.movementMethod =
                BetterLinkMovementMethod.newInstance().setOnLinkClickListener { _, url ->
                    item.linkClicked?.invoke(url)
                    true
                }
        } else {
            itemSettingsTextContent.movementMethod = null
            itemSettingsTextContent.text = when {
                item.content != null -> item.content.invoke()
                item.contentRes != null -> context.getText(item.contentRes)
                else -> null
            }
            itemSettingsTextContent.isVisible = !itemSettingsTextContent.text.isNullOrEmpty()
        }
        val isEnabled = item.isEnabled()
        if(item.onClick != null && isEnabled){
            root.addRipple()
        }else{
            root.removeRipple()
        }
        itemSettingsTextIcon.setImageResource(item.icon)
        scope.launchWhenResumed {
            item.onClick?.let {
                root.onClicked().collect {
                    if (!isEnabled) return@collect
                    item.onClick.invoke()
                }
            }
        }
    }

    private fun ItemSettingsSwitchItemBinding.setupSwitchItem(
        item: SettingsItem.Switch,
        scope: LifecycleCoroutineScope
    ) {
        val context = root.context
        itemSettingsSwitchTitle.text = when {
            item.title != null -> item.title
            item.titleRes != null -> context.getText(item.titleRes)
            else -> null
        }
        itemSettingsSwitchContent.text = when {
            item.content != null -> item.content.invoke()
            item.contentRes != null -> context.getText(item.contentRes)
            else -> null
        }.also {
            itemSettingsSwitchContent.isVisible = !it.isNullOrEmpty()
        }
        itemSettingsSwitchIcon.setImageResource(item.icon)
        val isEnabled = item.isEnabled()
        itemSettingsSwitchSwitch.isEnabled = isEnabled
        itemSettingsSwitchSwitch.isChecked = item.setting.getSync()
        itemSettingsSwitchSwitch.alpha = if (isEnabled) 1f else 0.5f
        itemSettingsSwitchTitle.alpha = if (isEnabled) 1f else 0.5f
        itemSettingsSwitchContent.alpha = if (isEnabled) 1f else 0.5f
        itemSettingsSwitchIcon.alpha = if (isEnabled) 1f else 0.5f
        scope.launchWhenResumed {
            item.setting.asFlow().collect {
                itemSettingsSwitchSwitch.isChecked = it
            }
        }
        scope.launchWhenResumed {
            root.onClicked().collect {
                if (!isEnabled) return@collect
                item.setting.invert()
            }
        }
        scope.launchWhenResumed {
            itemSettingsSwitchSwitch.onClicked().collect {
                if (!isEnabled) return@collect
                if (item.setting is SettingsRepository.FakePixelLauncherModsSetting) {
                    //Prevent initial change
                    (it as CompoundButton).isChecked = item.setting.get()
                }
                item.setting.invert()
            }
        }
    }

    private fun ItemSettingsSliderItemBinding.setupSliderItem(
        item: SettingsItem.Slider,
        scope: LifecycleCoroutineScope
    ) {
        root.removeRipple()
        val context = root.context
        itemSettingsSliderTitle.text = when {
            item.title != null -> item.title
            item.titleRes != null -> context.getText(item.titleRes)
            else -> null
        }
        itemSettingsSliderContent.text = when {
            item.content != null -> item.content.invoke()
            item.contentRes != null -> context.getText(item.contentRes)
            else -> null
        }.also {
            itemSettingsSliderContent.isVisible = !it.isNullOrEmpty()
        }
        itemSettingsSliderIcon.setImageResource(item.icon)
        val isEnabled = item.isEnabled()
        itemSettingsSliderSlider.isEnabled = isEnabled
        itemSettingsSliderSlider.valueFrom = item.min
        itemSettingsSliderSlider.valueTo = item.max
        itemSettingsSliderSlider.setLabelFormatter(item.labelFormatter)
        itemSettingsSliderSlider.value = item.setting.getSync()
        itemSettingsSliderSlider.alpha = if (isEnabled) 1f else 0.5f
        itemSettingsSliderTitle.alpha = if (isEnabled) 1f else 0.5f
        itemSettingsSliderContent.alpha = if (isEnabled) 1f else 0.5f
        itemSettingsSliderIcon.alpha = if (isEnabled) 1f else 0.5f
        scope.launchWhenResumed {
            item.setting.asFlow().collect {
                itemSettingsSliderSlider.value = it
            }
        }
        scope.launchWhenResumed {
            itemSettingsSliderSlider.onChanged().collect {
                if (!isEnabled) return@collect
                if (item.setting is SettingsRepository.FakePixelLauncherModsSetting) {
                    //Prevent initial change
                    itemSettingsSliderSlider.value = item.setting.get()
                }
                item.setting.set(it)
            }
        }
    }

    private fun ItemSettingsHeaderBinding.setupHeaderItem(item: SettingsItem.Header) {
        itemSettingsHeaderTitle.setText(item.titleRes)
    }

    private fun ItemSettingsAboutBinding.setupAboutItem(
        item: SettingsItem.About,
        scope: LifecycleCoroutineScope
    ) {
        val context = root.context
        val content = context.getString(R.string.about_version, BuildConfig.VERSION_NAME)
        itemSettingsMoreAboutContent.text = content
        mapOf(
            itemSettingsMoreAboutContributors to item.onContributorsClicked,
            itemSettingsMoreAboutDonate to item.onDonateClicked,
            itemSettingsMoreAboutGithub to item.onGitHubClicked,
            itemSettingsMoreAboutLibraries to item.onLibrariesClicked,
            itemSettingsMoreAboutTwitter to item.onTwitterClicked,
            itemSettingsMoreAboutXda to item.onXdaClicked
        ).forEach { chip ->
            with(chip.key){
                typeface = googleSansTextMedium
                scope.launchWhenResumed {
                    onClicked().collect {
                        chip.value()
                    }
                }
            }
        }
    }

    sealed class ViewHolder(open val binding: ViewBinding) : LifecycleAwareRecyclerView.ViewHolder(binding.root) {
        data class Text(override val binding: ItemSettingsTextItemBinding) : ViewHolder(binding)
        data class Switch(override val binding: ItemSettingsSwitchItemBinding) : ViewHolder(binding)
        data class Slider(override val binding: ItemSettingsSliderItemBinding) : ViewHolder(binding)
        data class Header(override val binding: ItemSettingsHeaderBinding) : ViewHolder(binding)
        data class About(override val binding: ItemSettingsAboutBinding) : ViewHolder(binding)
    }

}