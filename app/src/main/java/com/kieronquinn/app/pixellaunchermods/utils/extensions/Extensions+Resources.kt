package com.kieronquinn.app.pixellaunchermods.utils.extensions

import android.content.res.Resources
import androidx.annotation.ArrayRes

/**
 *  Hacky method for checking if a given resource is *probably* an adaptive icon, by checking
 *  its contents for `adaptive-icon`. May be slow on large resources.
 */
fun Resources.isAdaptiveIcon(identifier: Int): Boolean {
    return rawResourceContains(identifier, "adaptive-icon")
}

fun Resources.rawResourceContains(identifier: Int, searchString: String): Boolean {
    return try {
        openRawResource(identifier).use {
            it.bufferedReader().use { reader ->
                reader.readText().contains(searchString)
            }
        }
    }catch (e: Resources.NotFoundException){
        false
    }
}

fun Resources.getResourceIdArray(@ArrayRes resourceId: Int): Array<Int> {
    val array = obtainTypedArray(resourceId)
    val items = mutableListOf<Int>()
    for(i in 0 until array.length()){
        items.add(array.getResourceId(i, 0))
    }
    array.recycle()
    return items.toTypedArray()
}