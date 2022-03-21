package com.kieronquinn.app.pixellaunchermods.model.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.kieronquinn.app.pixellaunchermods.utils.room.IconMetadataConverter

@TypeConverters(IconMetadataConverter::class)
@Entity(tableName = "shortcuts")
data class ModifiedShortcut(
    @PrimaryKey val intent: String,
    @ColumnInfo(name = "title") val title: String?,
    @ColumnInfo(name = "icon") val icon: ByteArray?,
    @ColumnInfo(name = "icon_metadata") val iconMetadata: IconMetadata?,
    @ColumnInfo(name = "original_title") val originalTitle: String?,
    @ColumnInfo(name = "original_icon") val originalIcon: ByteArray?
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ModifiedShortcut

        if (intent != other.intent) return false
        if (title != other.title) return false
        if (icon != null) {
            if (other.icon == null) return false
            if (!icon.contentEquals(other.icon)) return false
        } else if (other.icon != null) return false
        if (iconMetadata != other.iconMetadata) return false
        if (originalTitle != other.originalTitle) return false
        if (originalIcon != null) {
            if (other.originalIcon == null) return false
            if (!originalIcon.contentEquals(other.originalIcon)) return false
        } else if (other.originalIcon != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = intent.hashCode()
        result = 31 * result + (title?.hashCode() ?: 0)
        result = 31 * result + (icon?.contentHashCode() ?: 0)
        result = 31 * result + (iconMetadata?.hashCode() ?: 0)
        result = 31 * result + (originalTitle?.hashCode() ?: 0)
        result = 31 * result + (originalIcon?.contentHashCode() ?: 0)
        return result
    }

}