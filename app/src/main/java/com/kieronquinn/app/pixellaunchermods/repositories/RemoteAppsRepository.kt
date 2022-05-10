package com.kieronquinn.app.pixellaunchermods.repositories

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.model.editor.AppEditorInfo
import com.kieronquinn.app.pixellaunchermods.model.icon.IconPickerResult
import com.kieronquinn.app.pixellaunchermods.model.ipc.GridSize
import com.kieronquinn.app.pixellaunchermods.model.ipc.ParceledListSlice
import com.kieronquinn.app.pixellaunchermods.model.remote.ModifiedRemoteApp
import com.kieronquinn.app.pixellaunchermods.model.remote.RemoteApp
import com.kieronquinn.app.pixellaunchermods.model.remote.RemoteFavourite
import com.kieronquinn.app.pixellaunchermods.model.remote.RemoteWidget
import com.kieronquinn.app.pixellaunchermods.model.room.IconMetadata
import com.kieronquinn.app.pixellaunchermods.model.room.ModifiedApp
import com.kieronquinn.app.pixellaunchermods.repositories.RemoteAppsRepository.*
import com.kieronquinn.app.pixellaunchermods.service.*
import com.kieronquinn.app.pixellaunchermods.ui.screens.iconpicker.BasePickerViewModel
import com.kieronquinn.app.pixellaunchermods.utils.extensions.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface RemoteAppsRepository {

    sealed class Shortcut(val label: String?) {
        data class AppShortcut(val shortcut: RemoteApp) : Shortcut(shortcut.label)
        data class LegacyShortcut(val shortcut: RemoteFavourite) : Shortcut(shortcut.title)
    }

    sealed class RemoteTarget(
        open val screen: Int,
        open val cellX: Int,
        open val cellY: Int,
        open var spanX: Int,
        open var spanY: Int
    ) {
        data class Shortcut(
            override val screen: Int,
            override val cellX: Int,
            override val cellY: Int,
            val applicationInfo: ApplicationInfo?,
            val label: String?
        ) : RemoteTarget(screen, cellX, cellY, 1, 1)

        data class Widget(
            override val screen: Int,
            override val cellX: Int,
            override val cellY: Int,
            override var spanX: Int,
            override var spanY: Int,
            val provider: String,
            val appWidgetId: Int
        ) : RemoteTarget(screen, cellX, cellY, spanX, spanY)
    }

    sealed class IconApplyProgress(val titleRes: Int, val contentRes: Int? = null) {
        data class UpdatingConfiguration(val progress: Float) :
            IconApplyProgress(R.string.notification_title_updating_icons)

        data class UpdatingIconPacks(val progress: Float, val fromUI: Boolean) :
            IconApplyProgress(
                R.string.notification_title_updating_icon_packs,
                R.string.notification_content_updating_icon_packs
            )

        object ApplyingIcons :
            IconApplyProgress(R.string.notification_title_applying_icons)
    }

    suspend fun getRemoteApps(sendForUpdates: Boolean = true): List<RemoteApp>
    suspend fun getRemoteShortcuts(): List<Shortcut>
    suspend fun getRemoteTargets(): List<RemoteTarget>
    suspend fun getGridSize(): GridSize?
    val onPixelLauncherRestarted: Flow<Unit>
    val onRemoteDatabaseChanged: Flow<Unit>
    val onPixelLauncherForegroundStateChanged: Flow<Boolean>
    val onPackageChanged: Flow<Pair<String, Boolean>>
    val onIconSizeChanged: Flow<Int>
    val onIconConfigurationChanged: Flow<Unit>
    val areNativeThemedIconsSupported: StateFlow<Boolean?>
    val areThemedIconsEnabled: StateFlow<Boolean?>
    val iconSize: StateFlow<Int?>
    val iconApplyProgress: StateFlow<IconApplyProgress?>
    fun recreateIcons(dynamicOnly: Boolean): Flow<Float?>
    suspend fun updateThemedIconsState()
    suspend fun updateModifiedApps(isFromRestart: Boolean = false)
    suspend fun updateShortcut(vararg shortcut: RemoteFavourite)
    suspend fun updateWidgets(widget: List<RemoteWidget>)
    suspend fun resetModifiedApp(componentName: String): RemoteApp?
    suspend fun resetModifiedShortcut(componentName: String): RemoteApp?
    fun autoApplyIconPacks(iconPacks: List<String>, fromUI: Boolean): Flow<Float?>
    fun resetAllApps(): Flow<Boolean>
    suspend fun forceReload()

}

class RemoteAppsRepositoryImpl(
    context: Context,
    private val rootServiceRepository: RootServiceRepository,
    private val databaseRepository: DatabaseRepository,
    private val iconLoaderRepository: IconLoaderRepository,
    private val iconPackRepository: IconPackRepository,
    private val appsRepository: AppsRepository,
    private val settings: SettingsRepository
) : RemoteAppsRepository {

    private val updateThemedIconsStateBus = MutableSharedFlow<Unit>()

    override val onIconConfigurationChanged = MutableSharedFlow<Unit>()
    override val iconApplyProgress = MutableStateFlow<IconApplyProgress?>(null)

    private val remoteDatabaseChanged = callbackFlow {
        val listener = object : IDatabaseChangedListener.Stub() {
            override fun onDatabaseChanged() {
                trySend(Unit)
            }
        }
        rootServiceRepository.runWithRootService { it.setDatabaseChangedListener(listener) }
        awaitClose {
            rootServiceRepository.runWithRootServiceIfAvailable { it.setDatabaseChangedListener(null) }
        }
    }

    private val forceReload = MutableSharedFlow<Unit>()

    override val onRemoteDatabaseChanged =
        instantCombine(remoteDatabaseChanged, forceReload).map { }

    override val onPixelLauncherRestarted = callbackFlow {
        val listener = object : IPixelLauncherRestartListener.Stub() {
            override fun onPixeLauncherRestarted() {
                trySend(Unit)
            }
        }
        rootServiceRepository.runWithRootService { it.setPixelLauncherRestartListener(listener) }
        awaitClose {
            rootServiceRepository.runWithRootServiceIfAvailable {
                it.setPixelLauncherRestartListener(
                    null
                )
            }
        }
    }

    override val onPackageChanged = callbackFlow {
        val listener = object : IPackageChangedListener.Stub() {
            override fun onPackageChanged(packageName: String, isShortcut: Boolean) {
                trySend(Pair(packageName, isShortcut))
            }
        }
        rootServiceRepository.runWithRootService { it.setPackageChangedListener(listener) }
        awaitClose {
            rootServiceRepository.runWithRootServiceIfAvailable { it.setPackageChangedListener(null) }
        }
    }

    override val onIconSizeChanged = callbackFlow {
        val listener = object : IIconSizeChangedListener.Stub() {
            override fun onIconSizeChanged(newSize: Int) {
                trySend(newSize)
            }
        }
        rootServiceRepository.runWithRootService { it.setIconSizeChangedListener(listener) }
        awaitClose {
            rootServiceRepository.runWithRootServiceIfAvailable { it.setIconSizeChangedListener(null) }
        }
    }

    override val onPixelLauncherForegroundStateChanged = callbackFlow {
        val listener = object : IPixelLauncherForegroundListener.Stub() {
            override fun onStateChanged(isForeground: Boolean) {
                trySend(isForeground)
            }
        }
        rootServiceRepository.runWithRootService { it.setPixelLauncherForegroundListener(listener) }
        awaitClose {
            rootServiceRepository.runWithRootServiceIfAvailable {
                it.setPixelLauncherForegroundListener(
                    null
                )
            }
        }
    }

    override fun recreateIcons(dynamicOnly: Boolean) = flow {
        val nativeMonoIcons = areNativeThemedIconsSupported.value ?: run {
            iconApplyProgress.emit(null)
            emit(null)
            return@flow
        }
        val modifiedApps = databaseRepository.getAllModifiedApps().run {
            if (dynamicOnly) {
                filter { it.iconMetadata?.isDynamic() == true }
            } else this
        }
        if (modifiedApps.isEmpty()) {
            iconApplyProgress.emit(null)
            emit(null)
            return@flow
        }
        val remoteApps = getRemoteApps(false)
        val workspace = ArrayList<AppEditorInfo>()
        var progress = 0f
        val chunkedProgress = 1f / modifiedApps.size
        iconApplyProgress.emit(IconApplyProgress.UpdatingConfiguration(progress))
        emit(progress)
        modifiedApps.forEach { modified ->
            val remoteApp = remoteApps.find { it.componentName == modified.componentName }
                ?: return@forEach
            val appEditorInfo = AppEditorInfo.merge(remoteApp, modified, nativeMonoIcons)
            val newIcon = modified.iconMetadata?.toIconPickerResult(
                iconLoaderRepository, appsRepository, modified.staticIcon
            ) ?: return@forEach
            val loaded = iconLoaderRepository.rasterIconPickerResultOptions(
                BasePickerViewModel.IconPickerResultOptions(
                    newIcon,
                    mono = false,
                    loadFullRes = false
                )
            )?.compress() ?: return@forEach
            val loadedFullRes = iconLoaderRepository.rasterIconPickerResultOptions(
                BasePickerViewModel.IconPickerResultOptions(
                    newIcon,
                    mono = false,
                    loadFullRes = true
                )
            )?.compress() ?: return@forEach
            appEditorInfo.updateIcon(loaded, modified.iconMetadata, loadedFullRes)
            workspace.add(appEditorInfo)
            progress += chunkedProgress
            iconApplyProgress.emit(IconApplyProgress.UpdatingConfiguration(progress))
            emit(progress)
        }
        workspace.forEach {
            databaseRepository.saveAppEditorInfo(it)
        }
        iconApplyProgress.emit(null)
        emit(null)
        onIconConfigurationChanged.emit(Unit)
    }.flowOn(Dispatchers.IO)

    override suspend fun getRemoteApps(sendForUpdates: Boolean): List<RemoteApp> =
        withContext(Dispatchers.IO) {
            val launchables = getLaunchables()
            val modifiedApps =
                ParceledListSlice(if (sendForUpdates) getAllModifiedApps() else emptyList())
            rootServiceRepository.runWithRootService { it.loadDatabase(modifiedApps) }?.list?.map {
                RemoteApp(it)
            }?.filter { launchables.contains(it.componentName) }?.sortedBy { it.label.lowercase() }
                ?: emptyList()
        }

    override suspend fun getRemoteShortcuts(): List<Shortcut> = withContext(Dispatchers.IO) {
        val remoteApps =
            rootServiceRepository.runWithRootService { it.loadDatabase(ParceledListSlice.emptyList()) }?.list?.map {
                RemoteApp(it)
            } ?: return@withContext emptyList<Shortcut>()
        val favourites =
            rootServiceRepository.runWithRootService { it.loadFavourites(true) }?.list?.map {
                RemoteFavourite(it)
            } ?: return@withContext emptyList<Shortcut>()
        favourites.mapNotNull {
            when (it.type) {
                RemoteFavourite.Type.WIDGET -> null //Not required
                RemoteFavourite.Type.APP, RemoteFavourite.Type.APP_SHORTCUT -> {
                    it.toAppShortcutOrNull(remoteApps)
                }
                RemoteFavourite.Type.SHORTCUT -> Shortcut.LegacyShortcut(it)
            }
        }
    }

    private fun RemoteFavourite.toAppShortcutOrNull(remoteApps: List<RemoteApp>): Shortcut.AppShortcut? {
        return Shortcut.AppShortcut(remoteApps.findFavouriteOrNull(this) ?: return null)
    }

    /**
     *  Attempts to link a RemoteFavourite to a RemoteApp from the app icons database.
     *
     *  - Intents with a `shortcut_id` extra are linked to a RemoteApp (App Shortcuts)
     *  - Intents with a `component` are linked to a RemoteApp (Normal app)
     *  - Other shortcuts are handled elsewhere, as they are edited in the `launcher.db` database.
     */
    private fun List<RemoteApp>.findFavouriteOrNull(favourite: RemoteFavourite): RemoteApp? {
        val intent = Intent.parseUri(favourite.intent ?: return null, 0)
        val shortcutId = intent.getStringExtra("shortcut_id")
        if (shortcutId != null) {
            val packageName = intent.`package` ?: return null
            val component = ComponentName(packageName, shortcutId).flattenToString()
            firstOrNull { it.componentName == component }?.also {
                //Update type of this RemoteApp to APP_SHORTCUT, which disables label editing
                it.type = RemoteApp.Type.APP_SHORTCUT
            }?.let {
                return it
            }
        }
        val component = intent.component?.unshortenToString() ?: return null
        return firstOrNull { it.componentName == component }
    }

    override val areNativeThemedIconsSupported = flow {
        emit(rootServiceRepository.runWithRootService { it.areNativeThemedIconsSupported() })
    }.flowOn(Dispatchers.IO).stateIn(GlobalScope, SharingStarted.Eagerly, null)

    override val areThemedIconsEnabled = updateThemedIconsStateBus.map {
        rootServiceRepository.runWithRootService {
            it.areThemedIconsEnabled()
        }
    }.flowOn(Dispatchers.IO).stateIn(GlobalScope, SharingStarted.Eagerly, null)

    override val iconSize = flow {
        emit(rootServiceRepository.runWithRootService { it.remoteIconSize })
    }.stateIn(GlobalScope, SharingStarted.Eagerly, null)

    override suspend fun updateThemedIconsState() {
        updateThemedIconsStateBus.emit(Unit)
    }

    override suspend fun updateModifiedApps(isFromRestart: Boolean) {
        withContext(Dispatchers.IO) {
            iconApplyProgress.emit(IconApplyProgress.ApplyingIcons)
            val cachedIconSize = settings.iconSize.get()
            val modifiedApps = getAllModifiedApps()
            rootServiceRepository.runWithRootService {
                var iconSize = it.remoteIconSize
                if (iconSize == -1) {
                    //Not yet set, try to use the cache if possible
                    if(!settings.iconSize.exists()) {
                        //We won't be able to apply as we don't have a size, abort until next time
                        return@runWithRootService
                    }
                    iconSize = cachedIconSize
                }
                settings.iconSize.set(iconSize)
                it.updateModifiedApps(
                    ParceledListSlice(modifiedApps),
                    isFromRestart,
                    iconSize
                )
            }
            iconApplyProgress.emit(null)
        }
    }

    override suspend fun updateShortcut(vararg shortcut: RemoteFavourite) {
        withContext(Dispatchers.IO) {
            rootServiceRepository.runWithRootService {
                val shortcuts = ParceledListSlice(shortcut.map { s -> s.toBundle() })
                it.updateFavourites(shortcuts)
            }
        }
    }

    override suspend fun updateWidgets(widgets: List<RemoteWidget>) {
        withContext(Dispatchers.IO) {
            rootServiceRepository.runWithRootService {
                it.updateWidgets(ParceledListSlice(widgets.map { it.toBundle() }))
            }
        }
    }

    override suspend fun resetModifiedApp(componentName: String): RemoteApp? {
        //Clear remote entry and restart Pixel Launcher
        rootServiceRepository.runWithRootService {
            it.deleteEntryAndRestart(componentName)
        } ?: return null
        return getRemoteApps(false).firstOrNull {
            it.componentName == componentName
        }
    }

    override suspend fun resetModifiedShortcut(componentName: String): RemoteApp? {
        //Clear remote entry and restart Pixel Launcher
        rootServiceRepository.runWithRootService {
            it.deleteEntryAndRestart(componentName)
        } ?: return null
        return (getRemoteShortcuts().firstOrNull {
            it is Shortcut.AppShortcut && it.shortcut.componentName == componentName
        } as Shortcut.AppShortcut).shortcut
    }

    override suspend fun getRemoteTargets(): List<RemoteTarget> = withContext(Dispatchers.IO) {
        val favouritesList = rootServiceRepository.runWithRootService {
            it.loadFavourites(false)
        } ?: return@withContext emptyList()
        val favourites = favouritesList.list.map { RemoteFavourite(it) }
        favourites.mapNotNull {
            when(it.type) {
                RemoteFavourite.Type.APP_SHORTCUT, RemoteFavourite.Type.SHORTCUT, RemoteFavourite.Type.APP -> {
                    val intent = Intent.parseUri(it.intent ?: return@mapNotNull null, 0)
                    val applicationInfo =
                        packageManager.resolveActivity(intent, 0)?.activityInfo?.applicationInfo ?: run {
                            packageManager.getApplicationInfoOrNull(intent.`package` ?: return@run null)
                        }
                    RemoteTarget.Shortcut(it.screen, it.cellX, it.cellY, applicationInfo, it.title)
                }
                RemoteFavourite.Type.WIDGET -> {
                    val provider = it.appWidgetProvider ?: return@mapNotNull null
                    RemoteTarget.Widget(it.screen, it.cellX, it.cellY, it.spanX, it.spanY, provider, it.appWidgetId)
                }
            }
        }
    }

    override suspend fun getGridSize(): GridSize? = withContext(Dispatchers.IO) {
        rootServiceRepository.runWithRootService {
            it.gridSize
        }
    }

    override fun resetAllApps() = callbackFlow {
        val callback = object: IResetCompleteCallback.Stub() {
            override fun onResetComplete() {
                trySend(true)
            }
        }
        databaseRepository.clearAllIcons()
        settings.resetIcons()
        rootServiceRepository.runWithRootService {
            it.uninstallOverlayUpdates()
            it.resetAllIcons(callback)
        } ?: trySend(false)
        awaitClose {
            //No-op
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun forceReload() {
        forceReload.emit(Unit)
    }

    override fun autoApplyIconPacks(iconPacks: List<String>, fromUI: Boolean) = flow {
        val allIconPacks = iconPackRepository.getAllIconPacks()
        val nativeMonoIcons = areNativeThemedIconsSupported.value ?: run {
            iconApplyProgress.emit(null)
            emit(null)
            return@flow
        }
        //Select only the installed packs so we don't waste time trying to create invalid packages
        val installedIconPacks = iconPacks.filter { packageName ->
            allIconPacks.firstOrNull { it.packageName == packageName } != null
        }
        val modifiedApps = databaseRepository.getAllModifiedApps()
        val remoteApps = getRemoteApps(false)
        val workspace = ArrayList<AppEditorInfo>()
        val chunkedProgress = 1f / installedIconPacks.size
        var overallProgress = 0f
        iconApplyProgress.emit(IconApplyProgress.UpdatingIconPacks(overallProgress, fromUI))
        emit(overallProgress)
        installedIconPacks.forEach { packageName ->
            if (remoteApps.isEmpty()) return@forEach //No icons left to theme
            val icons = iconPackRepository.getAllComponentIcons(packageName)
            if (icons.isEmpty()) return@forEach //No icons in pack
            updateIconsForIconPackage(
                remoteApps, modifiedApps, workspace, icons, nativeMonoIcons
            ).collectUntilNull {
                val progress = overallProgress + (it * chunkedProgress)
                iconApplyProgress.emit(IconApplyProgress.UpdatingIconPacks(progress, fromUI))
                emit(progress)
            }
            overallProgress += chunkedProgress
            iconApplyProgress.emit(IconApplyProgress.UpdatingIconPacks(overallProgress, fromUI))
            emit(overallProgress)
        }
        workspace.forEach {
            databaseRepository.saveAppEditorInfo(it)
        }
        if (fromUI) {
            updateModifiedApps()
        } else {
            onIconConfigurationChanged.emit(Unit)
        }
        iconApplyProgress.emit(null)
        emit(null)
    }.flowOn(Dispatchers.IO)

    private suspend fun updateIconsForIconPackage(
        remoteApps: List<RemoteApp>,
        modifiedApps: List<ModifiedApp>,
        workspace: ArrayList<AppEditorInfo>,
        icons: List<IconPackRepository.IconPackComponentIcon>,
        nativeMonoIcons: Boolean
    ) = flow {
        var progress = 0f
        val chunkedProgress = 1f / remoteApps.size
        remoteApps.forEach { remote ->
            val modifiedApp = modifiedApps.firstOrNull { it.componentName == remote.componentName }
            if (modifiedApp?.iconMetadata?.applyType == IconMetadata.ApplyType.MANUAL) {
                //Skip as icon was manually set
                progress += chunkedProgress
                emit(progress)
                return@forEach
            }
            if (remote.type != RemoteApp.Type.NORMAL) {
                //Don't theme dynamic icons
                progress += chunkedProgress
                emit(progress)
                return@forEach
            }
            if (workspace.firstOrNull { it.componentName == remote.componentName } != null) {
                //Already got an icon set
                progress += chunkedProgress
                emit(progress)
                return@forEach
            }
            val appEditorInfo = getAppEditorInfoForPackage(
                icons, remote, modifiedApp, nativeMonoIcons
            )
            workspace.add(appEditorInfo ?: run {
                progress += chunkedProgress
                emit(progress)
                return@forEach
            })
            progress += chunkedProgress
            emit(progress)
        }
        emit(null)
    }

    private suspend fun getAppEditorInfoForPackage(
        icons: List<IconPackRepository.IconPackComponentIcon>,
        remote: RemoteApp,
        modifiedApp: ModifiedApp?,
        nativeMonoIcons: Boolean
    ): AppEditorInfo? {
        val iconPackIcon = icons.firstOrNull { it.componentName == remote.componentName }
            ?: return null
        val iconPickerResultIcon = IconPickerResult.IconPackIcon(
            iconPackIcon.iconPackPackageName,
            iconPackIcon.resourceName,
            iconPackIcon.isAdaptiveIcon
        )
        val appEditorInfo = AppEditorInfo.merge(remote, modifiedApp, nativeMonoIcons)
        appEditorInfo.updateIcon(
            iconPickerResultIcon.toByteArray(iconLoaderRepository),
            iconPickerResultIcon.toModifiedAppMetadata(IconMetadata.ApplyType.AUTOMATIC),
            iconPickerResultIcon.toByteArray(iconLoaderRepository, true)
        )
        return appEditorInfo
    }

    private suspend fun getAllModifiedApps(): List<Bundle> = withContext(Dispatchers.IO) {
        val modifiedApps = databaseRepository.getAllModifiedApps()
        if (modifiedApps.isEmpty()) return@withContext emptyList<Bundle>()
        val remoteIconSize = iconSize.filterNotNull().first()
        val nativeMonoIcons = areNativeThemedIconsSupported.value
            ?: return@withContext emptyList<Bundle>()
        modifiedApps.map {
            it.convertToModifiedRemoteApp(remoteIconSize, nativeMonoIcons).toBundle()
        }
    }

    private fun ModifiedApp.convertToModifiedRemoteApp(
        remoteIconSize: Int,
        nativeMonoIcons: Boolean
    ): ModifiedRemoteApp {
        val icon = if (nativeMonoIcons) {
            icon?.let { iconLoaderRepository.resizeIcon(it, remoteIconSize) }
        } else {
            legacyIcon?.let { iconLoaderRepository.resizeLegacyIcon(it, remoteIconSize) }
        }
        val monoIcon = if (nativeMonoIcons) {
            monoIcon?.let {
                iconLoaderRepository
                    .resizeThemedIcon(it, remoteIconSize, remoteIconSize)
            }
        } else null
        return ModifiedRemoteApp(
            componentName, icon, monoIcon, iconColor, label, iconType
        )
    }

    private val packageManager = context.packageManager

    /**
     *  Gets a list of the launcher activities for all apps on the device, as flattened [ComponentName]s.
     */
    private fun getLaunchables(): List<String> {
        return packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }, 0).map {
            ComponentName(it.activityInfo.applicationInfo.packageName, it.activityInfo.name)
                .flattenToString()
        }
    }

    init {
        GlobalScope.launch {
            updateModifiedApps()
        }
    }

}