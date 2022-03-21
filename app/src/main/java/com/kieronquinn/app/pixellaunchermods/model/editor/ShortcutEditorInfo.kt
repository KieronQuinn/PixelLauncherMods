package com.kieronquinn.app.pixellaunchermods.model.editor

import com.kieronquinn.app.pixellaunchermods.model.remote.RemoteApp
import com.kieronquinn.app.pixellaunchermods.model.remote.RemoteFavourite
import com.kieronquinn.app.pixellaunchermods.model.room.IconMetadata
import com.kieronquinn.app.pixellaunchermods.model.room.ModifiedShortcut
import com.kieronquinn.app.pixellaunchermods.utils.extensions.updateLegacyIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ShortcutEditorInfo(
    val intent: String,
    var title: String?,
    var hasChangedTitle: Boolean,
    var icon: ByteArray?,
    var hasChangedIcon: Boolean,
    var iconMetadata: IconMetadata?,
    val originalTitle: String?,
    val originalIcon: ByteArray?
) {

    companion object {
        fun merge(remoteFavourite: RemoteFavourite, modifiedShortcut: ModifiedShortcut?): ShortcutEditorInfo {
            return ShortcutEditorInfo(
                remoteFavourite.intent!!,
                modifiedShortcut?.title ?: remoteFavourite.title,
                modifiedShortcut?.title != null,
                modifiedShortcut?.icon ?: remoteFavourite.icon,
                modifiedShortcut?.icon != null,
                modifiedShortcut?.iconMetadata,
                modifiedShortcut?.originalTitle ?: remoteFavourite.title,
                modifiedShortcut?.originalIcon ?: remoteFavourite.icon
            )
        }
    }

    fun toRemoteFavourite(): RemoteFavourite {
        return RemoteFavourite(
            title,
            intent,
            0,
            0,
            0,
            0,
            0,
            null,
            0,
            icon,
            RemoteFavourite.Type.SHORTCUT
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ShortcutEditorInfo

        if (intent != other.intent) return false
        if (title != other.title) return false
        if (icon != null) {
            if (other.icon == null) return false
            if (!icon.contentEquals(other.icon)) return false
        } else if (other.icon != null) return false
        if (iconMetadata != other.iconMetadata) return false

        return true
    }

    override fun hashCode(): Int {
        var result = intent.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + (icon?.contentHashCode() ?: 0)
        result = 31 * result + iconMetadata.hashCode()
        return result
    }

    suspend fun updateIcon(newIcon: ByteArray?, metadata: IconMetadata?) {
        withContext(Dispatchers.IO) {
            icon = newIcon
            iconMetadata = metadata
            hasChangedIcon = true
        }
    }

    fun updateLabel(newLabel: String) {
        title = newLabel
        hasChangedTitle = true
    }

}