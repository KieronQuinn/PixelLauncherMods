package com.kieronquinn.app.pixellaunchermods.utils.extensions

import android.annotation.SuppressLint

/**
 *  Bypasses writable library issues on Android 16+, prefers directly calling nativeLoad where
 *  possible, falling back to regular load call where not.
 */
@SuppressLint("UnsafeDynamicallyLoadedCode", "DiscouragedPrivateApi")
fun loadLibrary(path: String, classLoader: ClassLoader) {
    try {
        Runtime::class.java.getDeclaredMethod(
            "nativeLoad",
            String::class.java,
            ClassLoader::class.java
        ).apply {
            isAccessible = true
        }.invoke(Runtime.getRuntime(), path, classLoader)
    }catch (e: Throwable) {
        System.load(path)
    }
}