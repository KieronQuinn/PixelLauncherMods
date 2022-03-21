package com.kieronquinn.app.pixellaunchermods.model.editor

import android.util.Log
import com.kieronquinn.app.pixellaunchermods.model.remote.RemoteApp
import com.kieronquinn.app.pixellaunchermods.model.room.ModifiedApp
import com.kieronquinn.app.pixellaunchermods.model.room.IconMetadata
import com.kieronquinn.app.pixellaunchermods.utils.extensions.isLegacyMonoIcon
import com.kieronquinn.app.pixellaunchermods.utils.extensions.updateLegacyIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppEditorInfo(
    val componentName: String,
    var label: String,
    var icon: ByteArray?,
    var monoIcon: ByteArray?,
    var legacyIcon: ByteArray?,
    var iconColor: Int,
    var iconType: RemoteApp.Type,
    var hasChangedLabel: Boolean = false,
    var hasChangedIcon: Boolean = false,
    var hasChangedMonoIcon: Boolean = false,
    var iconMetadata: IconMetadata?,
    var monoIconMetadata: IconMetadata?,
    var staticIcon: ByteArray?,
    var staticMonoIcon: ByteArray?
) {

    companion object {

        private val DYNAMIC_TYPES = arrayOf(RemoteApp.Type.CALENDAR, RemoteApp.Type.CLOCK)

        fun merge(remoteApp: RemoteApp, modifiedApp: ModifiedApp?, nativeMonoIcons: Boolean): AppEditorInfo {
            return AppEditorInfo(
                remoteApp.componentName,
                modifiedApp?.label ?: remoteApp.label,
                if(nativeMonoIcons) { modifiedApp?.icon ?: remoteApp.icon } else null,
                if(nativeMonoIcons) { modifiedApp?.monoIcon ?: remoteApp.monoIcon } else null,
                if(nativeMonoIcons) null else { modifiedApp?.legacyIcon ?: remoteApp.icon },
                modifiedApp?.iconColor ?: remoteApp.iconColor,
                remoteApp.type,
                modifiedApp?.label != null,
                modifiedApp?.icon != null,
                modifiedApp?.monoIcon != null,
                modifiedApp?.iconMetadata,
                modifiedApp?.monoIconMetadata,
                modifiedApp?.staticIcon,
                modifiedApp?.staticMonoIcon
            )
        }
    }

    fun toRemoteApp(nativeMonoIcons: Boolean): RemoteApp {
        return if(nativeMonoIcons){
            RemoteApp(componentName, icon, monoIcon, iconColor, label, iconType)
        }else{
            RemoteApp(componentName, legacyIcon, null, iconColor, label, iconType)
        }
    }

    fun hasMonoIcon(nativeMonoIcons: Boolean): Boolean {
        if(nativeMonoIcons) return monoIcon != null
        return monoIcon != null || legacyIcon?.isLegacyMonoIcon() ?: false
    }

    suspend fun updateIcon(newIcon: ByteArray?, metadata: IconMetadata?, fullResIcon: ByteArray?) {
        withContext(Dispatchers.IO) {
            icon = newIcon
            iconMetadata = metadata
            legacyIcon = legacyIcon?.updateLegacyIcon(icon = newIcon)
            hasChangedIcon = true
            staticIcon = fullResIcon
        }
    }

    suspend fun updateMonoIcon(
        newIcon: ByteArray?,
        newLegacyIcon: ByteArray?,
        metadata: IconMetadata?,
        fullResIcon: ByteArray?
    ) {
        withContext(Dispatchers.IO) {
            monoIcon = newIcon
            monoIconMetadata = metadata
            legacyIcon = legacyIcon?.updateLegacyIcon(
                monoIcon = newLegacyIcon, clearMono = newLegacyIcon == null
            )
            hasChangedMonoIcon = true
            staticMonoIcon = fullResIcon
        }
    }

    fun updateLabel(newLabel: String) {
        label = newLabel
        hasChangedLabel = true
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AppEditorInfo

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
        if (hasChangedLabel != other.hasChangedLabel) return false
        if (hasChangedIcon != other.hasChangedIcon) return false
        if (hasChangedMonoIcon != other.hasChangedMonoIcon) return false
        if (iconMetadata != other.iconMetadata) return false
        if (monoIconMetadata != other.monoIconMetadata) return false

        return true
    }

    override fun hashCode(): Int {
        var result = componentName.hashCode()
        result = 31 * result + label.hashCode()
        result = 31 * result + (icon?.contentHashCode() ?: 0)
        result = 31 * result + (monoIcon?.contentHashCode() ?: 0)
        result = 31 * result + (legacyIcon?.contentHashCode() ?: 0)
        result = 31 * result + iconColor
        result = 31 * result + iconType.hashCode()
        result = 31 * result + hasChangedLabel.hashCode()
        result = 31 * result + hasChangedIcon.hashCode()
        result = 31 * result + hasChangedMonoIcon.hashCode()
        result = 31 * result + (iconMetadata?.hashCode() ?: 0)
        result = 31 * result + (monoIconMetadata?.hashCode() ?: 0)
        return result
    }

    fun isDynamic(): Boolean {
        return DYNAMIC_TYPES.contains(iconType)
    }

}

data class AppEditorInfoOptions(val appEditorInfo: AppEditorInfo, val loadThemedIcon: Boolean)