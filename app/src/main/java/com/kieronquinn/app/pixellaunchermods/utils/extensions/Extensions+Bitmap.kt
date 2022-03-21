package com.kieronquinn.app.pixellaunchermods.utils.extensions

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.util.Log
import com.kieronquinn.app.pixellaunchermods.repositories.IconLoaderRepositoryImpl
import java.io.*
import java.nio.ByteBuffer

fun Bitmap_decodeRawBitmap(byteArray: ByteArray, width: Int, height: Int, config: Bitmap.Config): Bitmap {
    return Bitmap.createBitmap(width, height, config).apply {
        copyPixelsFromBuffer(ByteBuffer.wrap(byteArray))
    }
}

fun Bitmap.compressRaw(): ByteArray {
    val bytes = ByteArray(width * height)
    copyPixelsToBuffer(ByteBuffer.wrap(bytes))
    return bytes
}

fun Bitmap_getSize(byteArray: ByteArray): Rect {
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size, options)
    return Rect(0, 0, options.outWidth, options.outHeight)
}

/**
 * Compresses the bitmap to a byte array for serialization.
 */
fun Bitmap.compress(): ByteArray? {
    val out = ByteArrayOutputStream(getExpectedBitmapSize())
    return try {
        compress(Bitmap.CompressFormat.PNG, 100, out)
        out.flush()
        out.close()
        out.toByteArray()
    } catch (e: IOException) {
        null
    }
}

/**
 * Try go guesstimate how much space the icon will take when serialized to avoid unnecessary
 * allocations/copies during the write (4 bytes per pixel).
 */
private fun Bitmap.getExpectedBitmapSize(): Int {
    return width * height * 4
}

fun ByteArray.isLegacyMonoIcon(): Boolean {
    if(size == 0) return false
    return this[0].toInt() == IconLoaderRepositoryImpl.LEGACY_ICON_TYPE_THEMED
}

fun ByteArray.updateLegacyIcon(icon: ByteArray? = null, monoIcon: ByteArray? = null, clearMono: Boolean = false): ByteArray? {
    val currentIcon = this
    val currentType = currentIcon[0]
    val currentIcons: Pair<ByteArray?, ByteArray?> = when(currentType.toInt()){
        IconLoaderRepositoryImpl.LEGACY_ICON_TYPE_NORMAL -> {
            Pair(currentIcon.copyOfRange(1, currentIcon.size), null)
        }
        IconLoaderRepositoryImpl.LEGACY_ICON_TYPE_THEMED -> {
            extractLegacyMonoIcon(currentIcon)
        }
        else -> Pair(null, null)
    }
    val newIcon = icon ?: currentIcons.first ?: return null
    val newMonoIcon = if(!clearMono) monoIcon ?: currentIcons.second else null
    return if(newMonoIcon != null){
        createLegacyMonoIcon(newIcon, newMonoIcon)
    }else{
        createLegacyIcon(newIcon)
    }
}

private fun createLegacyIcon(icon: ByteArray): ByteArray {
    val out = ByteArrayOutputStream(1 + icon.size)
    DataOutputStream(out).use { output ->
        output.write(IconLoaderRepositoryImpl.LEGACY_ICON_TYPE_NORMAL)
        output.write(icon)
        output.flush()
    }
    return out.toByteArray().also {
        out.close()
    }
}

private fun createLegacyMonoIcon(icon: ByteArray, monoIcon: ByteArray): ByteArray {
    val out = ByteArrayOutputStream(1 + icon.size + monoIcon.size)
    DataOutputStream(out).use { output ->
        output.write(IconLoaderRepositoryImpl.LEGACY_ICON_TYPE_THEMED)
        output.write(monoIcon)
        output.write(icon)
        output.flush()
    }
    return out.toByteArray().also {
        out.close()
    }
}

private fun extractLegacyMonoIcon(byteArray: ByteArray): Pair<ByteArray, ByteArray> {
    val input = ByteArrayInputStream(byteArray)
    return DataInputStream(input).use {
        it.readByte() //Skip type
        val normalizationScale = it.readFloat()
        val resourceName = it.readUTF()
        val out = ByteArrayOutputStream(2 + resourceName.length)
        DataOutputStream(out).use { output ->
            output.writeFloat(normalizationScale)
            output.writeUTF(resourceName)
            output.flush()
        }
        val legacyThemedIcon = out.toByteArray()
        out.close()
        val normalIcon = it.readBytes()
        Pair(normalIcon, legacyThemedIcon)
    }.also {
        input.close()
    }
}

fun ByteArray.extractLegacyNormalIcon(): ByteArray {
    return when(this[0].toInt()){
        IconLoaderRepositoryImpl.LEGACY_ICON_TYPE_NORMAL -> {
            loadLegacyNormalIcon()
        }
        IconLoaderRepositoryImpl.LEGACY_ICON_TYPE_THEMED -> {
            loadLegacyNormalIconFromThemed()
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

private fun ByteArray.loadLegacyNormalIconFromThemed(): ByteArray {
    val input = ByteArrayInputStream(this)
    return DataInputStream(input).use {
        it.readByte() //Skip type
        it.readFloat() //Skip normalization scale
        it.readUTF() //Skip resource name
        it.readBytes()
    }.also {
        input.close()
    }
}