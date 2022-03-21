package com.kieronquinn.app.pixellaunchermods.model.room

import com.google.gson.annotations.SerializedName
import com.kieronquinn.app.pixellaunchermods.model.icon.ApplicationIcon
import com.kieronquinn.app.pixellaunchermods.model.icon.IconPickerResult
import com.kieronquinn.app.pixellaunchermods.repositories.AppsRepository
import com.kieronquinn.app.pixellaunchermods.repositories.IconLoaderRepository
import com.kieronquinn.app.pixellaunchermods.utils.gson.RuntimeTypeAdapterFactory

sealed class IconMetadata(
    var typeId: Type,
    open var applyType: ApplyType
) {

    @SerializedName("type")
    protected var type = javaClass.simpleName

    companion object {
        fun getAdapterFactory(): RuntimeTypeAdapterFactory<IconMetadata> {
            return RuntimeTypeAdapterFactory.of(IconMetadata::class.java, "type", true)
                .registerSubtype(Static::class.java)
                .registerSubtype(Package::class.java)
                .registerSubtype(IconPack::class.java)
                .registerSubtype(LegacyThemedIcon::class.java)
                .registerSubtype(Lawnicon::class.java)
        }
    }

    data class Static(
        @SerializedName("apply_type")
        override var applyType: ApplyType
    ): IconMetadata(Type.STATIC, applyType) {

        override fun toIconPickerResult(
            iconLoaderRepository: IconLoaderRepository,
            appsRepository: AppsRepository,
            staticIcon: ByteArray?
        ): IconPickerResult? {
            return IconPickerResult.BitmapIcon(staticIcon ?: return null)
        }

    }

    data class Package(
        @SerializedName("apply_type")
        override var applyType: ApplyType,
        @SerializedName("package_name")
        var packageName: String,
        @SerializedName("shrink")
        var shrinkNonAdaptiveIcons: Boolean
    ): IconMetadata(Type.PACKAGE, applyType) {

        override fun toIconPickerResult(
            iconLoaderRepository: IconLoaderRepository,
            appsRepository: AppsRepository,
            staticIcon: ByteArray?
        ): IconPickerResult? {
            val appInfo = appsRepository.getApplicationInfoForPackage(packageName) ?: return null
            return IconPickerResult.PackageIcon(ApplicationIcon(appInfo, shrinkNonAdaptiveIcons))
        }

    }

    data class IconPack(
        @SerializedName("apply_type")
        override var applyType: ApplyType,
        @SerializedName("pack_package_name")
        var packPackageName: String,
        @SerializedName("resource_name")
        var resourceName: String,
        @SerializedName("adaptive")
        var adaptive: Boolean
    ): IconMetadata(Type.ICON_PACK, applyType) {

        override fun toIconPickerResult(
            iconLoaderRepository: IconLoaderRepository,
            appsRepository: AppsRepository,
            staticIcon: ByteArray?
        ): IconPickerResult? {
            appsRepository.getApplicationInfoForPackage(packPackageName) ?: return null
            return IconPickerResult.IconPackIcon(packPackageName, resourceName, adaptive)
        }

    }

    data class LegacyThemedIcon(
        @SerializedName("apply_type")
        override var applyType: ApplyType,
        @SerializedName("resource_name")
        var resourceName: String
    ): IconMetadata(Type.LEGACY_THEMED_ICON, applyType) {

        override fun toIconPickerResult(
            iconLoaderRepository: IconLoaderRepository,
            appsRepository: AppsRepository,
            staticIcon: ByteArray?
        ): IconPickerResult? {
            val icon = iconLoaderRepository.createLegacyThemedIcon(resourceName) ?: return null
            return IconPickerResult.LegacyThemedIcon(icon.resourceId, icon.resourceEntryName)
        }

    }

    data class Lawnicon(
        @SerializedName("apply_type")
        override var applyType: ApplyType,
        @SerializedName("resource_name")
        var resourceName: String
    ): IconMetadata(Type.LAWNICON, applyType) {

        override fun toIconPickerResult(
            iconLoaderRepository: IconLoaderRepository,
            appsRepository: AppsRepository,
            staticIcon: ByteArray?
        ): IconPickerResult? {
            val icon = iconLoaderRepository.createLawnicon(resourceName) ?: return null
            return IconPickerResult.Lawnicon(icon.resourceId, icon.resourceEntryName)
        }

    }

    enum class ApplyType {
        /**
         *  Icon has been applied automatically from an icon pack
         */
        AUTOMATIC,
        /**
         *  Icon has been manually selected by the user, and should not be overwritten by automatic
         *  icon pack applying
         */
        MANUAL
    }

    enum class Type {
        STATIC, PACKAGE, ICON_PACK, LEGACY_THEMED_ICON, LAWNICON
    }

    fun isDynamic(): Boolean {
        return when(this) {
            //Packages are assumed to be adaptive icons as most are nowadays, themed icons are always dynamic
            is Package, is Lawnicon, is LegacyThemedIcon -> true
            //Icon packs can be adaptive or not
            is IconPack -> adaptive
            else -> false
        }
    }

    abstract fun toIconPickerResult(
        iconLoaderRepository: IconLoaderRepository,
        appsRepository: AppsRepository,
        staticIcon: ByteArray?
    ): IconPickerResult?

}
