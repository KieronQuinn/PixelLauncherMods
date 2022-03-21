package com.kieronquinn.app.pixellaunchermods.utils.widget

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import android.widget.RemoteViews
import com.kieronquinn.app.pixellaunchermods.widget.ProxyWidget

/**
 *  [AppWidgetHostView] that stores and exposes its [RemoteViews] for use in the [ProxyWidget]
 */
class LauncherProxyAppWidgetHostView(context: Context): AppWidgetHostView(context) {

    private val appWidgetManager =
        context.getSystemService(Context.APPWIDGET_SERVICE) as AppWidgetManager

    private var remoteViews: RemoteViews? = null

    override fun updateAppWidget(remoteViews: RemoteViews?) {
        super.updateAppWidget(remoteViews)
        this.remoteViews = remoteViews
        appWidgetManager.getAppWidgetIds(ComponentName(context, ProxyWidget::class.java)).forEach {
            appWidgetManager.updateAppWidget(it, remoteViews)
        }
    }

    fun getRemoteViews(): RemoteViews? {
        return remoteViews
    }

}