package com.kieronquinn.app.pixellaunchermods.utils.extensions

import android.content.res.Resources

/**
 *  Hacky method for checking if a given resource is *probably* an adaptive icon, by checking
 *  its contents for `adaptive-icon`. May be slow on large resources.
 */
fun Resources.isAdaptiveIcon(identifier: Int): Boolean {
    return rawResourceContains(identifier, "adaptive-icon")
}

fun Resources.rawResourceContains(identifier: Int, searchString: String): Boolean {
    return openRawResource(identifier).use {
        it.bufferedReader().use { reader ->
            reader.readText().contains(searchString)
        }
    }
}