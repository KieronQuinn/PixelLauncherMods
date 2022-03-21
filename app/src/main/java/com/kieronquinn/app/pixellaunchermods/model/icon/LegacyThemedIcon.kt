package com.kieronquinn.app.pixellaunchermods.model.icon

import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import androidx.core.content.res.ResourcesCompat
import com.kieronquinn.app.pixellaunchermods.PIXEL_LAUNCHER_PACKAGE_NAME
import com.kieronquinn.app.pixellaunchermods.repositories.IconLoaderRepositoryImpl.Companion.LEGACY_ICON_TYPE_THEMED
import com.kieronquinn.app.pixellaunchermods.utils.extensions.createPixelLauncherResources
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

data class LegacyThemedIcon(
    val type: Type,
    val normalizationScale: Float,
    val resourceId: Int,
    val resources: Resources,
    val resourceEntryName: String
) {

    companion object {
        fun parseLegacyByteArray(context: Context, byteArray: ByteArray): Pair<ByteArray, LegacyThemedIcon> {
            val resources = context.createPixelLauncherResources()
            val input = ByteArrayInputStream(byteArray)
            return DataInputStream(input).use {
                it.readByte() //Skip type
                val normalizationScale = it.readFloat()
                val resourceName = it.readUTF()
                val resourceId = resources.getIdentifier(
                    resourceName,
                    "drawable",
                    PIXEL_LAUNCHER_PACKAGE_NAME
                )
                val legacyThemedIcon = LegacyThemedIcon(
                    Type.DRAWABLE, normalizationScale, resourceId, resources, resourceName
                )
                val normalIcon = it.readBytes()
                Pair(normalIcon, legacyThemedIcon)
            }.also {
                input.close()
            }
        }
    }

    fun loadDrawable(): Drawable? {
        return ResourcesCompat.getDrawable(resources, resourceId, null)
    }

    fun toLegacyByteArray(): ByteArray {
        val resourceName = resources.getResourceName(resourceId)
        val out = ByteArrayOutputStream(2 + resourceName.length)
        DataOutputStream(out).use {
            it.writeFloat(normalizationScale)
            it.writeUTF(resourceName)
            it.flush()
        }
        return out.toByteArray().also {
            out.close()
        }
    }

    enum class Type {
        DRAWABLE, CLOCK, CALENDAR
    }

}