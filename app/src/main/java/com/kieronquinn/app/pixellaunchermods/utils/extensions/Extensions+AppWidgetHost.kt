package com.kieronquinn.app.pixellaunchermods.utils.extensions

import android.annotation.SuppressLint
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.content.Context
import android.content.IntentSender
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.WindowManager
import com.android.internal.appwidget.IAppWidgetService
import com.kieronquinn.app.pixellaunchermods.BuildConfig
import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.model.tweaks.WidgetReplacement
import kotlinx.coroutines.delay

@SuppressLint("DiscouragedPrivateApi")
fun AppWidgetHost.getIntentSenderForConfigureActivity(appWidgetId: Int, intentFlags: Int): IntentSender {
    val sService = AppWidgetHost::class.java.getDeclaredField("sService").apply {
        isAccessible = true
    }.get(this) as IAppWidgetService
    return sService.createAppWidgetConfigIntentSender(BuildConfig.APPLICATION_ID, appWidgetId, intentFlags)
}

suspend fun AppWidgetHostView.renderToBitmap(replacement: WidgetReplacement): Bitmap {
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val width = windowManager.currentWindowMetrics.bounds.width()
    val height = when(replacement){
        WidgetReplacement.TOP -> R.dimen.widget_preview_height_top
        WidgetReplacement.BOTTOM -> R.dimen.widget_preview_height_bottom
        WidgetReplacement.NONE -> 0
    }.run {
        context.resources.getDimension(this).toInt()
    }
    return renderToBitmap(width, height)
}

private suspend fun AppWidgetHostView.renderToBitmap(width: Int, height: Int): Bitmap {
    val widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
    val heightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
    measure(widthSpec, heightSpec)
    delay(50L)
    layout(0, 0, width, height)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    draw(canvas)
    return bitmap
}