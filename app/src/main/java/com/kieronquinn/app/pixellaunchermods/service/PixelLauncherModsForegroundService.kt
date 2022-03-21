package com.kieronquinn.app.pixellaunchermods.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.components.notifications.NotificationChannel
import com.kieronquinn.app.pixellaunchermods.components.notifications.NotificationId
import com.kieronquinn.app.pixellaunchermods.components.notifications.createNotification
import com.kieronquinn.app.pixellaunchermods.repositories.*
import com.kieronquinn.app.pixellaunchermods.repositories.RemoteAppsRepository.IconApplyProgress
import com.kieronquinn.app.pixellaunchermods.repositories.SettingsRepository.DeferredRestartMode
import com.kieronquinn.app.pixellaunchermods.ui.activities.MainActivity
import com.kieronquinn.app.pixellaunchermods.utils.extensions.broadcastReceiverAsFlow
import com.kieronquinn.app.pixellaunchermods.utils.extensions.isDarkMode
import com.kieronquinn.app.pixellaunchermods.utils.extensions.parseToComponentName
import com.kieronquinn.app.pixellaunchermods.widget.ProxyWidget
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import org.koin.android.ext.android.inject
import kotlin.math.roundToInt

class PixelLauncherModsForegroundService: LifecycleService() {

    companion object {
        const val SETTINGS_KEY_ICON_BLACKLIST = "icon_blacklist"
        private const val ICON_DENYLIST_CLOCK = "clock"

        fun start(context: Context, restart: Boolean = false){
            val intent = Intent(context, PixelLauncherModsForegroundService::class.java)
            if(restart) {
                context.stopService(intent)
            }
            context.startForegroundService(intent)
        }

        private fun stop(context: Context, intent: Intent){
            context.stopService(intent)
        }

        /**
         *  The delay time between a package update and applying PLM changes. Allows time for the
         *  Pixel Launcher to update its icons.
         */
        private const val PACKAGE_CHANGED_DELAY = 5000L
    }

    private val remoteAppsRepository by inject<RemoteAppsRepository>()
    private val rootServiceRepository by inject<RootServiceRepository>()
    private val databaseRepository by inject<DatabaseRepository>()
    private val settingsRepository by inject<SettingsRepository>()
    private val appStateRepository by inject<AppStateRepository>()
    private val proxyAppWidgetRepository by inject<ProxyAppWidgetRepository>()

    private val restartMode = settingsRepository.deferredRestartMode.asFlow()
        .stateIn(lifecycleScope, SharingStarted.Eagerly, settingsRepository.deferredRestartMode.getSync())

    private val deferredRestart = MutableStateFlow<DeferredRestart?>(null)

    private val pixelLauncherForeground = remoteAppsRepository.onPixelLauncherForegroundStateChanged
        .stateIn(lifecycleScope, SharingStarted.Eagerly, false)

    private val iconApplyProgress = remoteAppsRepository.iconApplyProgress

    private val screenState by lazy {
        broadcastReceiverAsFlow(Intent.ACTION_SCREEN_OFF, Intent.ACTION_SCREEN_ON).map {
            it.action == Intent.ACTION_SCREEN_ON
        }.stateIn(lifecycleScope, SharingStarted.Eagerly, true)
    }

    private val restartBus by lazy {
        combine(restartMode, deferredRestart, screenState, pixelLauncherForeground) { mode, deferred, screen, foreground ->
            if(deferred == null) return@combine null
            if(appStateRepository.appInForeground.first()){
                //App is in foreground, always restart immediately
                return@combine deferred.isFromRestart
            }
            when(mode){
                DeferredRestartMode.INSTANT -> {
                    //Restart immediately
                    deferred.isFromRestart
                }
                DeferredRestartMode.BACKGROUND -> {
                    //Restart only if Pixel Launcher is in the background
                    if(!foreground){
                        deferred.isFromRestart
                    }else null
                }
                DeferredRestartMode.SCREEN_OFF -> {
                    //Restart only if the screen is off
                    if(!screen){
                        deferred.isFromRestart
                    }else null
                }
            }
        }.filterNotNull()
    }

    private val autoIconPacks = settingsRepository.autoIconPackOrder.asFlow()
        .stateIn(lifecycleScope, SharingStarted.Eagerly, settingsRepository.autoIconPackOrder.getSync())

    private val suppressShortcuts = settingsRepository.suppressShortcutChangeListener.asFlow()
        .stateIn(lifecycleScope, SharingStarted.Eagerly, settingsRepository.suppressShortcutChangeListener.getSync())

    private val modifiedPackages = databaseRepository.getAllModifiedAppsAsFlow().mapLatest {
        it.mapNotNull { app ->
            app.componentName.parseToComponentName()?.packageName
        }
    }.stateIn(lifecycleScope, SharingStarted.Eagerly, emptyList())

    private val nightModeChanged by lazy {
        broadcastReceiverAsFlow(Intent.ACTION_CONFIGURATION_CHANGED).mapLatest {
            isDarkMode
        }.stateIn(lifecycleScope, SharingStarted.Eagerly, isDarkMode)
    }

    private val packageChanged = remoteAppsRepository.onPackageChanged
        .mapNotNull {
            if(it.second && suppressShortcuts.value) return@mapNotNull null
            it.first
        }
        .shareIn(lifecycleScope, SharingStarted.Eagerly)

    private val iconPackChangedListener = packageChanged.filter {
        autoIconPacks.first().contains(it)
    }

    private val packageChangedListener = packageChanged.filter {
        modifiedPackages.first().contains(it)
    }

    private val delayedPackageChangeListener = packageChangedListener.mapLatest {
        delay(PACKAGE_CHANGED_DELAY)
    }

    private val delayedPixelLauncherRestartListener = remoteAppsRepository.onPixelLauncherRestarted.mapLatest {
        delay(PACKAGE_CHANGED_DELAY)
    }

    private val shouldHideClock = combine(
        settingsRepository.hideClock.asFlow(),
        pixelLauncherForeground
    ) { hide, foreground ->
        hide && foreground
    }.stateIn(lifecycleScope, SharingStarted.Eagerly, settingsRepository.hideClock.getSync() && pixelLauncherForeground.value)

    private val proxyAppWidgetListening = combine(
        settingsRepository.qsbWidgetId.asFlow(),
        settingsRepository.qsbWidgetProvider.asFlow(),
        pixelLauncherForeground
    ) { id, provider, foreground ->
        foreground && id != -1 && provider != ""
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = showNotification()
        startForeground(NotificationId.FOREGROUND_SERVICE.ordinal, notification)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()
        setupPackageUpdateListener()
        setupPixelLauncherRestartListener()
        setupIconSizeChangedListener()
        setupIconConfigurationChangedListener()
        setupRestartBus()
        setupIconPackChangeListener()
        setupNightModeChangedListener()
        setupIconApplyProgress()
        setupHideClock()
        //setupQsbWidget()
        setupProxyAppWidget()
    }

    override fun onDestroy() {
        rootServiceRepository.unbindRootServiceIfNeeded()
        super.onDestroy()
    }

    private fun setupPackageUpdateListener() = lifecycleScope.launchWhenCreated {
        delayedPackageChangeListener.collect {
            deferredRestart.emit(false)
        }
    }

    private fun setupIconPackChangeListener() = lifecycleScope.launchWhenCreated {
        iconPackChangedListener.collectLatest {
            onIconPackChanged()
        }
    }

    private fun setupPixelLauncherRestartListener() = lifecycleScope.launchWhenCreated {
        delayedPixelLauncherRestartListener.collect {
            deferredRestart.emit(true)
        }
    }

    private fun setupIconSizeChangedListener() = lifecycleScope.launchWhenCreated {
        remoteAppsRepository.onIconSizeChanged.collectLatest {
            remoteAppsRepository.recreateIcons(false)
        }
    }

    private fun setupIconConfigurationChangedListener() = lifecycleScope.launchWhenCreated {
        remoteAppsRepository.onIconConfigurationChanged.collect {
            deferredRestart.emit(false)
        }
    }

    private fun setupNightModeChangedListener() = lifecycleScope.launchWhenCreated {
        nightModeChanged.drop(1).collectLatest {
            remoteAppsRepository.recreateIcons(true).collect()
        }
    }

    private fun setupRestartBus() = lifecycleScope.launchWhenCreated {
        restartBus.collect {
            remoteAppsRepository.updateModifiedApps(it)
            deferredRestart.emit(null)
        }
    }

    private fun setupIconApplyProgress() = lifecycleScope.launchWhenCreated {
        iconApplyProgress.collect {
            if(it != null){
                showApplyingNotification(it)
            }else{
                clearApplyingNotification()
            }
        }
    }

    private suspend fun onIconPackChanged() {
        remoteAppsRepository.autoApplyIconPacks(autoIconPacks.first(), false).collect()
    }

    private fun setupHideClock() = lifecycleScope.launchWhenCreated {
        shouldHideClock.collect {
            val iconList = getIconDenylist().toMutableList().apply {
                if(it) add(ICON_DENYLIST_CLOCK)
            }.joinToString(",")
            rootServiceRepository.runWithRootService {
                it.setStatusBarIconDenylist(iconList)
            }
        }
    }

    private fun showNotification(): Notification {
        val notificationIntent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            putExtra(Settings.EXTRA_CHANNEL_ID, NotificationChannel.FOREGROUND_SERVICE.id)
        }
        val notification = createNotification(NotificationChannel.FOREGROUND_SERVICE) {
            it.setContentTitle(getString(R.string.notification_title_foreground_service))
            it.setContentText(getString(R.string.notification_content_foreground_service))
            it.setSmallIcon(R.drawable.ic_notification)
            it.setOngoing(true)
            it.setContentIntent(
                PendingIntent.getActivity(
                    this,
                    NotificationId.FOREGROUND_SERVICE.ordinal,
                    notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            it.setTicker(getString(R.string.notification_title_foreground_service))
        }
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NotificationId.FOREGROUND_SERVICE.ordinal, notification)
        return notification
    }

    private fun clearApplyingNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NotificationId.ICON_APPLYING.ordinal)
    }

    private fun showApplyingNotification(iconApplyProgress: IconApplyProgress): Notification {
        val notification = createNotification(NotificationChannel.ICON_APPLYING) {
            val title = iconApplyProgress.titleRes
            val content = iconApplyProgress.contentRes
            val progress = when(iconApplyProgress){
                is IconApplyProgress.ApplyingIcons -> null
                is IconApplyProgress.UpdatingConfiguration -> {
                    iconApplyProgress.progress
                }
                is IconApplyProgress.UpdatingIconPacks -> {
                    iconApplyProgress.progress
                }
            }
            it.setContentTitle(getString(title))
            if(content != null){
                it.setContentText(getString(content))
            }
            it.setSmallIcon(R.drawable.ic_notification)
            it.setOngoing(true)
            if(progress != null){
                it.setProgress(100, (100 * progress).roundToInt(), progress == 0f)
            }else{
                it.setProgress(100, 0, true)
            }
            it.setContentIntent(
                PendingIntent.getActivity(
                    this,
                    NotificationId.ICON_APPLYING.ordinal,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            it.setTicker(getString(title))
        }
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NotificationId.ICON_APPLYING.ordinal, notification)
        return notification
    }

    private data class DeferredRestart(val isFromRestart: Boolean)

    private suspend fun MutableStateFlow<DeferredRestart?>.emit(isFromRestart: Boolean) {
        value?.let {
            if(!it.isFromRestart && isFromRestart) return //isFromRestart = false overrides true
        }
        emit(DeferredRestart(isFromRestart))
    }

    /**
     *  Gets the icon denylist for the statusbar from Settings.Secure. If the clock is already
     *  denied, it will be ignored so it can be toggled.
     */
    private fun getIconDenylist(): List<String> {
        val blacklist = Settings.Secure.getString(contentResolver, SETTINGS_KEY_ICON_BLACKLIST) ?: ""
        val items = if(blacklist.contains(",")){
            blacklist.split(",")
        }else listOf(blacklist)
        return items.filterNot { it == ICON_DENYLIST_CLOCK }
    }

    private fun setupProxyAppWidget() {
        if(settingsRepository.qsbWidgetId.getSync() != -1) {
            ProxyWidget.sendUpdate(this)
        }
        lifecycleScope.launchWhenCreated {
            proxyAppWidgetListening.collect {
                proxyAppWidgetRepository.setListening(it)
            }
        }
    }

}