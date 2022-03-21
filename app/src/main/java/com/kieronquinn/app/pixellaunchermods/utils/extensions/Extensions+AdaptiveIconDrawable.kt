package com.kieronquinn.app.pixellaunchermods.utils.extensions

import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable

fun AdaptiveIconDrawable.getMonochromeOrForeground(): Drawable {
    return getMonochromeInternal() ?: foreground
}

//TODO move to using standard API when targeting T
private fun AdaptiveIconDrawable.getMonochromeInternal(): Drawable? {
    return try {
        AdaptiveIconDrawable::class.java.getMethod("getMonochrome").invoke(this) as? Drawable
    }catch (e: NoSuchMethodException){
        null
    }
}

fun AdaptiveIconDrawable_getInsetFraction(): Float {
    return AdaptiveIconDrawable.getExtraInsetFraction() / (1 + 2 * AdaptiveIconDrawable.getExtraInsetFraction())
}