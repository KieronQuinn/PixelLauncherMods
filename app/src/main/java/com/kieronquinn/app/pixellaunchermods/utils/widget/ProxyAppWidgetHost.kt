package com.kieronquinn.app.pixellaunchermods.utils.widget

import android.appwidget.AppWidgetHost
import android.content.Context
import com.android.internal.appwidget.IAppWidgetService
import com.kieronquinn.app.pixellaunchermods.utils.extensions.reflect

/**
 *  [AppWidgetHost] with the ability to replace its normally private [IAppWidgetService]
 *  connection with a custom one, retrieved over the root service.
 */
class ProxyAppWidgetHost(
    context: Context,
    hostId: Int
): AppWidgetHost(context, hostId) {

    private var sService by reflect<AppWidgetHost, IAppWidgetService>("sService")

    fun setService(service: IAppWidgetService) {
        sService = service
    }

}