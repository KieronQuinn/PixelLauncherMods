package com.kieronquinn.app.pixellaunchermods.utils.extensions

import android.content.res.AssetManager
import java.io.File
import java.io.FileOutputStream

//https://stackoverflow.com/a/54193251/1088334
fun AssetManager.copyRecursively(assetPath: String, targetFile: File) {
    val list = list(assetPath) ?: return
    if (list.isEmpty()) { // assetPath is file
        open(assetPath).use { input ->
            FileOutputStream(targetFile.absolutePath).use { output ->
                input.copyTo(output)
                output.flush()
            }
        }
    } else { // assetPath is folder
        targetFile.delete()
        targetFile.mkdir()
        list.forEach {
            copyRecursively("$assetPath/$it", File(targetFile, it))
        }
    }
}