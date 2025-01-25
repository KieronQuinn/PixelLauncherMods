package com.kieronquinn.app.pixellaunchermods.service

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.app.IActivityManager
import android.app.IProcessObserver
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.om.IOverlayManager
import android.content.om.OverlayManagerTransaction
import android.content.pm.ILauncherApps
import android.content.pm.IOnAppsChangedListener
import android.os.*
import android.system.Os
import com.android.internal.appwidget.IAppWidgetService
import com.kieronquinn.app.pixellaunchermods.BuildConfig
import com.kieronquinn.app.pixellaunchermods.OVERLAY_PACKAGE_NAME
import com.kieronquinn.app.pixellaunchermods.PIXEL_LAUNCHER_PACKAGE_NAME
import com.kieronquinn.app.pixellaunchermods.model.ipc.GridSize
import com.kieronquinn.app.pixellaunchermods.model.ipc.ParceledListSlice
import com.kieronquinn.app.pixellaunchermods.model.ipc.ProxyAppWidgetServiceContainer
import com.kieronquinn.app.pixellaunchermods.model.remote.ModifiedRemoteApp
import com.kieronquinn.app.pixellaunchermods.model.remote.RemoteApp
import com.kieronquinn.app.pixellaunchermods.model.remote.RemoteFavourite
import com.kieronquinn.app.pixellaunchermods.model.remote.RemoteWidget
import com.kieronquinn.app.pixellaunchermods.repositories.HideClockRepositoryImpl.Companion.SETTINGS_KEY_ICON_BLACKLIST
import com.kieronquinn.app.pixellaunchermods.utils.extensions.*
import com.kieronquinn.app.pixellaunchermods.utils.widget.ProxyAppWidgetService
import com.topjohnwu.superuser.internal.Utils
import com.topjohnwu.superuser.ipc.RootService
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import org.sqlite.database.sqlite.SQLiteDatabase
import rikka.shizuku.SystemServiceHelper
import java.io.File
import kotlin.reflect.KMutableProperty0

class PixelLauncherModsRootService: RootService() {

    override fun onBind(intent: Intent): IBinder {
        return PixelLauncherModsRootServiceImpl()
    }

}

@SuppressLint("RestrictedApi")
class PixelLauncherModsRootServiceImpl: IPixelLauncherModsRootService.Stub() {

    companion object {
        private const val APP_ICONS_DATABASE = "app_icons.db"
        private const val PIXEL_LAUNCHER_SHARED_PREFS_NAME = "com.android.launcher3.prefs.xml"
        private const val SHARED_PREFS_KEY_THEMED_ICONS = "themed_icons"
        private const val SHARED_PREFS_KEY_GRID_SIZE = "migration_src_workspace_size"

        private const val PLM_DATABASE_TABLE_ICONS = "icons"
        private const val PLM_DATABASE_COLUMN_COMPONENT = "componentName"
        private const val PLM_DATABASE_COLUMN_LABEL = "label"
        private const val PLM_DATABASE_COLUMN_ICON = "icon"
        private const val PLM_DATABASE_COLUMN_MONO_ICON = "mono_icon"
        private const val PLM_DATABASE_COLUMN_ICON_COLOR = "icon_color"

        private const val PLM_FAVOURITES_TABLE_FAVOURITES = "favorites"
        private const val PLM_FAVOURITES_COLUMN_TITLE = "title"
        private const val PLM_FAVOURITES_COLUMN_INTENT = "intent"
        private const val PLM_FAVOURITES_COLUMN_SCREEN = "screen"
        private const val PLM_FAVOURITES_COLUMN_CELL_X = "cellX"
        private const val PLM_FAVOURITES_COLUMN_CELL_Y = "cellY"
        private const val PLM_FAVOURITES_COLUMN_SPAN_X = "spanX"
        private const val PLM_FAVOURITES_COLUMN_SPAN_Y = "spanY"
        private const val PLM_FAVOURITES_COLUMN_APP_WIDGET_PROVIDER = "appWidgetProvider"
        private const val PLM_FAVOURITES_COLUMN_APP_WIDGET_ID = "appWidgetId"
        private const val PLM_FAVOURITES_COLUMN_ICON = "icon"
        private const val PLM_FAVOURITES_COLUMN_ITEM_TYPE = "itemType"
        private const val PLM_FAVOURITES_COLUMN_CONTAINER = "container"

        private const val PLM_FAVOURITES_CONTAINER_DESKTOP = -100
        private const val PLM_FAVOURITES_CONTAINER_HOTSEAT = -101
        private const val PLM_FAVOURITES_CONTAINER_PREDICTION = -102
        private const val PLM_FAVOURITES_CONTAINER_WIDGETS_PREDICTION = -111
        private const val PLM_FAVOURITES_CONTAINER_HOTSEAT_PREDICTION = -103
        private const val PLM_FAVOURITES_CONTAINER_ALL_APPS = -104
        private const val PLM_FAVOURITES_CONTAINER_WIDGETS_TRAY = -105
        private const val PLM_FAVOURITES_CONTAINER_BOTTOM_WIDGETS_TRAY = -112
        private const val PLM_FAVOURITES_CONTAINER_PIN_WIDGETS = -113
        private const val PLM_FAVOURITES_CONTAINER_WALLPAPERS = -114
        private const val PLM_FAVOURITES_CONTAINER_SEARCH_RESULTS = -106
        private const val PLM_FAVOURITES_CONTAINER_SHORTCUTS = -107
        private const val PLM_FAVOURITES_CONTAINER_SETTINGS = -108
        private const val PLM_FAVOURITES_CONTAINER_TASKSWITCHER = -109
        private const val PLM_FAVOURITES_CONTAINER_QSB = -110

        private val IGNORED_CONTAINERS = arrayOf(
            PLM_FAVOURITES_CONTAINER_PREDICTION,
            PLM_FAVOURITES_CONTAINER_WIDGETS_PREDICTION,
            PLM_FAVOURITES_CONTAINER_HOTSEAT_PREDICTION,
            PLM_FAVOURITES_CONTAINER_ALL_APPS,
            PLM_FAVOURITES_CONTAINER_WIDGETS_TRAY,
            PLM_FAVOURITES_CONTAINER_BOTTOM_WIDGETS_TRAY,
            PLM_FAVOURITES_CONTAINER_PIN_WIDGETS,
            PLM_FAVOURITES_CONTAINER_WALLPAPERS,
            PLM_FAVOURITES_CONTAINER_SEARCH_RESULTS,
            PLM_FAVOURITES_CONTAINER_SHORTCUTS,
            PLM_FAVOURITES_CONTAINER_SETTINGS,
            PLM_FAVOURITES_CONTAINER_TASKSWITCHER,
            PLM_FAVOURITES_CONTAINER_QSB
        )

        private const val SEARCH_PROVIDER_SETTINGS_KEY = "SEARCH_PROVIDER_PACKAGE_NAME"
        private const val SEARCH_PROVIDER_SETTINGS_KEY_VARIANT = "selected_search_engine"
    }

    private val context by lazy {
        Utils.getContext()
    }

    private val pixelLauncherUid by lazy {
        context.packageManager.getPackageUid(PIXEL_LAUNCHER_PACKAGE_NAME, 0)
    }

    private val pixelLauncherContext by lazy {
        context.createPackageContext(PIXEL_LAUNCHER_PACKAGE_NAME, Context.CONTEXT_IGNORE_SECURITY)
    }

    private val pixelLauncherDatabase by lazy {
        pixelLauncherContext.getDatabasePath(APP_ICONS_DATABASE)
    }

    private val pixelLauncherLaunchIntent by lazy {
        Intent(Intent.ACTION_MAIN).apply {
            `package` = PIXEL_LAUNCHER_PACKAGE_NAME
            component = ComponentName(
                PIXEL_LAUNCHER_PACKAGE_NAME,
                "com.google.android.apps.nexuslauncher.NexusLauncherActivity"
            )
            addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        }
    }

    private val pixelLauncherModsContext by lazy {
        context.createPackageContext(BuildConfig.APPLICATION_ID, Context.CONTEXT_IGNORE_SECURITY)
    }

    private val pixelLauncherSharedPrefsFile by lazy {
        val sharedPrefsDir = File(pixelLauncherContext.filesDir.parentFile, "shared_prefs")
        File(sharedPrefsDir, PIXEL_LAUNCHER_SHARED_PREFS_NAME)
    }

    private val currentUser by lazy {
        Process.myUserHandle()
    }

    private val currentUserId by lazy {
        UserHandle::class.java.getMethod("getIdentifier").invoke(currentUser) as Int
    }

    private val activityManager by lazy {
        val proxy = SystemServiceHelper.getSystemService("activity")
        IActivityManager.Stub.asInterface(proxy)
    }

    private val launcherApps by lazy {
        val proxy = SystemServiceHelper.getSystemService("launcherapps")
        ILauncherApps.Stub.asInterface(proxy)
    }

    private val appWidgetService by lazy {
        val proxy = SystemServiceHelper.getSystemService("appwidget")
        IAppWidgetService.Stub.asInterface(proxy)
    }

    private val calendarComponentName by lazy {
        val pixelLauncherResources = pixelLauncherContext.resources
        pixelLauncherResources.getIdentifier("calendar_component_name", "string", PIXEL_LAUNCHER_PACKAGE_NAME).run {
            if(this == 0) null
            else pixelLauncherResources.getString(this)
        }
    }

    private val clockComponentName by lazy {
        val pixelLauncherResources = pixelLauncherContext.resources
        pixelLauncherResources.getIdentifier("clock_component_name", "string", PIXEL_LAUNCHER_PACKAGE_NAME).run {
            if(this == 0) null
            else pixelLauncherResources.getString(this)
        }
    }

    private val onAppsChangedListener = object: IOnAppsChangedListener.Stub() {
        override fun onPackageRemoved(user: UserHandle?, packageName: String?) {
            if(currentUser != user) return
            notifyPackageChange(packageName ?: return)
        }

        override fun onPackageAdded(user: UserHandle?, packageName: String?) {
            if(currentUser != user) return
            notifyPackageChange(packageName ?: return)
        }

        override fun onPackageChanged(user: UserHandle?, packageName: String?) {
            if(currentUser != user) return
            notifyPackageChange(packageName ?: return)
        }

        override fun onPackagesAvailable(
            user: UserHandle?,
            packageNames: Array<out String>?,
            replacing: Boolean
        ) {
            if(currentUser != user) return
            packageNames?.forEach {
                notifyPackageChange(it)
            }
        }

        override fun onPackagesUnavailable(
            user: UserHandle?,
            packageNames: Array<out String>?,
            replacing: Boolean
        ) {
            if(currentUser != user) return
            packageNames?.forEach {
                notifyPackageChange(it)
            }
        }

        override fun onPackagesSuspended(
            user: UserHandle?,
            packageNames: Array<out String>?,
            launcherExtras: Bundle?
        ) {
            if(currentUser != user) return
            packageNames?.forEach {
                notifyPackageChange(it)
            }
        }

        override fun onPackagesUnsuspended(user: UserHandle?, packageNames: Array<out String>?) {
            if(currentUser != user) return
            packageNames?.forEach {
                notifyPackageChange(it)
            }
        }

        override fun onShortcutChanged(
            user: UserHandle?,
            packageName: String?,
            shortcuts: android.content.pm.ParceledListSlice<*>?
        ) {
            if(currentUser != user) return
            notifyPackageChange(packageName ?: return, true)
        }

        override fun onPackageLoadingProgressChanged(
            user: UserHandle?,
            packageName: String?,
            progress: Float
        ) {
            if(currentUser != user) return
            notifyPackageChange(packageName ?: return)
        }

    }

    /**
     *  Loads the SQLite native libraries from the clone library, as we can't use the system
     *  classes due to Provider issues.
     */
    @SuppressLint("UnsafeDynamicallyLoadedCode")
    private fun loadNativeLibraries(){
        val applicationInfo = pixelLauncherModsContext.applicationInfo
        val nativeLibraryDir = applicationInfo.nativeLibraryDir
        loadLibrary(File(nativeLibraryDir, "libsqliteX.so").absolutePath, this::class.java.classLoader!!)
        loadLibrary(File(nativeLibraryDir, "libsqlitefunctions.so").absolutePath, this::class.java.classLoader!!)
    }

    private var suppressChangeListeners = false
    private var isPixelLauncherInForeground = false
    private var servicePixelLauncherRestartListener: IPixelLauncherRestartListener? = null
    private var servicePixelLauncherForegroundListener: IPixelLauncherForegroundListener? = null
    private var serviceDatabaseChangedListener: IDatabaseChangedListener? = null
    private var servicePackageChangedListener: IPackageChangedListener? = null
    private var serviceIconSizeChangedListener: IIconSizeChangedListener? = null

    private val databaseChangeFlow = callbackFlow {
        val observer = object: FileObserver(pixelLauncherDatabase, MODIFY) {
            override fun onEvent(event: Int, path: String?) {
                trySend(event)
            }
        }
        observer.startWatching()
        awaitClose {
            observer.stopWatching()
        }
    }

    private val processListener = object: IProcessObserver.Stub() {
        override fun onForegroundActivitiesChanged(
            pid: Int,
            uid: Int,
            foregroundActivities: Boolean
        ) {
            if(uid == pixelLauncherUid){
                servicePixelLauncherForegroundListener?.onStateChanged(foregroundActivities)
                isPixelLauncherInForeground = foregroundActivities
            }
        }

        override fun onForegroundServicesChanged(pid: Int, uid: Int, serviceTypes: Int) {
            //No-op
        }

        override fun onProcessDied(pid: Int, uid: Int) {
            if(uid == pixelLauncherUid){
                notifyPixelLauncherRestart()
            }
        }
    }

    override fun areThemedIconsEnabled(): Boolean {
        val pixelLauncherSharedPrefs = SharedPreferences_openFile(pixelLauncherSharedPrefsFile)
        return pixelLauncherSharedPrefs.getBoolean(SHARED_PREFS_KEY_THEMED_ICONS, false)
    }

    override fun loadDatabase(modifiedApps: ParceledListSlice<Bundle>): ParceledListSlice<Bundle> {
        suppressChangeListeners = true
        val apps = modifiedApps.list.map {
            ModifiedRemoteApp(it)
        }
        val database = getIconsDatabase()
        database.performUpdateAndRestartIfRequired(apps)
        val icons = database.getIcons().asBundle()
        database.close()
        suppressChangeListeners = false
        return ParceledListSlice(icons)
    }

    override fun updateModifiedApps(modifiedApps: ParceledListSlice<Bundle>, skipRestart: Boolean, iconSize: Int) {
        val apps = modifiedApps.list.map {
            ModifiedRemoteApp(it)
        }
        val remoteIconSize = getIconSize() ?: return
        if(remoteIconSize != iconSize){
            runWithInterface(::serviceIconSizeChangedListener){
                it.onIconSizeChanged(remoteIconSize)
            }
            //This will be re-called when the icons are sized correctly, so return for now
            return
        }
        val database = getIconsDatabase()
        if(skipRestart){
            database.performUpdateIfRequired(apps)
        }else{
            database.performUpdateAndRestartIfRequired(apps)
        }
        database.close()
    }

    override fun setPixelLauncherRestartListener(listener: IPixelLauncherRestartListener?) {
        servicePixelLauncherRestartListener = listener
    }

    override fun setDatabaseChangedListener(listener: IDatabaseChangedListener?) {
        serviceDatabaseChangedListener = listener
    }

    override fun setPackageChangedListener(listener: IPackageChangedListener?) {
        servicePackageChangedListener = listener
    }

    override fun setIconSizeChangedListener(listener: IIconSizeChangedListener?) {
        serviceIconSizeChangedListener = listener
    }

    override fun setPixelLauncherForegroundListener(listener: IPixelLauncherForegroundListener?) {
        servicePixelLauncherForegroundListener = listener
    }

    override fun areNativeThemedIconsSupported(): Boolean {
        val database = getIconsDatabase()
        val cursor = database.rawQuery(
            "SELECT COUNT(*) AS 'count' FROM pragma_table_info('icons') WHERE name='mono_icon'",
            null
        )
        cursor.moveToFirst()
        return (cursor.getInt(0) == 1).also {
            cursor.close()
            database.close()
        }
    }

    private fun SQLiteDatabase.performUpdateAndRestartIfRequired(modifiedApps: List<ModifiedRemoteApp>) {
        if(performUpdateIfRequired(modifiedApps)){
            restartPixelLauncher()
        }
    }

    private fun SQLiteDatabase.performUpdateIfRequired(modifiedApps: List<ModifiedRemoteApp>): Boolean {
        val currentIcons = getIcons()
        val updateRequired = modifiedApps.any { app ->
            //Match to a current icon, or ignore (false)
            val currentApp = currentIcons.firstOrNull { it.componentName == app.componentName }
                ?: return@any false
            //Compare the current icon to the custom icon, returning if there is an update required
            !currentApp.compareTo(app)
        }
        //No point hitting the database if no updates are required
        if(!updateRequired) return false
        modifiedApps.forEach { modifiedApp ->
            update(
                PLM_DATABASE_TABLE_ICONS,
                ContentValues().apply {
                    if(modifiedApp.label != null)
                        put(PLM_DATABASE_COLUMN_LABEL, modifiedApp.label)
                    if(modifiedApp.icon != null)
                        put(PLM_DATABASE_COLUMN_ICON, modifiedApp.icon)
                    if(modifiedApp.monoIcon != null)
                        put(PLM_DATABASE_COLUMN_MONO_ICON, modifiedApp.monoIcon)
                    if(modifiedApp.iconColor != null)
                        put(PLM_DATABASE_COLUMN_ICON_COLOR, modifiedApp.iconColor)
                },
                "$PLM_DATABASE_COLUMN_COMPONENT=?",
                arrayOf(modifiedApp.componentName)
            )
        }
        notifyDatabaseChange()
        return true
    }

    private fun notifyDatabaseChange() {
        if(suppressChangeListeners) return
        runWithInterface(::serviceDatabaseChangedListener){
            it.onDatabaseChanged()
        }
    }

    private fun notifyPixelLauncherRestart() {
        runWithInterface(::servicePixelLauncherRestartListener){
            it.onPixeLauncherRestarted()
        }
    }

    private fun notifyPackageChange(packageName: String, isShortcut: Boolean = false) {
        runWithInterface(::servicePackageChangedListener) {
            it.onPackageChanged(packageName, isShortcut)
        }
    }

    private fun setupPixelLauncherRestartListener() {
        activityManager.registerProcessObserver(processListener)
    }

    private fun setupLauncherAppsListener() {
        val callingIdentity = Binder.clearCallingIdentity()
        launcherApps.addOnAppsChangedListener(PIXEL_LAUNCHER_PACKAGE_NAME, onAppsChangedListener)
        Binder.restoreCallingIdentity(callingIdentity)
    }

    override fun getRemoteIconSize(): Int {
        return getIconSize() ?: -1
    }

    override fun getGridSize(): GridSize? {
        val pixelLauncherSharedPrefs = SharedPreferences_openFile(pixelLauncherSharedPrefsFile)
        val gridSize = pixelLauncherSharedPrefs.getString(SHARED_PREFS_KEY_GRID_SIZE, null)
            ?: return null
        if(!gridSize.contains(",")) return null
        val xy = gridSize.split(",")
        return GridSize(xy[0].toInt(), xy[1].toInt())
    }

    private fun getIconSize(): Int? {
        val database = getIconsDatabase()
        val firstIcon = database.getFirstIcon() ?: return null
        val icon = if(areNativeThemedIconsSupported()){
            firstIcon
        }else firstIcon.extractLegacyNormalIcon()
        return Bitmap_getSize(icon).width().also {
            database.close()
        }
    }

    private fun getIconsDatabase(): SQLiteDatabase {
        return SQLiteDatabase.openOrCreateDatabase(pixelLauncherDatabase.absolutePath, null)
    }

    private fun getLauncherDatabase(): SQLiteDatabase? {
        val name = getLauncherDatabaseName() ?: return null
        return SQLiteDatabase.openOrCreateDatabase(name, null)
    }

    private fun getLauncherDatabaseName(): String? {
        return pixelLauncherContext.getDatabasePath(gridSize?.toLauncherFilename() ?: return null).absolutePath
    }

    private fun SQLiteDatabase.getIcons(): List<RemoteApp> {
        val cursor = query(PLM_DATABASE_TABLE_ICONS, null, null, null, null, null, null)
        val componentColumn = cursor.getColumnIndex(PLM_DATABASE_COLUMN_COMPONENT)
        val iconColumn = cursor.getColumnIndex(PLM_DATABASE_COLUMN_ICON)
        val monoIconColumn = cursor.getColumnIndex(PLM_DATABASE_COLUMN_MONO_ICON)
        val iconColorColumn = cursor.getColumnIndex(PLM_DATABASE_COLUMN_ICON_COLOR)
        val labelColumn = cursor.getColumnIndex(PLM_DATABASE_COLUMN_LABEL)
        val monoIconsSupported = areNativeThemedIconsSupported()
        return cursor.map {
            val component = it.getString(componentColumn)
            val type = when(component){
                calendarComponentName -> RemoteApp.Type.CALENDAR
                clockComponentName -> RemoteApp.Type.CLOCK
                else -> RemoteApp.Type.NORMAL
            }
            RemoteApp(
                component,
                it.getBlob(iconColumn),
                if(monoIconsSupported) it.getBlob(monoIconColumn) else null,
                it.getInt(iconColorColumn),
                it.getString(labelColumn),
                type
            )
        }.also {
            cursor.close()
        }
    }

    private fun List<RemoteApp>.asBundle(): List<Bundle> {
        return map { it.asBundle() }
    }

    private fun SQLiteDatabase.getFirstIcon(): ByteArray? {
        val cursor = query(PLM_DATABASE_TABLE_ICONS, arrayOf(PLM_DATABASE_COLUMN_ICON), null, null, null, null, null)
        return cursor.firstNotNull {
            it.getBlob(0)
        }?.also {
            cursor.close()
        }
    }

    override fun loadFavourites(includeFolders: Boolean): ParceledListSlice<Bundle> {
        val database = getLauncherDatabase() ?: return ParceledListSlice.emptyList()
        return ParceledListSlice(database.getFavourites(includeFolders)).also {
            database.close()
        }
    }

    override fun deleteEntryAndRestart(componentName: String?) {
        val database = getIconsDatabase()
        database.delete(
            PLM_DATABASE_TABLE_ICONS,
            "$PLM_DATABASE_COLUMN_COMPONENT=?",
            arrayOf(componentName)
        )
        database.close()
        restartPixelLauncher()
        //Wait for entry to re-populated
        Thread.sleep(2500L)
        notifyDatabaseChange()
    }

    private fun SQLiteDatabase.getFavourites(includeFolders: Boolean): List<Bundle> {
        val cursor = query(PLM_FAVOURITES_TABLE_FAVOURITES, null, null, null, null, null, null)
        val titleColumn = cursor.getColumnIndex(PLM_FAVOURITES_COLUMN_TITLE)
        val intentColumn = cursor.getColumnIndex(PLM_FAVOURITES_COLUMN_INTENT)
        val screenColumn = cursor.getColumnIndex(PLM_FAVOURITES_COLUMN_SCREEN)
        val cellXColumn = cursor.getColumnIndex(PLM_FAVOURITES_COLUMN_CELL_X)
        val cellYColumn = cursor.getColumnIndex(PLM_FAVOURITES_COLUMN_CELL_Y)
        val spanXColumn = cursor.getColumnIndex(PLM_FAVOURITES_COLUMN_SPAN_X)
        val spanYColumn = cursor.getColumnIndex(PLM_FAVOURITES_COLUMN_SPAN_Y)
        val iconColumn = cursor.getColumnIndex(PLM_FAVOURITES_COLUMN_ICON)
        val appWidgetProviderColumn = cursor.getColumnIndex(PLM_FAVOURITES_COLUMN_APP_WIDGET_PROVIDER)
        val appWidgetIdColumn = cursor.getColumnIndex(PLM_FAVOURITES_COLUMN_APP_WIDGET_ID)
        val itemTypeColumn = cursor.getColumnIndex(PLM_FAVOURITES_COLUMN_ITEM_TYPE)
        val containerColumn = cursor.getColumnIndex(PLM_FAVOURITES_COLUMN_CONTAINER)
        return cursor.map {
            val rawItemType = cursor.getInt(itemTypeColumn)
            val itemType = RemoteFavourite.Type.values().firstOrNull { it.remoteType == rawItemType }
                ?: return@map null
            if(includeFolders){
                if(IGNORED_CONTAINERS.contains(cursor.getInt(containerColumn))){
                    return@map null //Reject anything that isn't desktop or folders
                }
            }else{
                if(cursor.getInt(containerColumn) != PLM_FAVOURITES_CONTAINER_DESKTOP){
                    return@map null //Reject non-desktop items (ie. folders, dock etc.)
                }
            }
            val title = cursor.getString(titleColumn)
            val intent = cursor.getString(intentColumn)
            val screen = cursor.getInt(screenColumn)
            val cellX = cursor.getInt(cellXColumn)
            val cellY = cursor.getInt(cellYColumn)
            val spanX = cursor.getInt(spanXColumn)
            val spanY = cursor.getInt(spanYColumn)
            val icon = cursor.getBlob(iconColumn)
            val appWidgetProvider = cursor.getString(appWidgetProviderColumn)
            val appWidgetId = cursor.getInt(appWidgetIdColumn)
            RemoteFavourite(
                title, intent, cellX, cellY, spanX, spanY, screen, appWidgetProvider, appWidgetId, icon, itemType
            ).toBundle()
        }.filterNotNull().also {
            cursor.close()
        }
    }

    private fun SQLiteDatabase.updateFavourite(favourite: RemoteFavourite) {
        update(
            PLM_FAVOURITES_TABLE_FAVOURITES,
            ContentValues().apply {
                if(favourite.title != null) put(PLM_FAVOURITES_COLUMN_TITLE, favourite.title)
                if(favourite.icon != null) put(PLM_FAVOURITES_COLUMN_ICON, favourite.icon)
            },
            "$PLM_FAVOURITES_COLUMN_INTENT=?",
            arrayOf(favourite.intent)
        )
    }

    private fun SQLiteDatabase.updateWidget(widget: RemoteWidget) {
        update(
            PLM_FAVOURITES_TABLE_FAVOURITES,
            ContentValues().apply {
                  put(PLM_FAVOURITES_COLUMN_SPAN_X, widget.spanX)
                  put(PLM_FAVOURITES_COLUMN_SPAN_Y, widget.spanY)
            },
            "$PLM_FAVOURITES_COLUMN_APP_WIDGET_ID=?",
            arrayOf(widget.appWidgetId.toString())
        )
    }

    override fun updateFavourites(favourites: ParceledListSlice<Bundle>) {
        val database = getLauncherDatabase()
        favourites.list.forEach { favourite ->
            val remoteFavourite = RemoteFavourite(favourite)
            database?.updateFavourite(remoteFavourite)
        }
        database?.close()
        restartPixelLauncher()
        notifyDatabaseChange()
    }

    override fun updateWidgets(widgets: ParceledListSlice<Bundle>) {
        val remoteWidgets = widgets.list.map { RemoteWidget(it) }
        val database = getLauncherDatabase()
        remoteWidgets.forEach {
            database?.updateWidget(it)
        }
        database?.close()
        restartPixelLauncher()
    }

    private fun restartPixelLauncher() {
        val wasInForeground = isPixelLauncherInForeground
        /**
         * We can't use ActivityManager here as the Pixel Launcher doesn't have the required
         * permission, but we can use `am force-stop` from a separate process.
         */
        execRootCommand("am force-stop $PIXEL_LAUNCHER_PACKAGE_NAME")
        if(wasInForeground){
            //"Start" the Pixel Launcher, this will actually just bring it to the foreground again
            activityManager.startActivityWithFeature(
                null,
                PIXEL_LAUNCHER_PACKAGE_NAME,
                null,
                pixelLauncherLaunchIntent,
                pixelLauncherLaunchIntent.type,
                null,
                null,
                0,
                pixelLauncherLaunchIntent.flags,
                null,
                ActivityOptions.makeBasic().toBundle()
            )
        }
    }

    init {
        loadNativeLibraries()
        //Have to set the restart listener up before changing UID for permission reasons
        setupPixelLauncherRestartListener()
        //Needs root for permission change
        grantBindPermissionIfRequired()
        /**
         * Switch to the Pixel Launcher UID, allowing access to App Shortcuts and widgets, while
         * keeping access to databases.
         */
        Os.setuid(pixelLauncherUid)
        setupLauncherAppsListener()
    }

    /**
     *  Safely runs a [block] with a given interface [int]. If the interface is dead, `null` will be
     *  returned, and the passed field will be reset to null to prevent it being used again.
     */
    private fun <I: IInterface, R> runWithInterface(int: KMutableProperty0<I?>, block: (I) -> R?): R? {
        try {
            return block(int.get() ?: return null)
        }catch (e: RemoteException){
            //Binder is dead
            int.set(null)
            return null
        }
    }

    //WIDGET PROXY

    private val serviceProxyAppWidgetService by lazy {
        ProxyAppWidgetService(appWidgetService, PIXEL_LAUNCHER_PACKAGE_NAME)
    }

    override fun getProxyAppWidgetService(): ProxyAppWidgetServiceContainer {
        return ProxyAppWidgetServiceContainer(serviceProxyAppWidgetService)
    }

    /**
     *  Grants the bind app permission so we can proxy widget requests
     */
    private fun grantBindPermissionIfRequired() {
        val identity = clearCallingIdentity()
        if(!appWidgetService.hasBindAppWidgetPermission(BuildConfig.APPLICATION_ID, currentUserId)){
            appWidgetService.setBindAppWidgetPermission(
                BuildConfig.APPLICATION_ID,
                currentUserId,
                true
            )
        }
        restoreCallingIdentity(identity)
    }

    // HIDE CLOCK

    override fun setStatusBarIconDenylist(denylist: String) {
        if(denylist.isBlank()){
            execRootCommand("settings delete secure $SETTINGS_KEY_ICON_BLACKLIST")
        }else{
            execRootCommand("settings put secure $SETTINGS_KEY_ICON_BLACKLIST $denylist")
        }
    }

    // OVERLAY

    private val overlayManager by lazy {
        val proxy = SystemServiceHelper.getSystemService("overlay")
        IOverlayManager.Stub.asInterface(proxy)
    }

    override fun restartLauncherImmediately() {
        restartPixelLauncher()
    }

    override fun enableOverlay() {
        val overlay = overlayManager.getOverlayInfo(OVERLAY_PACKAGE_NAME, currentUserId)
        val overlayIdentifier = overlay?.overlayIdentifier ?: return
        overlayManager.commit(OverlayManagerTransaction.Builder()
            .setEnabled(overlayIdentifier, true)
            .build())
    }

    override fun isOverlayInstalled(): Boolean {
        val overlay = overlayManager.getOverlayInfo(OVERLAY_PACKAGE_NAME, currentUserId)
        return overlay != null
    }

    override fun isOverlayEnabled(): Boolean {
        val overlay = overlayManager.getOverlayInfo(OVERLAY_PACKAGE_NAME, currentUserId)
        return overlay?.isEnabled ?: false
    }

    override fun setSearchWidgetPackageEnabled(enabled: Boolean) {
        val key: String
        val table: String
        if(Build.VERSION.SDK_INT >= 35) {
            key = SEARCH_PROVIDER_SETTINGS_KEY_VARIANT
            table = "secure"
        }else{
            key = SEARCH_PROVIDER_SETTINGS_KEY
            table = "global"
        }
        if(enabled) {
            //Set it to PLM package name so it uses our Proxy Widget
            execRootCommand("settings put $table $key ${BuildConfig.APPLICATION_ID}")
        }else{
            //Delete to set it back to default (null)
            execRootCommand("settings delete $table $key")
        }
    }

    override fun uninstallOverlayUpdates() {
        execRootCommand("pm uninstall $OVERLAY_PACKAGE_NAME")
    }

    override fun resetAllIcons(callback: IResetCompleteCallback) {
        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                pixelLauncherDatabase.delete()
                restartLauncherImmediately()
                //Wait for database file to get recreated
                while(!pixelLauncherDatabase.exists()){
                    delay(10L)
                }
                databaseChangeFlow.collectUntilTimeout(5000L) { }
            }
            callback.onResetComplete()
        }
    }

    /**
     *  Execs a command as root. This works as it's a separate process spawned from this one,
     *  which is spawned as the root UID (despite us changing our UID to the Pixel Launcher one).
     */
    private fun execRootCommand(command: String) {
        Runtime.getRuntime().exec(command)
    }

}