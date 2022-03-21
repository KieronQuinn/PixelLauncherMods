package com.kieronquinn.app.pixellaunchermods.utils.extensions

import me.zhanghai.android.appiconloader.AppIconLoader

fun AppIconLoader.setShrinkNonAdaptiveIcons(shrinkNonAdaptiveIcons: Boolean){
    AppIconLoader::class.java.getDeclaredField("mShrinkNonAdaptiveIcons").apply {
        isAccessible = true
    }.set(this, shrinkNonAdaptiveIcons)
}