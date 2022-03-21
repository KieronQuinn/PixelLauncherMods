package com.kieronquinn.app.pixellaunchermods.utils.widget

import android.app.IApplicationThread
import android.app.IServiceConnection
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Intent
import android.content.IntentSender
import android.content.pm.ParceledListSlice
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.widget.RemoteViews
import com.android.internal.appwidget.IAppWidgetHost
import com.android.internal.appwidget.IAppWidgetService

/**
 *  "Proxy" for [IAppWidgetService], clearing the caller identity and running with the system
 *  service, returning the result. This class will also replace all passed "callingPackage" names
 *  with a spoofed one.
 */
class ProxyAppWidgetService(
    private val original: IAppWidgetService,
    private val spoofCallingPackage: String
): IAppWidgetService.Stub() {

    private fun <T> runWithOriginal(block: (IAppWidgetService) -> T): T {
        val identity = Binder.clearCallingIdentity()
        return block(original).also {
            Binder.restoreCallingIdentity(identity)
        }
    }

    override fun startListening(
        host: IAppWidgetHost?,
        callingPackage: String?,
        hostId: Int,
        appWidgetIds: IntArray?
    ): ParceledListSlice<*> {
        return runWithOriginal {
            it.startListening(
                host, spoofCallingPackage, hostId, appWidgetIds
            )
        }
    }

    override fun stopListening(callingPackage: String?, hostId: Int) {
        runWithOriginal {
            it.stopListening(spoofCallingPackage, hostId)
        }
    }

    override fun allocateAppWidgetId(callingPackage: String?, hostId: Int): Int {
        return runWithOriginal {
            it.allocateAppWidgetId(spoofCallingPackage, hostId)
        }
    }

    override fun deleteAppWidgetId(callingPackage: String?, appWidgetId: Int) {
        runWithOriginal {
            it.deleteAppWidgetId(spoofCallingPackage, appWidgetId)
        }
    }

    override fun deleteHost(packageName: String?, hostId: Int) {
        runWithOriginal {
            it.deleteHost(packageName, hostId)
        }
    }

    override fun deleteAllHosts() {
        runWithOriginal {
            it.deleteAllHosts()
        }
    }

    override fun getAppWidgetViews(callingPackage: String?, appWidgetId: Int): RemoteViews? {
        return runWithOriginal {
            it.getAppWidgetViews(spoofCallingPackage, appWidgetId)
        }
    }

    override fun getAppWidgetIdsForHost(callingPackage: String?, hostId: Int): IntArray {
        return runWithOriginal {
            it.getAppWidgetIdsForHost(spoofCallingPackage, hostId)
        }
    }

    override fun createAppWidgetConfigIntentSender(
        callingPackage: String?,
        appWidgetId: Int,
        intentFlags: Int
    ): IntentSender {
        return runWithOriginal {
            it.createAppWidgetConfigIntentSender(spoofCallingPackage, appWidgetId, intentFlags)
        }
    }

    override fun updateAppWidgetIds(
        callingPackage: String?,
        appWidgetIds: IntArray?,
        views: RemoteViews?
    ) {
        runWithOriginal {
            it.updateAppWidgetIds(spoofCallingPackage, appWidgetIds, views)
        }
    }

    override fun updateAppWidgetOptions(
        callingPackage: String?,
        appWidgetId: Int,
        extras: Bundle?
    ) {
        runWithOriginal {
            it.updateAppWidgetOptions(spoofCallingPackage, appWidgetId, extras)
        }
    }

    override fun getAppWidgetOptions(callingPackage: String?, appWidgetId: Int): Bundle {
        return runWithOriginal {
            it.getAppWidgetOptions(spoofCallingPackage, appWidgetId)
        }
    }

    override fun partiallyUpdateAppWidgetIds(
        callingPackage: String?,
        appWidgetIds: IntArray?,
        views: RemoteViews?
    ) {
        runWithOriginal {
            it.partiallyUpdateAppWidgetIds(spoofCallingPackage, appWidgetIds, views)
        }
    }

    override fun updateAppWidgetProvider(provider: ComponentName?, views: RemoteViews?) {
        runWithOriginal {
            it.updateAppWidgetProvider(provider, views)
        }
    }

    override fun updateAppWidgetProviderInfo(provider: ComponentName?, metadataKey: String?) {
        runWithOriginal {
            it.updateAppWidgetProviderInfo(provider, metadataKey)
        }
    }

    override fun notifyAppWidgetViewDataChanged(
        packageName: String?,
        appWidgetIds: IntArray?,
        viewId: Int
    ) {
        runWithOriginal {
            it.notifyAppWidgetViewDataChanged(packageName, appWidgetIds, viewId)
        }
    }

    override fun getInstalledProvidersForProfile(
        categoryFilter: Int,
        profileId: Int,
        packageName: String?
    ): ParceledListSlice<*> {
        return runWithOriginal {
            it.getInstalledProvidersForProfile(categoryFilter, profileId, packageName)
        }
    }

    override fun getAppWidgetInfo(
        callingPackage: String?,
        appWidgetId: Int
    ): AppWidgetProviderInfo {
        return runWithOriginal {
            it.getAppWidgetInfo(spoofCallingPackage, appWidgetId)
        }
    }

    override fun hasBindAppWidgetPermission(packageName: String?, userId: Int): Boolean {
        return runWithOriginal {
            it.hasBindAppWidgetPermission(packageName, userId)
        }
    }

    override fun setBindAppWidgetPermission(
        packageName: String?,
        userId: Int,
        permission: Boolean
    ) {
        runWithOriginal {
            it.setBindAppWidgetPermission(packageName, userId, permission)
        }
    }

    override fun bindAppWidgetId(
        callingPackage: String?,
        appWidgetId: Int,
        providerProfileId: Int,
        providerComponent: ComponentName?,
        options: Bundle?
    ): Boolean {
        return runWithOriginal {
            it.bindAppWidgetId(spoofCallingPackage, appWidgetId, providerProfileId, providerComponent, options)
        }
    }

    override fun bindRemoteViewsService(
        callingPackage: String?,
        appWidgetId: Int,
        intent: Intent?,
        caller: IApplicationThread?,
        token: IBinder?,
        connection: IServiceConnection?,
        flags: Int
    ): Boolean {
        return runWithOriginal {
            it.bindRemoteViewsService(spoofCallingPackage, appWidgetId, intent, caller, token, connection, flags)
        }
    }

    override fun getAppWidgetIds(providerComponent: ComponentName?): IntArray {
        return runWithOriginal {
            it.getAppWidgetIds(providerComponent)
        }
    }

    override fun isBoundWidgetPackage(packageName: String?, userId: Int): Boolean {
        return runWithOriginal {
            it.isBoundWidgetPackage(packageName, userId)
        }
    }

    override fun requestPinAppWidget(
        packageName: String?,
        providerComponent: ComponentName?,
        extras: Bundle?,
        resultIntent: IntentSender?
    ): Boolean {
        return runWithOriginal {
            it.requestPinAppWidget(packageName, providerComponent, extras, resultIntent)
        }
    }

    override fun isRequestPinAppWidgetSupported(): Boolean {
        return runWithOriginal {
            it.isRequestPinAppWidgetSupported
        }
    }

    override fun noteAppWidgetTapped(callingPackage: String?, appWidgetId: Int) {
        return runWithOriginal {
            it.noteAppWidgetTapped(spoofCallingPackage, appWidgetId)
        }
    }

}