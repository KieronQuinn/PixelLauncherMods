package com.kieronquinn.app.pixellaunchermods.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import com.kieronquinn.app.pixellaunchermods.BuildConfig
import com.kieronquinn.app.pixellaunchermods.R
import org.koin.core.component.KoinComponent

class BlankWidget: AppWidgetProvider(), KoinComponent {

    companion object {

        fun sendUpdate(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, BlankWidget::class.java)
            )
            if(appWidgetIds.isEmpty()) return
            Intent(context, BlankWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            }.also {
                context.sendBroadcast(it)
            }
        }
    }

    private val provider = ComponentName(BuildConfig.APPLICATION_ID, BlankWidget::class.java.name)

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        appWidgetIds.forEach {
            setupWidget(appWidgetManager, context, it)
        }
    }

    private fun setupAllWidgets(context: Context){
        val appWidgetManager = context.getSystemService(Context.APPWIDGET_SERVICE) as AppWidgetManager
        appWidgetManager.getAppWidgetIds(provider).forEach {
            setupWidget(appWidgetManager, context, it)
        }
    }

    private fun setupWidget(
        appWidgetManager: AppWidgetManager,
        context: Context,
        appWidgetId: Int
    ) {
        val emptyView = RemoteViews(context.packageName, R.layout.widget_blank)
        appWidgetManager.updateAppWidget(appWidgetId, emptyView)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        setupWidget(appWidgetManager, context, appWidgetId)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val appWidgetManager = context.getSystemService(Context.APPWIDGET_SERVICE) as AppWidgetManager
        appWidgetIds.forEach {
            setupWidget(appWidgetManager, context, it)
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        setupAllWidgets(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        setupAllWidgets(context)
    }

    override fun onRestored(context: Context, oldWidgetIds: IntArray?, newWidgetIds: IntArray?) {
        super.onRestored(context, oldWidgetIds, newWidgetIds)
        setupAllWidgets(context)
    }

}