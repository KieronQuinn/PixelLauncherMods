package com.kieronquinn.app.pixellaunchermods.utils.glide

import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.signature.ObjectKey
import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.model.editor.AppEditorInfoOptions
import com.kieronquinn.app.pixellaunchermods.model.icon.ApplicationIcon
import com.kieronquinn.app.pixellaunchermods.model.icon.LegacyThemedIcon
import com.kieronquinn.app.pixellaunchermods.model.icon.RawBytesIcon
import com.kieronquinn.app.pixellaunchermods.model.remote.RemoteApp
import com.kieronquinn.app.pixellaunchermods.model.remote.RemoteAppOptions
import com.kieronquinn.app.pixellaunchermods.model.remote.RemoteFavourite
import com.kieronquinn.app.pixellaunchermods.model.tweaks.WidgetReplacementOptions
import com.kieronquinn.app.pixellaunchermods.repositories.IconLoaderRepository
import com.kieronquinn.app.pixellaunchermods.repositories.IconLoaderRepositoryImpl.Companion.THEMED_ICON_INSET_FRACTION
import com.kieronquinn.app.pixellaunchermods.repositories.IconPackRepository
import com.kieronquinn.app.pixellaunchermods.repositories.IconPackRepository.IconPackIconOptions
import com.kieronquinn.app.pixellaunchermods.repositories.RemoteAppsRepository
import com.kieronquinn.app.pixellaunchermods.ui.screens.iconpicker.BasePickerViewModel.IconPickerResultOptions
import com.kieronquinn.app.pixellaunchermods.utils.extensions.Bitmap_decodeRawBitmap
import com.kieronquinn.app.pixellaunchermods.utils.extensions.renderToBitmap
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*

@GlideModule
class GlideModule: AppGlideModule(), KoinComponent {

    private val remoteAppsRepository by inject<RemoteAppsRepository>()

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        val iconLoader by inject<IconLoaderRepository>()
        val iconPackRepository by inject<IconPackRepository>()
        registry.prepend(
            context,
            RemoteAppOptions::class.java,
            Drawable::class.java,
            iconLoader,
            this::loadRemoteAppOptions,
            this::getKeyForRemoteAppOptions
        )
        registry.prepend(
            context,
            AppEditorInfoOptions::class.java,
            Drawable::class.java,
            iconLoader,
            this::loadAppEditorInfoOptions,
            this::getKeyForAppEditorInfoOptions
        )
        registry.prepend(
            context,
            IconPickerResultOptions::class.java,
            Drawable::class.java,
            iconLoader,
            this::loadIconPickerResultOptions,
            this::getKeyForIconPickerResultOptions
        )
        registry.prepend(
            context,
            IconPackIconOptions::class.java,
            Drawable::class.java,
            Pair(iconLoader, iconPackRepository),
            this::loadIconPackIconOptions,
            this::getKeyForIconPackIconOptions
        )
        registry.prepend(
            context,
            ApplicationIcon::class.java,
            Drawable::class.java,
            iconLoader,
            this::loadApplicationIcon,
            this::getKeyForApplicationIcon
        )
        registry.prepend(
            context,
            LegacyThemedIcon::class.java,
            Drawable::class.java,
            iconLoader,
            this::loadLegacyThemedIcon,
            this::getKeyForLegacyThemedIcon
        )
        registry.prepend(
            context,
            RemoteFavourite::class.java,
            Drawable::class.java,
            iconLoader,
            this::loadRemoteFavourite,
            this::getKeyForRemoteFavourite
        )
        registry.prepend(
            context,
            LauncherActivityInfo::class.java,
            Drawable::class.java,
            Unit,
            this::loadLauncherActivityInfo,
            this::getKeyForLauncherActivityInfo
        )
        registry.prepend(
            context,
            AppWidgetProviderInfo::class.java,
            Drawable::class.java,
            Unit,
            this::loadAppWidgetProviderInfo,
            this::getKeyForAppWidgetProviderInfo
        )
        registry.prepend(
            context,
            WidgetReplacementOptions::class.java,
            Drawable::class.java,
            Unit,
            this::loadWidgetReplacementOptions,
            this::getKeyForWidgetReplacementOptions
        )
        registry.prepend(
            context,
            RawBytesIcon::class.java,
            Drawable::class.java,
            Unit,
            this::loadRawBytesIcon,
            this::getKeyForRawBytesIcon
        )
    }

    private fun areNativeMonoIconsSupported(): Boolean {
        return remoteAppsRepository.areNativeThemedIconsSupported.value
            ?: throw NullPointerException("Attempting to access areNativeThemedIconsSupported before it is set")
    }

    private fun getKeyForRemoteAppOptions(options: RemoteAppOptions): ObjectKey {
        return if(options.remoteApp.type != RemoteApp.Type.NORMAL){
            ObjectKey(UUID.randomUUID().toString()) //Don't cache dynamic icons
        }else{
            ObjectKey(options)
        }
    }

    private fun loadRemoteAppOptions(
        context: Context,
        remoteAppOptions: RemoteAppOptions,
        iconLoader: IconLoaderRepository,
        callback: DataFetcher.DataCallback<in Drawable>
    ) {
        val nativeMonoIcons = areNativeMonoIconsSupported()
        try {
            iconLoader.loadRemoteAppIconOptions(context, remoteAppOptions, nativeMonoIcons)?.let {
                callback.onDataReady(it)
            } ?: run {
                throw NullPointerException("Loading remote app icon returned null")
            }
        }catch (e: Exception){
            callback.onLoadFailed(e)
        }
    }

    private fun getKeyForAppEditorInfoOptions(options: AppEditorInfoOptions): ObjectKey {
        return ObjectKey(UUID.randomUUID().toString()) //Never cache this
    }

    private fun loadAppEditorInfoOptions(
        context: Context,
        appEditorInfoOptions: AppEditorInfoOptions,
        iconLoader: IconLoaderRepository,
        callback: DataFetcher.DataCallback<in Drawable>
    ) {
        val nativeMonoIcons = areNativeMonoIconsSupported()
        val remoteAppOptions = RemoteAppOptions(
            appEditorInfoOptions.appEditorInfo.toRemoteApp(nativeMonoIcons),
            appEditorInfoOptions.loadThemedIcon
        )
        try {
            iconLoader.loadRemoteAppIconOptions(context, remoteAppOptions, nativeMonoIcons)?.let {
                callback.onDataReady(it)
            } ?: run {
                throw NullPointerException("Loading remote app icon returned null")
            }
        }catch (e: Exception){
            callback.onLoadFailed(e)
        }
    }

    private fun getKeyForIconPickerResultOptions(options: IconPickerResultOptions): ObjectKey {
        return ObjectKey(UUID.randomUUID().toString()) //Never cache this
    }

    private fun loadIconPickerResultOptions(
        context: Context,
        options: IconPickerResultOptions,
        iconLoader: IconLoaderRepository,
        callback: DataFetcher.DataCallback<in Drawable>
    ) {
        iconLoader.loadIconPickerResult(context, options)?.let {
            callback.onDataReady(it)
        } ?: run {
            callback.onLoadFailed(GlideLoadException())
        }
    }

    private fun getKeyForIconPackIconOptions(icon: IconPackIconOptions): ObjectKey {
        val rawIcon = icon.iconPackIcon
        val mono = icon.mono
        return ObjectKey("${rawIcon.iconPackPackageName}:${rawIcon.resourceName}:$mono")
    }

    private fun loadIconPackIconOptions(
        context: Context,
        icon: IconPackIconOptions,
        repositories: Pair<IconLoaderRepository, IconPackRepository>,
        callback: DataFetcher.DataCallback<in Drawable>
    ) {
        try {
            val drawable = repositories.second.getIcon(icon.iconPackIcon)?.run {
                if(icon.mono){
                    repositories.first.convertToMono(context, this, false)
                }else this
            }
            callback.onDataReady(drawable)
        }catch (e: Resources.NotFoundException) {
            e.printStackTrace()
            callback.onLoadFailed(e)
        }
    }

    private fun getKeyForApplicationIcon(icon: ApplicationIcon): ObjectKey {
        return ObjectKey("app:${icon.applicationInfo.packageName}")
    }

    private fun loadApplicationIcon(
        context: Context,
        icon: ApplicationIcon,
        iconLoader: IconLoaderRepository,
        callback: DataFetcher.DataCallback<in Drawable>
    ) {
        try {
            val drawable = iconLoader.loadApplicationInfoIcon(
                context, icon.applicationInfo, icon.shrinkNonAdaptiveIcons, icon.mono
            ).run {
                if(icon.mono){
                    iconLoader.convertToMono(context, this, false)
                }else this
            }
            callback.onDataReady(drawable)
        }catch (e: Resources.NotFoundException) {
            e.printStackTrace()
            callback.onLoadFailed(e)
        }
    }

    private fun getKeyForLegacyThemedIcon(icon: LegacyThemedIcon): ObjectKey {
        return ObjectKey("legacythemed:${icon.resourceId}")
    }

    private fun loadLegacyThemedIcon(
        context: Context,
        icon: LegacyThemedIcon,
        iconLoader: IconLoaderRepository,
        callback: DataFetcher.DataCallback<in Drawable>
    ) {
        try {
            val drawable = iconLoader.loadLegacyThemedIcon(context, icon, THEMED_ICON_INSET_FRACTION)
            callback.onDataReady(drawable)
        }catch (e: Resources.NotFoundException) {
            e.printStackTrace()
            callback.onLoadFailed(e)
        }
    }

    private fun getKeyForRemoteFavourite(icon: RemoteFavourite): ObjectKey {
        return ObjectKey("remotefavourite:${icon.intent}")
    }

    private fun loadRemoteFavourite(
        context: Context,
        icon: RemoteFavourite,
        iconLoader: IconLoaderRepository,
        callback: DataFetcher.DataCallback<in Drawable>
    ) {
        iconLoader.loadRemoteFavourite(context, icon)?.let {
            callback.onDataReady(it)
        } ?: run {
            callback.onLoadFailed(NullPointerException())
        }
    }

    private fun getKeyForLauncherActivityInfo(info: LauncherActivityInfo): ObjectKey {
        return ObjectKey("launcherapp:${info.componentName.flattenToShortString()}")
    }

    private fun loadLauncherActivityInfo(
        context: Context,
        info: LauncherActivityInfo,
        dummy: Unit,
        callback: DataFetcher.DataCallback<in Drawable>
    ) {
        callback.onDataReady(info.getIcon(context.resources.configuration.densityDpi))
    }

    private fun getKeyForAppWidgetProviderInfo(info: AppWidgetProviderInfo): ObjectKey {
        return ObjectKey("appwidgetproviderinfo:${info.provider.flattenToShortString()}")
    }

    private fun loadAppWidgetProviderInfo(
        context: Context,
        info: AppWidgetProviderInfo,
        dummy: Unit,
        callback: DataFetcher.DataCallback<in Drawable>
    ) {
        if(info.previewImage != 0){
            callback.onDataReady(info.loadPreviewImage(context, context.resources.configuration.densityDpi))
        }else{
            callback.onDataReady(ContextCompat.getDrawable(context, R.drawable.ic_tweaks_widget_preview_generic))
        }
    }

    private fun getKeyForWidgetReplacementOptions(options: WidgetReplacementOptions): ObjectKey {
        return ObjectKey("appwidgetpreview:${options.widgetView.appWidgetInfo.provider.flattenToShortString()}")
    }

    private fun loadWidgetReplacementOptions(
        context: Context,
        options: WidgetReplacementOptions,
        dummy: Unit,
        callback: DataFetcher.DataCallback<in Drawable>
    ) {
        GlobalScope.launch {
            val bitmap = options.widgetView.renderToBitmap(options.widgetReplacement)
            callback.onDataReady(BitmapDrawable(context.resources, bitmap))
        }
    }

    private fun getKeyForRawBytesIcon(icon: RawBytesIcon): ObjectKey {
        return ObjectKey(UUID.randomUUID().toString()) //No cache
    }

    private fun loadRawBytesIcon(
        context: Context,
        icon: RawBytesIcon,
        dummy: Unit,
        callback: DataFetcher.DataCallback<in Drawable>
    ) {
        val bitmap = Bitmap_decodeRawBitmap(icon.bytes, icon.size, icon.size, Bitmap.Config.ALPHA_8)
        callback.onDataReady(BitmapDrawable(context.resources, bitmap))
    }

}

class GlideLoadException: Exception()