package com.kieronquinn.app.pixellaunchermods.model.remote

import android.os.Bundle
import android.os.Parcelable
import androidx.core.os.bundleOf
import kotlinx.parcelize.Parcelize

@Parcelize
data class RemoteApp(
    val componentName: String,
    val icon: ByteArray?,
    val monoIcon: ByteArray?,
    val iconColor: Int,
    val label: String,
    var type: Type = Type.NORMAL
): Parcelable {

    private companion object {
        private const val KEY_COMPONENT = "component_name"
        private const val KEY_ICON = "icon"
        private const val KEY_MONO_ICON = "mono_icon"
        private const val KEY_ICON_COLOR = "icon_color"
        private const val KEY_LABEL = "label"
        private const val KEY_TYPE = "type"
    }

    enum class Type {
        NORMAL, CLOCK, CALENDAR, APP_SHORTCUT
    }

    constructor(bundle: Bundle): this(
        bundle.getString(KEY_COMPONENT) ?: throw InvalidBundleException(),
        bundle.getByteArray(KEY_ICON),
        bundle.getByteArray(KEY_MONO_ICON),
        bundle.getInt(KEY_ICON_COLOR),
        bundle.getString(KEY_LABEL) ?: throw InvalidBundleException(),
        bundle.getSerializable(KEY_TYPE) as Type
    )

    fun asBundle(): Bundle {
        return bundleOf(
            KEY_COMPONENT to componentName,
            KEY_ICON to icon,
            KEY_MONO_ICON to monoIcon,
            KEY_ICON_COLOR to iconColor,
            KEY_LABEL to label,
            KEY_TYPE to type
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RemoteApp

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
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = componentName.hashCode()
        result = 31 * result + (icon?.contentHashCode() ?: 0)
        result = 31 * result + (monoIcon?.contentHashCode() ?: 0)
        result = 31 * result + iconColor
        result = 31 * result + label.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }

    fun isLabelImmutable(): Boolean {
        return type == Type.APP_SHORTCUT
    }

    /**
     *  Compares the current [RemoteApp] to a given [ModifiedRemoteApp], returning `false` if
     *  there are any differences in field content
     */
    fun compareTo(modified: ModifiedRemoteApp): Boolean {
        if(modified.label != null && modified.label != label) {
            return false
        }
        if(modified.icon != null && !modified.icon.contentEquals(icon)){
            return false
        }
        if(modified.monoIcon != null && !modified.monoIcon.contentEquals(monoIcon)) {
            return false
        }
        return true
    }

    override fun toString(): String {
        return "RemoteApp[componentName=$componentName, label=$label, icon size: ${icon?.size}, mono icon size: ${monoIcon?.size}, iconColor: $iconColor, type: $type]"
    }

}

data class RemoteAppOptions(val remoteApp: RemoteApp, val mono: Boolean)

class InvalidBundleException: Exception()