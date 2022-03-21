package com.kieronquinn.app.pixellaunchermods.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.RemoteViews
import com.kieronquinn.app.pixellaunchermods.BuildConfig
import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.components.notifications.NotificationId
import com.kieronquinn.app.pixellaunchermods.repositories.ProxyAppWidgetRepository
import com.kieronquinn.app.pixellaunchermods.repositories.SettingsRepository
import com.kieronquinn.app.pixellaunchermods.utils.widget.LauncherProxyAppWidgetHostView
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ProxyWidget: AppWidgetProvider(), KoinComponent {

    companion object {

        fun sendUpdate(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, ProxyWidget::class.java)
            )
            if(appWidgetIds.isEmpty()) return
            Intent(context, ProxyWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            }.also {
                context.sendBroadcast(it)
            }
        }
    }

    private val settings by inject<SettingsRepository>()
    private val appWidgetRepository by inject<ProxyAppWidgetRepository>()
    private val provider = ComponentName(BuildConfig.APPLICATION_ID, ProxyWidget::class.java.name)

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
        val previewWidgetIds = arrayOf(
            settings.proxyWidgetPreviewIdTop.getSync(),
            settings.proxyWidgetPreviewIdBottom.getSync()
        )
        val remoteAppWidgetId = settings.qsbWidgetId.getSync()
        val remoteProvider = settings.qsbWidgetProvider.getSync()
        val provider = appWidgetManager.installedProviders.firstOrNull {
            it.provider.flattenToShortString() == remoteProvider
        }
        setupWidget(
            appWidgetManager, context, appWidgetId, previewWidgetIds, remoteAppWidgetId, provider
        )
    }

    private fun setupWidget(
        appWidgetManager: AppWidgetManager,
        context: Context,
        appWidgetId: Int,
        previewWidgetIds: Array<Int>,
        remoteAppWidgetId: Int,
        provider: AppWidgetProviderInfo?
    ) {
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        when {
            provider != null -> {
                setupProxyWidget(context, appWidgetManager, remoteAppWidgetId, provider, appWidgetId, options)
            }
            previewWidgetIds.contains(appWidgetId) -> {
                setupPreviewWidget(appWidgetManager, context, appWidgetId)
            }
            else -> {
                setupBlankWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    private fun setupProxyWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        remoteAppWidgetId: Int,
        provider: AppWidgetProviderInfo,
        appWidgetId: Int,
        options: Bundle
    ){
        val remoteWidget = appWidgetRepository.getAppWidgetHostView(
            remoteAppWidgetId,
            context,
            provider,
            options
        ) as LauncherProxyAppWidgetHostView
        val wrapped = RemoteViews(context.packageName, R.layout.widget_wrapper).apply {
            addView(R.id.plm_root, remoteWidget.getRemoteViews() ?: return@apply)
        }
        appWidgetManager.updateAppWidget(appWidgetId, wrapped)
    }

    private fun setupPreviewWidget(
        appWidgetManager: AppWidgetManager,
        context: Context,
        appWidgetId: Int
    ) {
        //Showing in the preview pane, show preview
        appWidgetManager.updateAppWidget(
            appWidgetId,
            RemoteViews(context.packageName, R.layout.widget_preview)
        )
    }

    private fun setupBlankWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val configurePendingIntent = PendingIntent.getActivity(
            context,
            NotificationId.PROXY_WIDGET_CLICK.ordinal,
            Intent(Intent.ACTION_VIEW).apply {
                `package` = context.packageName
                data = Uri.parse("plm://widgetcomponentpicker")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val emptyView = RemoteViews(context.packageName, R.layout.widget_empty).apply {
            setOnClickPendingIntent(R.id.widget_empty_configure, configurePendingIntent)
        }
        appWidgetManager.updateAppWidget(appWidgetId, emptyView)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        appWidgetRepository.onAppWidgetHostOptionsChanged(appWidgetId, newOptions)
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