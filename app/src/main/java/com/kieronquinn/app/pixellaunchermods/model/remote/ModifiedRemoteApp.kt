package com.kieronquinn.app.pixellaunchermods.model.remote

import android.os.Bundle
import androidx.core.os.bundleOf

data class ModifiedRemoteApp(
    val componentName: String,
    val icon: ByteArray?,
    val monoIcon: ByteArray?,
    val iconColor: Int?,
    val label: String?,
    val iconType: RemoteApp.Type
) {

    private companion object {
        private const val KEY_COMPONENT = "component_name"
        private const val KEY_ICON = "icon"
        private const val KEY_MONO_ICON = "mono_icon"
        private const val KEY_ICON_COLOR = "icon_color"
        private const val KEY_LABEL = "label"
        private const val KEY_ICON_TYPE = "icon_type"
    }

    constructor(bundle: Bundle): this(
        bundle.getString(KEY_COMPONENT) ?: throw InvalidBundleException(),
        bundle.getByteArray(KEY_ICON),
        bundle.getByteArray(KEY_MONO_ICON),
        bundle.getInt(KEY_ICON_COLOR),
        bundle.getString(KEY_LABEL),
        bundle.getSerializable(KEY_ICON_TYPE) as RemoteApp.Type
    )

    fun toBundle(): Bundle {
        return bundleOf(
            KEY_COMPONENT to componentName,
            KEY_ICON to icon,
            KEY_MONO_ICON to monoIcon,
            KEY_ICON_COLOR to iconColor,
            KEY_LABEL to label,
            KEY_ICON_TYPE to iconType
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ModifiedRemoteApp

        if (componentName != other.componentName) return false
        if (icon != null) {
            if (other.icon == null) return false
            if (!icon.contentEquals(other.icon)) return false
        } else if (other.icon != null) return false
        if (monoIcon != null) {
            if (other.monoIcon == null) return false
            if (!monoIcon.contentEquals(other.monoIcon)) return false
        } else if (other.monoIcon != null) return false
        if (iconColor != other.iconColor) return false
        if (label != other.label) return false
        if (iconType != other.iconType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = componentName.hashCode()
        result = 31 * result + (icon?.contentHashCode() ?: 0)
        result = 31 * result + (monoIcon?.contentHashCode() ?: 0)
        result = 31 * result + (iconColor ?: 0)
        result = 31 * result + (label?.hashCode() ?: 0)
        result = 31 * result + iconType.hashCode()
        return result
    }

}