package com.kieronquinn.app.pixellaunchermods.repositories

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import android.util.SizeF
import androidx.activity.result.ActivityResultLauncher
import com.kieronquinn.app.pixellaunchermods.utils.extensions.dp
import com.kieronquinn.app.pixellaunchermods.utils.extensions.getIntentSenderForConfigureActivity
import com.kieronquinn.app.pixellaunchermods.utils.widget.LauncherProxyAppWidgetHost
import com.kieronquinn.app.pixellaunchermods.utils.widget.PreviewAppWidgetHost
import com.kieronquinn.app.pixellaunchermods.widget.ProxyWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

interface ProxyAppWidgetRepository {

    fun getAppWidgetHostView(
        appWidgetId: Int,
        context: Context,
        appWidgetProviderInfo: AppWidgetProviderInfo,
        options: Bundle
    ): AppWidgetHostView

    fun onAppWidgetHostOptionsChanged(appWidgetId: Int, options: Bundle)
    fun setListening(listening: Boolean)
    fun getHost(): AppWidgetHost
    suspend fun getProviderInfo(provider: String): AppWidgetProviderInfo?
    suspend fun getAllProviders(): List<AppWidgetProviderInfo>
    suspend fun getCurrentProxyWidgetPreviewTop(): AppWidgetHostView?
    suspend fun getCurrentProxyWidgetPreviewBottom(): AppWidgetHostView?
    fun loadWidgetLabel(appWidgetProviderInfo: AppWidgetProviderInfo): CharSequence
    fun loadWidgetDescription(appWidgetProviderInfo: AppWidgetProviderInfo): CharSequence?
    fun startListeningRegular()
    fun stopListeningRegular()

    fun allocateAppWidgetId(provider: AppWidgetProviderInfo, bindLauncher: ActivityResultLauncher<Intent>): Int?
    fun deallocateAppWidgetId(appWidgetId: Int)
    fun getConfigureIntentSenderForProvider(appWidgetId: Int): IntentSender
    suspend fun commitProxyWidgetProvider(context: Context, provider: String, appWidgetId: Int)

}

class ProxyAppWidgetRepositoryImpl(
    private val applicationContext: Context,
    private val settingsRepository: SettingsRepository
): ProxyAppWidgetRepository {

    companion object {
        private const val HOST_ID = 2
    }

    private val packageManager = applicationContext.packageManager
    private val launcherAppWidgetHost = LauncherProxyAppWidgetHost(applicationContext, HOST_ID)
    private val regularHost = PreviewAppWidgetHost(applicationContext, HOST_ID)
    private val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
    private val proxyComponent = ComponentName(applicationContext, ProxyWidget::class.java)

    private var hasAddedView = false

    @Synchronized
    override fun getAppWidgetHostView(
        appWidgetId: Int,
        context: Context,
        appWidgetProviderInfo: AppWidgetProviderInfo,
        options: Bundle
    ): AppWidgetHostView {
        return launcherAppWidgetHost.getView(appWidgetId)?.also {
            it.updateAppWidgetOptionsIfValid(options)
        } ?: run {
            launcherAppWidgetHost.createView(applicationContext, appWidgetId, appWidgetProviderInfo).also {
                it.updateAppWidgetOptionsIfValid(options)
                launcherAppWidgetHost.startListening()
                hasAddedView = true
            }
        }
    }

    override fun onAppWidgetHostOptionsChanged(appWidgetId: Int, options: Bundle) {
        launcherAppWidgetHost.getView(appWidgetId)?.also {
            it.updateAppWidgetOptionsIfValid(options)
        }
    }

    private fun AppWidgetHostView.updateAppWidgetOptionsIfValid(options: Bundle) {
        val sizes = ArrayList<SizeF>().apply {
            val minWidth = options.getIntOrNull(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
                ?: return@apply
            val minHeight = options.getIntOrNull(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
                ?: return@apply
            val maxWidth = options.getIntOrNull(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
                ?: return@apply
            val maxHeight = options.getIntOrNull(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
                ?: return@apply
            add(SizeF(minWidth.dp.toFloat(), minHeight.dp.toFloat()))
            add(SizeF(maxWidth.dp.toFloat(), maxHeight.dp.toFloat()))
        }
        Log.d("PA", "Setting size: $sizes from $options")
        updateAppWidgetSize(Bundle.EMPTY, sizes)
    }

    private fun Bundle.getIntOrNull(key: String): Int? {
        return getInt(key).takeIf { it >= 0 }
    }

    private fun Bundle.copy(to: Bundle, key: String) {
        if(getInt(key) != 0) {
            to.putInt(key, getInt(key))
        }
    }

    override fun setListening(listening: Boolean) {
        if(!hasAddedView) return
        if(listening){
            launcherAppWidgetHost.startListening()
        }else{
            launcherAppWidgetHost.stopListening()
        }
    }

    override suspend fun getProviderInfo(provider: String): AppWidgetProviderInfo? = withContext(Dispatchers.IO) {
        getAllProviders().firstOrNull {
            it.provider.flattenToShortString() == provider
        }
    }

    override suspend fun getAllProviders(): List<AppWidgetProviderInfo> = withContext(Dispatchers.IO) {
        appWidgetManager.installedProviders
    }

    override fun getHost(): AppWidgetHost {
        return regularHost
    }

    private val currentProxyWidgetLock = Mutex()

    override suspend fun getCurrentProxyWidgetPreviewTop(): AppWidgetHostView? = currentProxyWidgetLock.withLock {
        return getCurrentProxyWidget(settingsRepository.proxyWidgetPreviewIdTop)
    }

    override suspend fun getCurrentProxyWidgetPreviewBottom(): AppWidgetHostView? = currentProxyWidgetLock.withLock {
        return getCurrentProxyWidget(settingsRepository.proxyWidgetPreviewIdBottom)
    }

    private suspend fun getCurrentProxyWidget(id: SettingsRepository.PixelLauncherModsSetting<Int>): AppWidgetHostView? {
        val appWidgetId = id.get()
        val provider = appWidgetManager.installedProviders.firstOrNull {
            it.provider == proxyComponent
        } ?: throw Exception("Can't find own proxy component")
        if(appWidgetId != -1) {
            return regularHost.createView(applicationContext, appWidgetId, provider)
        }
        val newId = regularHost.allocateAppWidgetId()
        id.set(newId)
        appWidgetManager.bindAppWidgetIdIfAllowed(newId, provider.provider)
        return regularHost.createView(applicationContext, newId, provider)
    }

    override fun loadWidgetLabel(appWidgetProviderInfo: AppWidgetProviderInfo): CharSequence {
        return appWidgetProviderInfo.loadLabel(packageManager)
    }

    override fun loadWidgetDescription(appWidgetProviderInfo: AppWidgetProviderInfo): CharSequence? {
        return appWidgetProviderInfo.loadDescription(applicationContext)
    }

    override fun startListeningRegular() {
        regularHost.startListening()
    }

    override fun stopListeningRegular() {
        regularHost.stopListening()
    }

    override fun allocateAppWidgetId(
        provider: AppWidgetProviderInfo,
        bindLauncher: ActivityResultLauncher<Intent>
    ): Int? {
        val id = regularHost.allocateAppWidgetId()
        if(!appWidgetManager.bindAppWidgetIdIfAllowed(id, provider.provider)) {
            bindLauncher.launch(Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider.provider)
            })
            return null
        }
        return id
    }

    override fun deallocateAppWidgetId(appWidgetId: Int) {
        regularHost.deleteAppWidgetId(appWidgetId)
    }

    override fun getConfigureIntentSenderForProvider(appWidgetId: Int): IntentSender {
        return regularHost.getIntentSenderForConfigureActivity(appWidgetId, 0)
    }

    override suspend fun commitProxyWidgetProvider(context: Context, provider: String, appWidgetId: Int) {
        val currentId = settingsRepository.qsbWidgetId.get()
        if(currentId != -1 && currentId != appWidgetId){
            //Deallocate the current widget to allow re-use
            deallocateAppWidgetId(currentId)
        }
        settingsRepository.qsbWidgetId.set(appWidgetId)
        settingsRepository.qsbWidgetProvider.set(provider)
        ProxyWidget.sendUpdate(context)
    }

}