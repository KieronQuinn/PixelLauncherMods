package com.kieronquinn.app.pixellaunchermods.utils.extensions

import android.content.Context
import androidx.core.content.ContextCompat

fun Context.getPlateColours(): IntArray {
    val colors = IntArray(2)
    if (isDarkMode) {
        colors[0] = ContextCompat.getColor(this, android.R.color.system_neutral1_800)
        colors[1] = ContextCompat.getColor(this, android.R.color.system_accent1_100)
    } else {
        colors[0] = ContextCompat.getColor(this, android.R.color.system_accent1_100)
        colors[1] = ContextCompat.getColor(this, android.R.color.system_neutral2_700)
    }
    return colors
}

fun Int.toHexString(): String {
    return "#" + Integer.toHexString(this)
}