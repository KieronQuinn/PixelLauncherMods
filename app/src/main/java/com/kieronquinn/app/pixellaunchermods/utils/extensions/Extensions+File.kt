package com.kieronquinn.app.pixellaunchermods.utils.extensions

import android.os.FileObserver
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import java.io.File

fun File.observe() = callbackFlow {
    val observer = object: FileObserver(this@observe, ALL_EVENTS) {
        override fun onEvent(event: Int, path: String?) {
            trySend(event)
        }
    }
    observer.startWatching()
    awaitClose {
        observer.stopWatching()
    }
}