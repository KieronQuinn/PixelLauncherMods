package com.kieronquinn.app.pixellaunchermods.repositories

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.graphics.Bitmap
import android.graphics.Bitmap.Config
import android.graphics.BitmapFactory
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.os.Bundle
import android.text.TextUtils
import android.util.ArrayMap
import android.util.Log
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.Glide
import com.kieronquinn.app.pixellaunchermods.LAWNICONS_PACKAGE_NAME
import com.kieronquinn.app.pixellaunchermods.PIXEL_LAUNCHER_PACKAGE_NAME
import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.model.icon.ApplicationIcon
import com.kieronquinn.app.pixellaunchermods.model.icon.IconPickerResult
import com.kieronquinn.app.pixellaunchermods.model.icon.IconPickerResult.IconPackIcon
import com.kieronquinn.app.pixellaunchermods.model.icon.IconPickerResult.PackageIcon
import com.kieronquinn.app.pixellaunchermods.model.icon.LegacyThemedIcon
import com.kieronquinn.app.pixellaunchermods.model.icon.RawBytesIcon
import com.kieronquinn.app.pixellaunchermods.model.remote.RemoteApp
import com.kieronquinn.app.pixellaunchermods.model.remote.RemoteAppOptions
import com.kieronquinn.app.pixellaunchermods.model.remote.RemoteFavourite
import com.kieronquinn.app.pixellaunchermods.ui.drawables.ClockDrawableWrapper
import com.kieronquinn.app.pixellaunchermods.ui.screens.iconpicker.BasePickerViewModel.IconPickerResultOptions
import com.kieronquinn.app.pixellaunchermods.utils.drawable.IconNormalizer
import com.kieronquinn.app.pixellaunchermods.utils.extensions.AdaptiveIconDrawable_getInsetFraction
import com.kieronquinn.app.pixellaunchermods.utils.extensions.Bitmap_decodeRawBitmap
import com.kieronquinn.app.pixellaunchermods.utils.extensions.Bitmap_getSize
import com.kieronquinn.app.pixellaunchermods.utils.extensions.compress
import com.kieronquinn.app.pixellaunchermods.utils.extensions.compressRaw
import com.kieronquinn.app.pixellaunchermods.utils.extensions.createLawniconsResources
import com.kieronquinn.app.pixellaunchermods.utils.extensions.createPixelLauncherResources
import com.kieronquinn.app.pixellaunchermods.utils.extensions.extractLegacyNormalIcon
import com.kieronquinn.app.pixellaunchermods.utils.extensions.getMonochromeOrForeground
import com.kieronquinn.app.pixellaunchermods.utils.extensions.getPlateColours
import com.kieronquinn.app.pixellaunchermods.utils.extensions.isPackageInstalled
import com.kieronquinn.app.pixellaunchermods.utils.extensions.parseToComponentName
import com.kieronquinn.app.pixellaunchermods.utils.extensions.setShrinkNonAdaptiveIcons
import com.kieronquinn.app.pixellaunchermods.utils.extensions.updateLegacyIcon
import com.kieronquinn.app.pixellaunchermods.utils.glide.ScaleTransformation
import me.zhanghai.android.appiconloader.AppIconLoader
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.util.Calendar

interface IconLoaderRepository {

    val iconSize: Int

    fun loadRemoteAppIconOptions(context: Context, remoteAppOptions: RemoteAppOptions, nativeMonoIcons: Boolean): Drawable?
    fun loadApplicationInfoIcon(context: Context, applicationInfo: ApplicationInfo, shrinkAdaptiveIcons: Boolean, mono: Boolean): Drawable
    fun getAllLegacyThemedIcons(): Map<String, LegacyThemedIcon>
    fun getLegacyThemedIconForPackage(packageName: String): LegacyThemedIcon?
    fun getLegacyThemedIconName(legacyThemedIcon: LegacyThemedIcon): String
    fun getAllLawnicons(): Map<String, LegacyThemedIcon>
    fun getLawniconForPackage(packageName: String): LegacyThemedIcon?
    fun getLawniconName(lawnicon: LegacyThemedIcon): String
    fun loadLegacyThemedIcon(context: Context, legacyThemedIcon: LegacyThemedIcon, insetFraction: Float): Drawable?
    fun rasterIconPickerResultOptions(options: IconPickerResultOptions): Bitmap?
    fun loadIconPickerResult(context: Context, options: IconPickerResultOptions): Drawable?
    fun createLegacyThemedIcon(resourceId: Int): LegacyThemedIcon
    fun createLawnicon(resourceId: Int): LegacyThemedIcon?
    fun createLegacyThemedIcon(resourceName: String): LegacyThemedIcon?
    fun createLawnicon(resourceName: String): LegacyThemedIcon?
    fun getPixelLauncherApplicationIcon(): ApplicationIcon
    fun getLawniconsApplicationIcon(): ApplicationIcon
    fun convertToMono(context: Context, drawable: Drawable, addInset: Boolean): Drawable
    fun resizeIcon(icon: ByteArray, size: Int): ByteArray?
    fun resizeThemedIcon(icon: ByteArray, currentSize: Int, size: Int): ByteArray?
    fun resizeLegacyIcon(icon: ByteArray, size: Int): ByteArray?
    fun loadRemoteFavourite(context: Context, remoteFavourite: RemoteFavourite): Drawable?

}

class IconLoaderRepositoryImpl(
    private val context: Context,
    private val iconPackRepository: IconPackRepository,
    private val overrideIconSize: Int? = null,
): IconLoaderRepository, KoinComponent {

    companion object {
        const val LEGACY_ICON_TYPE_NORMAL = 1
        const val LEGACY_ICON_TYPE_THEMED = 2

        private const val TAG_ICON = "icon"
        private const val ATTR_PACKAGE = "package"
        private const val ATTR_DRAWABLE = "drawable"
        private const val THEMED_ICON_MAP_FILE = "grayscale_icon_map"
        const val THEMED_ICON_INSET_FRACTION = 0.325f
        const val THEMED_ICON_INSET_FRACTION_LARGE = 0.225f
        const val THEMED_ICON_INSET_FRACTION_EXTRA_LARGE = 0.175f
    }

    private val remoteAppsRepository by inject<RemoteAppsRepository>()
    private val fullResIconSize = context.resources.getDimension(R.dimen.app_icon_size).toInt()
    private val appIconLoader = AppIconLoader(fullResIconSize, true, context)
    private val pixelLauncherResources = context.createPixelLauncherResources()
    private val packageManager = context.packageManager
    private var lawniconsIconMap: Map<String, LegacyThemedIcon>? = null

    private val iconNormalizer by lazy {
        IconNormalizer(context, iconSize, true)
    }

    override val iconSize
        get() = overrideIconSize ?: remoteAppsRepository.iconSize.value ?: throw NullPointerException("Cannot access iconSize before it is set")

    private val glide by lazy {
        Glide.with(context)
    }

    private val pixelLauncherThemedIconMap by lazy {
        getThemedIconMap(pixelLauncherResources, PIXEL_LAUNCHER_PACKAGE_NAME)
    }

    private val calendarComponentName by lazy {
        pixelLauncherResources.getIdentifier("calendar_component_name", "string", PIXEL_LAUNCHER_PACKAGE_NAME).run {
            if(this == 0) null
            else pixelLauncherResources.getString(this)
        }
    }

    private val calendarPackageName by lazy {
        calendarComponentName?.parseToComponentName()?.packageName
    }

    private val clockComponentName by lazy {
        pixelLauncherResources.getIdentifier("clock_component_name", "string", PIXEL_LAUNCHER_PACKAGE_NAME).run {
            if(this == 0) null
            else pixelLauncherResources.getString(this)
        }
    }

    private val clockPackageName by lazy {
        clockComponentName?.parseToComponentName()?.packageName
    }

    private fun getLawniconResources(): Resources? {
        return if(context.isPackageInstalled(LAWNICONS_PACKAGE_NAME)){
            context.createLawniconsResources()
        }else null
    }

    override fun loadRemoteAppIconOptions(context: Context, remoteAppOptions: RemoteAppOptions, nativeMonoIcons: Boolean): Drawable? {
        return when(remoteAppOptions.remoteApp.type) {
            RemoteApp.Type.NORMAL, RemoteApp.Type.APP_SHORTCUT -> {
                if(nativeMonoIcons){
                    remoteAppOptions.remoteApp.loadIcon(context, remoteAppOptions.mono)
                }else{
                    remoteAppOptions.remoteApp.loadLegacyIcon(context, remoteAppOptions.mono)
                }
            }
            RemoteApp.Type.CLOCK -> {
                loadClockIcon(context, remoteAppOptions.remoteApp.componentName, remoteAppOptions.mono)
            }
            RemoteApp.Type.CALENDAR -> {
                loadCalendarDrawable(context, remoteAppOptions.remoteApp.componentName, remoteAppOptions.mono)
            }
        }
    }

    override fun loadLegacyThemedIcon(context: Context, legacyThemedIcon: LegacyThemedIcon, insetFraction: Float): Drawable? {
        return ResourcesCompat.getDrawable(
            legacyThemedIcon.resources,
            legacyThemedIcon.resourceId,
            null
        )?.run {
            val resized = BitmapDrawable(context.resources, toBitmap(iconSize, iconSize))
            resized.serveOnPlate(context, insetFraction)
        }
    }

    private fun RemoteApp.loadIcon(context: Context, loadThemedIcon: Boolean): Drawable? {
        return if(loadThemedIcon && monoIcon != null && icon != null){
            val iconSize = Bitmap_getSize(icon)
            Bitmap_decodeRawBitmap(monoIcon, iconSize.width(), iconSize.height(), Config.ALPHA_8).run {
                BitmapDrawable(context.resources, this).serveOnPlate(context)
            }
        } else if(icon != null) {
            BitmapFactory.decodeByteArray(icon, 0, icon.size).run {
                BitmapDrawable(context.resources, this)
            }
        } else null
    }

    private fun Drawable.serveOnPlate(context: Context, customInset: Float? = null): AdaptiveIconDrawable {
        val colors: IntArray = context.getPlateColours()
        val bg: Drawable = ColorDrawable(colors[0])
        val inset = customInset ?: AdaptiveIconDrawable_getInsetFraction()
        val tintedDrawable = mutate().apply {
            setTint(colors[1])
        }
        val fg: Drawable = InsetDrawable(tintedDrawable, inset)
        return AdaptiveIconDrawable(bg, fg)
    }

    private fun RemoteApp.loadLegacyIcon(context: Context, loadThemedIcon: Boolean): Drawable? {
        if(icon == null || icon.isEmpty()) return null
        val icons = icon.loadLegacyIcons(context)
        val themedIcon = icons.second
        val normalIcon = icons.first
        return if(loadThemedIcon && themedIcon != null){
            themedIcon.loadDrawable()?.serveOnPlate(context, THEMED_ICON_INSET_FRACTION)
        }else{
            BitmapFactory.decodeByteArray(normalIcon, 0, normalIcon.size).run {
                BitmapDrawable(context.resources, this)
            }
        }
    }

    private fun ByteArray.loadLegacyIcons(context: Context): Pair<ByteArray, LegacyThemedIcon?> {
        return when(this[0].toInt()){
            LEGACY_ICON_TYPE_NORMAL -> {
               Pair(loadLegacyNormalIcon(), null)
            }
            LEGACY_ICON_TYPE_THEMED -> {
                LegacyThemedIcon.parseLegacyByteArray(context, this)
            }
            else -> throw RuntimeException("Unknown icon type ${this[0].toInt()}")
        }
    }

    private fun ByteArray.loadLegacyNormalIcon(): ByteArray {
        val input = ByteArrayInputStream(this)
        return DataInputStream(input).use {
            it.readByte() //Skip type
            it.readBytes() //Read rest of bytes for bitmap
        }.also {
            input.close()
        }
    }

    override fun loadApplicationInfoIcon(context: Context, applicationInfo: ApplicationInfo, shrinkAdaptiveIcons: Boolean, mono: Boolean): Drawable {
        //Mono overrides shrinkAdaptiveIcons as they are not compatible with each other
        return if(mono){
            context.packageManager.getApplicationIcon(applicationInfo)
        }else{
            appIconLoader.setShrinkNonAdaptiveIcons(shrinkAdaptiveIcons)
            BitmapDrawable(context.resources, appIconLoader.loadIcon(applicationInfo))
        }
    }

    override fun getAllLegacyThemedIcons(): Map<String, LegacyThemedIcon> {
        return pixelLauncherThemedIconMap
    }

    override fun getLegacyThemedIconForPackage(packageName: String): LegacyThemedIcon? {
        return pixelLauncherThemedIconMap[packageName]
    }

    override fun getLegacyThemedIconName(legacyThemedIcon: LegacyThemedIcon): String {
        return pixelLauncherResources.getResourceName(legacyThemedIcon.resourceId)
    }

    private fun getThemedIconMap(resources: Resources, packageName: String): Map<String, LegacyThemedIcon> {
        val map: ArrayMap<String, LegacyThemedIcon> = ArrayMap()
        try {
            val resID = resources.getIdentifier(THEMED_ICON_MAP_FILE, "xml", packageName)

            if (resID != 0) {
                val parser: XmlResourceParser = resources.getXml(resID)
                val depth = parser.depth
                var type: Int
                while (parser.next().also { type = it } != XmlPullParser.START_TAG
                    && type != XmlPullParser.END_DOCUMENT)
                while ((parser.next().also { type = it } != XmlPullParser.END_TAG ||
                            parser.depth > depth) && type != XmlPullParser.END_DOCUMENT) {
                    if (type != XmlPullParser.START_TAG) {
                        continue
                    }
                    if (TAG_ICON == parser.name) {
                        val pkg = parser.getAttributeValue(null, ATTR_PACKAGE)
                        val iconId = parser.getAttributeResourceValue(null, ATTR_DRAWABLE, 0)
                        if (iconId != 0 && !TextUtils.isEmpty(pkg)) {
                            val iconName = resources.getResourceEntryName(iconId)
                            val iconType = when(resources.getResourceTypeName(iconId)) {
                                "drawable" -> LegacyThemedIcon.Type.DRAWABLE
                                "array" -> {
                                    when(pkg){
                                        clockPackageName -> LegacyThemedIcon.Type.CLOCK
                                        calendarPackageName -> LegacyThemedIcon.Type.CALENDAR
                                        else -> LegacyThemedIcon.Type.DRAWABLE
                                    }
                                }
                                else -> LegacyThemedIcon.Type.DRAWABLE
                            }
                            map[pkg] = LegacyThemedIcon(
                                iconType, 1f, iconId, resources, iconName
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("IconLoader", "Unable to parse icon map", e)
        }
        return map
    }

    override fun getAllLawnicons(): Map<String, LegacyThemedIcon> {
        return getThemedIconMap(
            getLawniconResources() ?: return emptyMap(),
            LAWNICONS_PACKAGE_NAME
        ).also {
            lawniconsIconMap = it
        }
    }

    override fun getLawniconForPackage(packageName: String): LegacyThemedIcon? {
        return (lawniconsIconMap ?: getAllLawnicons())[packageName]
    }

    override fun getLawniconName(lawnicon: LegacyThemedIcon): String {
        return getLawniconResources()?.getResourceName(lawnicon.resourceId)!!
    }

    override fun rasterIconPickerResultOptions(options: IconPickerResultOptions): Bitmap? {
        val size = if(options.loadFullRes) fullResIconSize else iconSize
        return try {
            glide.asDrawable().load(options).submit(size, size)
                .get().toBitmap(size, size, if(options.mono) Config.ALPHA_8 else null)
        }catch (e: Exception){
            null
        }
    }

    override fun loadIconPickerResult(
        context: Context,
        options: IconPickerResultOptions
    ): Drawable? {
        val result = options.result
        var scale: Float? = null
        return when (result) {
            is IconPackIcon -> {
                if(!context.isPackageInstalled(result.iconPackPackageName)) return null
                iconPackRepository.getIcon(result.iconPackPackageName, result.iconResource)
            }
            is PackageIcon -> {
                loadApplicationInfoIcon(
                    context,
                    result.applicationIcon.applicationInfo,
                    result.applicationIcon.shrinkNonAdaptiveIcons,
                    options.mono
                )
            }
            is IconPickerResult.LegacyThemedIcon -> {
                val fraction = if(options.mono){
                    THEMED_ICON_INSET_FRACTION_LARGE
                }else{
                    THEMED_ICON_INSET_FRACTION
                }
                loadLegacyThemedIcon(context, createLegacyThemedIcon(result.resourceId), fraction)
            }
            is IconPickerResult.UriIcon -> {
                glide.load(result.uri).submit(iconSize, iconSize).get()
            }
            is IconPickerResult.Lawnicon -> {
                val lawnicon = createLawnicon(result.resourceId) ?: return null
                loadLegacyThemedIcon(context, lawnicon, THEMED_ICON_INSET_FRACTION_LARGE)
            }
            is IconPickerResult.BitmapIcon -> {
                if(options.mono){
                    glide.load(RawBytesIcon(result.bitmapBytes, fullResIconSize))
                        .submit(iconSize, iconSize).get()
                }else{
                    glide.load(result.bitmapBytes).submit(iconSize, iconSize).get()
                }
            }
        }.run {
            if(!options.result.isLegacyThemedIcon() && options.mono && this is AdaptiveIconDrawable){
                val drawable = this.getMonochromeOrForeground()
                scale = 1f + iconNormalizer.getScale(drawable, null, null, null)
                drawable
            }else this
        }.run {
            if(options.result.isLegacyThemedIcon() && options.mono && this is AdaptiveIconDrawable) {
                getMonochromeOrForeground()
            }else this
        }.run {
            glide.load(this).run {
                scale?.let {
                    transform(ScaleTransformation(it))
                } ?: this
            }.submit(iconSize, iconSize).get()
        }?.run {
            if(!options.result.isLegacyThemedIcon() && options.mono){
                InsetDrawable(this, THEMED_ICON_INSET_FRACTION_EXTRA_LARGE)
            }else this
        }
    }

    private fun IconPickerResult.isLegacyThemedIcon(): Boolean {
        return this is IconPickerResult.LegacyThemedIcon || this is IconPickerResult.Lawnicon
    }

    override fun createLegacyThemedIcon(resourceId: Int): LegacyThemedIcon {
        val resourceName = pixelLauncherResources.getResourceEntryName(resourceId)
        return LegacyThemedIcon(
            LegacyThemedIcon.Type.DRAWABLE, 1f, resourceId, pixelLauncherResources, resourceName
        )
    }

    override fun createLawnicon(resourceId: Int): LegacyThemedIcon? {
        val resources = getLawniconResources() ?: return null
        val resourceName = resources.getResourceEntryName(resourceId)
        return LegacyThemedIcon(
            LegacyThemedIcon.Type.DRAWABLE, 1f, resourceId, resources, resourceName
        )
    }

    override fun createLegacyThemedIcon(resourceName: String): LegacyThemedIcon? {
        val identifier = pixelLauncherResources.getIdentifier(resourceName, "drawable", PIXEL_LAUNCHER_PACKAGE_NAME)
        if(identifier == 0) return null
        return createLegacyThemedIcon(identifier)
    }

    override fun createLawnicon(resourceName: String): LegacyThemedIcon? {
        val identifier = getLawniconResources()?.getIdentifier(resourceName, "drawable", LAWNICONS_PACKAGE_NAME)
            ?: return null
        if(identifier == 0) return null
        return createLawnicon(identifier)
    }

    override fun getPixelLauncherApplicationIcon(): ApplicationIcon {
        return packageManager.getApplicationInfo(PIXEL_LAUNCHER_PACKAGE_NAME, 0).run {
            ApplicationIcon(this, false)
        }
    }

    override fun getLawniconsApplicationIcon(): ApplicationIcon {
        return packageManager.getApplicationInfo(LAWNICONS_PACKAGE_NAME, 0).run {
            ApplicationIcon(this, false)
        }
    }

    override fun convertToMono(context: Context, drawable: Drawable, addInset: Boolean): Drawable {
        val bestForeground = if(drawable is AdaptiveIconDrawable){
            drawable.getMonochromeOrForeground()
        }else drawable
        return bestForeground.serveOnPlate(context, if(addInset) null else 0.1f)
    }

    override fun resizeIcon(icon: ByteArray, size: Int): ByteArray? {
        return try {
            val bitmap = BitmapFactory.decodeByteArray(icon, 0, icon.size)
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, size, size, true)
            scaledBitmap.compress().also {
                bitmap.recycle()
                scaledBitmap.recycle()
            }
        }catch (e: Exception){
            null
        }
    }

    override fun resizeThemedIcon(icon: ByteArray, currentSize: Int, size: Int): ByteArray? {
        return try {
            val bitmap = Bitmap_decodeRawBitmap(icon, currentSize, currentSize, Config.ALPHA_8)
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, size, size, true)
            scaledBitmap.compressRaw().also {
                bitmap.recycle()
                scaledBitmap.recycle()
            }
        }catch (e: Exception) {
            null
        }
    }

    override fun resizeLegacyIcon(icon: ByteArray, size: Int): ByteArray? {
        return try {
            val iconBytes = icon.extractLegacyNormalIcon()
            val bitmap = BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.size)
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, size, size, true)
            return icon.updateLegacyIcon(icon = scaledBitmap.compress())?.also {
                bitmap.recycle()
                scaledBitmap.recycle()
            }
        }catch (e: Exception){
            null
        }
    }

    private fun loadClockIcon(context: Context, component: String, mono: Boolean): ClockDrawableWrapper {
        val packageName = component.parseToComponentName()?.packageName
        val themedIcon = if(packageName != null && mono) {
            getLegacyThemedIconForPackage(packageName)
        }else null
        return if(themedIcon != null){
            ClockDrawableWrapper.fromThemeData(context, themedIcon)
        }else{
            ClockDrawableWrapper.forPackage(context, packageName, context.resources.configuration.densityDpi)
        }
    }

    private fun loadCalendarDrawable(context: Context, component: String, mono: Boolean): Drawable? {
        val componentName = component.parseToComponentName() ?: return null
        val packageName = componentName.packageName
        if(mono) {
            loadThemedCalendarDrawable(pixelLauncherResources, packageName)?.let {
                return it.serveOnPlate(context, THEMED_ICON_INSET_FRACTION)
            }
        }
        val resources = packageManager.getResourcesForApplication(packageName)
        val packageManager = context.packageManager
        val iconDpi = context.resources.configuration.densityDpi
        return try {
            val metadata =
                packageManager.getActivityInfo(componentName, PackageManager.GET_META_DATA).metaData
            val id: Int = getDynamicIconId(packageName, metadata, resources)
            if (id != 0) {
                resources.getDrawableForDensity(id, iconDpi, null)
            }else null
        }catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    private fun loadThemedCalendarDrawable(resources: Resources, packageName: String): Drawable? {
        val themedIcon = getLegacyThemedIconForPackage(packageName) ?: return null
        val typedArray = resources.obtainTypedArray(themedIcon.resourceId)
        val id = typedArray.getResourceId(getDay(), 0)
        typedArray.recycle()
        return if(id != 0) ResourcesCompat.getDrawable(resources, id, null)
        else null
    }

    /**
     * @param metadata metadata of the default activity of Calendar
     * @param resources from the Calendar package
     * @return the resource id for today's Calendar icon; 0 if resources cannot be found.
     */
    private fun getDynamicIconId(packageName: String, metadata: Bundle?, resources: Resources): Int {
        if (metadata == null) {
            return 0
        }
        val key = "$packageName.dynamic_icons"
        val arrayId = metadata.getInt(key, 0)
        return if (arrayId == 0) {
            0
        } else try {
            val typedArray = resources.obtainTypedArray(arrayId)
            val id = typedArray.getResourceId(getDay(), 0)
            typedArray.recycle()
            id
        } catch (e: Resources.NotFoundException) {
            0
        }
    }

    /**
     * @return Today's day of the month, zero-indexed.
     */
    private fun getDay(): Int {
        return Calendar.getInstance().get(Calendar.DAY_OF_MONTH) - 1
    }

    override fun loadRemoteFavourite(context: Context, remoteFavourite: RemoteFavourite): Drawable? {
        return remoteFavourite.icon?.let {
            BitmapFactory.decodeByteArray(it, 0, it.size)
        }?.run {
            BitmapDrawable(context.resources, this)
        }
    }

}