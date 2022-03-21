package com.kieronquinn.app.pixellaunchermods.utils.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.util.SparseArray
import com.kieronquinn.app.pixellaunchermods.utils.extensions.reflect

/**
 *  [AppWidgetHost] with exposed views (via [getView]), and cached state to prevent [startListening]
 *  and [stopListening] calls when already in that state.
 *
 *  Creates [LauncherProxyAppWidgetHostView] from [onCreateView].
 */
class LauncherProxyAppWidgetHost(context: Context, hostId: Int): AppWidgetHost(context, hostId) {

    private val mViews by reflect<AppWidgetHost, SparseArray<AppWidgetHostView>>("mViews")
    private var isListening = false

    override fun onCreateView(
        context: Context,
        appWidgetId: Int,
        appWidget: AppWidgetProviderInfo?
    ): AppWidgetHostView {
        return LauncherProxyAppWidgetHostView(context)
    }

    fun getView(appWidgetId: Int): AppWidgetHostView? {
        return mViews[appWidgetId]
    }

    @Synchronized
    override fun startListening() {
        if(isListening) return
        super.startListening()
        isListening = true
    }

    @Synchronized
    override fun stopListening() {
        if(!isListening) return
        try {
            super.stopListening()
        }catch (e: NullPointerException){
            //Provider has been removed, ignore
        }
        isListening = false
    }

}