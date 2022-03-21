package com.kieronquinn.app.pixellaunchermods.model.room

import androidx.core.graphics.drawable.IconCompat
import androidx.room.*
import com.kieronquinn.app.pixellaunchermods.model.remote.RemoteApp
import com.kieronquinn.app.pixellaunchermods.utils.room.IconMetadataConverter

/**
 *  Represents an app that has been modified in the local database, which will then be applied to
 *  the remote database after changes.
 *
 *  @param componentName: The app's component name, should match that in the remote database (key)
 *  @param icon: The app's resized, normal (app drawer) icon, used for Android 13+ (can be null)
 *  @param monoIcon: The app's resized, themed (homescreen) icon, used for Android 13+ (can be null)
 *  @param legacyIcon: The app's **combined** icon and compatible mono icon, used on Android 12 (can be null)
 *  @param iconColor: The icon's colour, used when loading in the launcher. Currently not updated.
 *  @param iconType: The type of the icon, can be one of [RemoteApp.Type]
 *  @param iconMetadata: The metadata required to recreate the icon, if it is set
 *  @param monoIconMetadata: The metadata required to recreate the mono icon, if it is set
 *  @param staticIcon: The original image used for the icon, resized to 512x512 (used when recreating the icon, if it is set)
 *  @param staticMonoIcon: The original image used for the mono icon, resized to 512x512 (used when recreating the mono icon, if it is set)
 */
@TypeConverters(IconMetadataConverter::class)
@Entity(tableName = "apps")
data class ModifiedApp(
    @PrimaryKey val componentName: String,
    @ColumnInfo(name = "label") val label: String?,
    @ColumnInfo(name = "icon") val icon: ByteArray?,
    @ColumnInfo(name = "mono_icon") val monoIcon: ByteArray?,
    @ColumnInfo(name = "legacy_icon") val legacyIcon: ByteArray?,
    @ColumnInfo(name = "color") val iconColor: Int?,
    @ColumnInfo(name = "icon_type") val iconType: RemoteApp.Type,
    @ColumnInfo(name = "icon_metadata") val iconMetadata: IconMetadata?,
    @ColumnInfo(name = "mono_icon_metadata") val monoIconMetadata: IconMetadata?,
    @ColumnInfo(name = "static_icon") val staticIcon: ByteArray?,
    @ColumnInfo(name = "static_mono_icon") val staticMonoIcon: ByteArray?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ModifiedApp

        if (componentName != other.componentName) return false
        if (label != other.label) return false
        if (icon != null) {
            if (other.icon == null) return false
            if (!icon.contentEquals(other.icon)) return false
        } else if (other.icon != null) return false
        if (monoIcon != null) {
            if (other.monoIcon == null) return false
            if (!monoIcon.contentEquals(other.monoIcon)) return false
        } else if (other.monoIcon != null) return false
        if (legacyIcon != null) {
            if (other.legacyIcon == null) return false
            if (!legacyIcon.contentEquals(other.legacyIcon)) return false
        } else if (other.legacyIcon != null) return false
        if (iconColor != other.iconColor) return false
        if (iconType != other.iconType) return false
        if (iconMetadata != other.iconMetadata) return false
        if (monoIconMetadata != other.monoIconMetadata) return false
        if (staticIcon != null) {
            if (other.staticIcon == null) return false
            if (!staticIcon.contentEquals(other.staticIcon)) return false
        } else if (other.staticIcon != null) return false
        if (staticMonoIcon != null) {
            if (other.staticMonoIcon == null) return false
            if (!staticMonoIcon.contentEquals(other.staticMonoIcon)) return false
        } else if (other.staticMonoIcon != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = componentName.hashCode()
        result = 31 * result + (label?.hashCode() ?: 0)
        result = 31 * result + (icon?.contentHashCode() ?: 0)
        result = 31 * result + (monoIcon?.contentHashCode() ?: 0)
        result = 31 * result + (legacyIcon?.contentHashCode() ?: 0)
        result = 31 * result + (iconColor ?: 0)
        result = 31 * result + iconType.hashCode()
        result = 31 * result + (iconMetadata?.hashCode() ?: 0)
        result = 31 * result + (monoIconMetadata?.hashCode() ?: 0)
        result = 31 * result + (staticIcon?.contentHashCode() ?: 0)
        result = 31 * result + (staticMonoIcon?.contentHashCode() ?: 0)
        return result
    }

}