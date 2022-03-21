package com.kieronquinn.app.pixellaunchermods.utils.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context

/**
 *  [AppWidgetHost] that creates a [PreviewAppWidgetHostView] instance.
 */
class PreviewAppWidgetHost(context: Context, hostId: Int): AppWidgetHost(context, hostId) {

    override fun onCreateView(
        context: Context,
        appWidgetId: Int,
        appWidget: AppWidgetProviderInfo?
    ): AppWidgetHostView {
        return PreviewAppWidgetHostView(context)
    }

}