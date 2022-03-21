package com.kieronquinn.app.pixellaunchermods.utils.extensions

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.kieronquinn.app.pixellaunchermods.LAWNICONS_PACKAGE_NAME
import com.kieronquinn.app.pixellaunchermods.PIXEL_LAUNCHER_PACKAGE_NAME
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

fun Context.broadcastReceiverAsFlow(vararg actions: String): Flow<Intent> {
    return broadcastReceiverAsFlow(*actions.map { IntentFilter(it) }.toTypedArray())
}

fun Context.broadcastReceiverAsFlow(vararg intentFilters: IntentFilter) = callbackFlow {
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            trySend(intent)
        }
    }
    intentFilters.forEach {
        registerReceiver(receiver, it)
    }
    awaitClose {
        unregisterReceiver(receiver)
    }
}

fun Context.createPixelLauncherResources(): Resources {
    return packageManager.getResourcesForApplication(PIXEL_LAUNCHER_PACKAGE_NAME)
}

fun Context.createLawniconsResources(): Resources {
    return packageManager.getResourcesForApplication(LAWNICONS_PACKAGE_NAME)
}

fun Context.isPackageInstalled(packageName: String): Boolean {
    return try {
        packageManager.getApplicationInfo(packageName, 0)
        true
    }catch (e: PackageManager.NameNotFoundException){
        false
    }
}

val Context.isDarkMode: Boolean
    get() {
        return when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> true
            Configuration.UI_MODE_NIGHT_NO -> false
            Configuration.UI_MODE_NIGHT_UNDEFINED -> false
            else -> false
        }
    }

// From https://stackoverflow.com/a/55280832
@ColorInt
fun Context.getColorResCompat(@AttrRes id: Int): Int {
    val resolvedAttr = TypedValue()
    this.theme.resolveAttribute(id, resolvedAttr, true)
    val colorRes = resolvedAttr.run { if (resourceId != 0) resourceId else data }
    return ContextCompat.getColor(this, colorRes)
}