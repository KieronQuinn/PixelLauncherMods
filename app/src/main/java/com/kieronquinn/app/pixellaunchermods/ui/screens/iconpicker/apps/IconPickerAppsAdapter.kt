package com.kieronquinn.app.pixellaunchermods.ui.screens.iconpicker.apps

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.RelativeSizeSpan
import android.text.style.TypefaceSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.databinding.ItemIconAppBinding
import com.kieronquinn.app.pixellaunchermods.databinding.ItemIconAppHeaderBinding
import com.kieronquinn.app.pixellaunchermods.model.icon.ApplicationIcon
import com.kieronquinn.app.pixellaunchermods.ui.screens.iconpicker.apps.IconPickerAppsViewModel.Item

class IconPickerAppsAdapter(
    context: Context,
    var items: List<Item>,
    var mono: Boolean,
    private val onShrinkNoneAdaptiveIconsChanged: (Boolean) -> Unit,
    private val onAppClicked: (ApplicationIcon) -> Unit
): RecyclerView.Adapter<IconPickerAppsAdapter.ViewHolder>() {

    init {
        setHasStableIds(true)
    }

    private val layoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    private val glide = Glide.with(context)
    private val googleSans = ResourcesCompat.getFont(context, R.font.google_sans_text)
    private val googleSansMedium = ResourcesCompat.getFont(context, R.font.google_sans_text_medium)

    override fun getItemCount() = items.size

    override fun getItemId(position: Int): Long {
        return when (val item = items[position]) {
            is Item.Header -> {
                -1L
            }
            is Item.App -> {
                item.app.label.hashCode().toLong()
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].type.ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when(Item.Type.values()[viewType]){
            Item.Type.HEADER -> ViewHolder.Header(ItemIconAppHeaderBinding.inflate(layoutInflater, parent, false))
            Item.Type.APP -> ViewHolder.App(ItemIconAppBinding.inflate(layoutInflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        when(holder){
            is ViewHolder.Header -> {
                holder.binding.setupHeader(item as Item.Header)
            }
            is ViewHolder.App -> {
                holder.binding.setupApp(item as Item.App)
            }
        }
    }

    private fun ItemIconAppBinding.setupApp(item: Item.App) {
        val app = item.app
        itemIconAppName.text = app.label
        val applicationInfo = ApplicationIcon(app.applicationInfo, item.shrinkNonAdaptiveIcons, mono)
        glide.load(applicationInfo)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(false)
            .into(itemIconAppIcon)
        root.setOnClickListener { onAppClicked(applicationInfo) }
    }

    private fun ItemIconAppHeaderBinding.setupHeader(item: Item.Header) = with(itemIconAppHeaderSwitch){
        text = root.context.getSwitchLabel()
        typeface = googleSansMedium
        setOnCheckedChangeListener(null)
        isChecked = item.shrinkNonAdaptiveIcons
        setOnCheckedChangeListener { _, enabled ->
            onShrinkNoneAdaptiveIconsChanged(enabled)
        }
    }

    private fun Context.getSwitchLabel(): CharSequence {
        val sizeSpan = RelativeSizeSpan(0.75f)
        val fontSpan = TypefaceSpan(googleSans ?: Typeface.DEFAULT)
        val secondLine = SpannableString(getString(R.string.app_editor_iconpicker_apps_switch_subtitle)).apply {
            setSpan(sizeSpan, 0, length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            setSpan(fontSpan, 0, length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
        }
        return SpannableStringBuilder().apply {
            appendLine(getString(R.string.app_editor_iconpicker_apps_switch_title))
            append(secondLine)
        }
    }

    sealed class ViewHolder(open val binding: ViewBinding): RecyclerView.ViewHolder(binding.root) {
        data class Header(override val binding: ItemIconAppHeaderBinding): ViewHolder(binding)
        data class App(override val binding: ItemIconAppBinding): ViewHolder(binding)
    }

}