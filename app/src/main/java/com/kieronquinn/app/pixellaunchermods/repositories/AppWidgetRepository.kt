package com.kieronquinn.app.pixellaunchermods.repositories

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.android.internal.appwidget.IAppWidgetService
import com.kieronquinn.app.pixellaunchermods.utils.widget.ProxyAppWidgetHost
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface AppWidgetRepository {

    suspend fun createView(context: Context, appWidgetId: Int, provider: String): AppWidgetHostView
    suspend fun startListening()
    suspend fun stopListening()

}

class AppWidgetRepositoryImpl(
    private val applicationContext: Context,
    private val rootServiceRepository: RootServiceRepository
): AppWidgetRepository {

    companion object {
        private const val HOST_ID = 3
    }

    private val appWidgetService = AppWidgetManager.getInstance(applicationContext)

    private val appWidgetHost = ProxyAppWidgetHost(
        applicationContext,
        HOST_ID
    )

    private var hasInitHost = false
    private val initLock = Mutex()

    private suspend fun initHostIfNeeded() = initLock.withLock {
        if(hasInitHost) return@withLock
        val service = rootServiceRepository.runWithRootService { it.proxyAppWidgetService }
            ?: return@withLock
        appWidgetHost.setService(IAppWidgetService.Stub.asInterface(service.proxy))
        hasInitHost = true
    }

    override suspend fun createView(context: Context, appWidgetId: Int, provider: String): AppWidgetHostView {
        initHostIfNeeded()
        val providerInfo = appWidgetService.installedProviders.firstOrNull {
            it.activityInfo.run {
                ComponentName(packageName, name)
            } == ComponentName.unflattenFromString(provider)
        }
        return appWidgetHost.createView(applicationContext, appWidgetId, providerInfo)
    }

    override suspend fun startListening() {
        initHostIfNeeded()
        appWidgetHost.startListening()
    }

    override suspend fun stopListening() {
        initHostIfNeeded()
        appWidgetHost.stopListening()
    }

}