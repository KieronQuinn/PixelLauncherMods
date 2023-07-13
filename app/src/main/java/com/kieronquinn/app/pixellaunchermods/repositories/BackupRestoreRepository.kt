package com.kieronquinn.app.pixellaunchermods.repositories

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import com.google.gson.Gson
import com.google.gson.stream.JsonWriter
import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.model.backup.Backup
import com.kieronquinn.app.pixellaunchermods.model.editor.AppEditorInfo
import com.kieronquinn.app.pixellaunchermods.model.editor.ShortcutEditorInfo
import com.kieronquinn.app.pixellaunchermods.model.icon.IconPickerResult
import com.kieronquinn.app.pixellaunchermods.model.remote.RemoteApp
import com.kieronquinn.app.pixellaunchermods.model.remote.RemoteFavourite
import com.kieronquinn.app.pixellaunchermods.model.room.IconMetadata
import com.kieronquinn.app.pixellaunchermods.model.room.ModifiedApp
import com.kieronquinn.app.pixellaunchermods.model.room.ModifiedShortcut
import com.kieronquinn.app.pixellaunchermods.model.tweaks.WidgetReplacement
import com.kieronquinn.app.pixellaunchermods.repositories.BackupRestoreRepository.OverlayAction
import com.kieronquinn.app.pixellaunchermods.repositories.BackupRestoreRepository.RestoreIssue
import com.kieronquinn.app.pixellaunchermods.repositories.BackupRestoreRepository.RestoreResult
import com.kieronquinn.app.pixellaunchermods.ui.screens.iconpicker.BasePickerViewModel
import com.kieronquinn.app.pixellaunchermods.utils.extensions.compress
import com.kieronquinn.app.pixellaunchermods.utils.extensions.compressRaw
import com.kieronquinn.app.pixellaunchermods.utils.extensions.parseToComponentName
import com.kieronquinn.app.pixellaunchermods.utils.extensions.updateLegacyIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.reflect.KMutableProperty0

interface BackupRestoreRepository {

    /**
     *  Creates a backup at a given [Uri] [toUri]. Returns if the backup was successful.
     */
    suspend fun createBackup(toUri: Uri): Boolean

    /**
     *  Restores a backup from a given [Uri] [fromUri]. If the file cannot be opened or parsed,
     *  `null` will be returned. If successful, a [RestoreResult] will be returned, with the list
     *  of further  overlay actions (if applicable) and a list of restore issues, or empty for no
     *  issues.
     */
    suspend fun restoreBackup(fromUri: Uri): RestoreResult?

    /**
     *  Returns a date-based filename for the backup
     */
    fun generateFilename(): String

    /**
     *  Returns an array of the possible mime types for a backup
     */
    fun getMimeTypes(): Array<String>

    data class RestoreResult(val restoreIssues: List<RestoreIssue>, val overlayActions: List<OverlayAction>)

    sealed class OverlayAction {
        data class CommitWidgetReplacement(val widgetReplacement: WidgetReplacement): OverlayAction()
        data class CommitHiddenApps(val components: List<String>): OverlayAction()
        data class CommitOverlayTweaks(
            val transparency: Float?,
            val disableWallpaperScrim: Boolean?,
            val disableWallpaperRegionColours: Boolean?,
            val disableSmartspace: Boolean?
        ): OverlayAction() {
            override fun isValid(): Boolean {
                return transparency != null || disableWallpaperScrim != null
                        || disableWallpaperRegionColours != null || disableSmartspace != null
            }
        }

        open fun isValid(): Boolean = true
    }

    sealed class RestoreIssue(open val component: String): Parcelable {

        private fun isShortcut(): Boolean = component.startsWith("#")

        protected fun getPackageName(context: Context): String {
            return if(isShortcut()){
                Intent.parseUri(component, 0).component?.packageName
            }else{
                component.parseToComponentName()?.packageName
            } ?: context.getString(R.string.restore_issue_package_unknown)
        }

        protected fun String.dynamicFormat(context: Context): String {
            val isShortcut = isShortcut()
            val typeShortUpper = if(isShortcut){
                context.getString(R.string.restore_issue_type_shortcut_short)
            }else{
                context.getString(R.string.restore_issue_type_app_short)
            }
            val typeShort = typeShortUpper.lowercase()
            val type = if(isShortcut){
                context.getString(R.string.restore_issue_type_shortcut)
            }else{
                context.getString(R.string.restore_issue_type_app)
            }
            return replace("{type_short}", typeShort)
                .replace("{type_short_upper}", typeShortUpper)
                .replace("{type}", type)
        }

        /**
         *  The app for the config that is attempting to be restored no longer exists
         */
        @Parcelize
        data class ComponentNotFound(override val component: String): RestoreIssue(component) {

            override fun getTitle(context: Context): String {
                return context.getString(R.string.restore_issue_component_title)
                    .dynamicFormat(context)
            }

            override fun getContent(context: Context): String {
                return context.getString(
                    R.string.restore_issue_component_content, getPackageName(context)
                ).dynamicFormat(context)
            }

        }

        /**
         *  The package whose icon is being used no longer exists
         */
        @Parcelize
        data class PackageNotInstalled(
            override val component: String, val packageName: String
        ): RestoreIssue(component) {

            override fun getTitle(context: Context): String {
                return context.getString(R.string.restore_issue_package_title)
                    .dynamicFormat(context)
            }

            override fun getContent(context: Context): String {
                return context.getString(
                    R.string.restore_issue_package_content, getPackageName(context), packageName
                ).dynamicFormat(context)
            }

        }

        /**
         *  The icon pack whose icon is being used is not installed
         */
        @Parcelize
        data class IconPackNotInstalled(
            override val component: String, val iconPackPackageName: String
        ): RestoreIssue(component) {

            override fun getTitle(context: Context): String {
                return context.getString(R.string.restore_issue_icon_pack_title)
                    .dynamicFormat(context)
            }

            override fun getContent(context: Context): String {
                return context.getString(
                    R.string.restore_issue_icon_pack_content, getPackageName(context), iconPackPackageName
                ).dynamicFormat(context)
            }

        }

        /**
         *  Lawnicons is either uninstalled or the referenced icon no longer exists in the package
         */
        @Parcelize
        data class LawniconNotFound(
            override val component: String,
            val iconName: String
        ): RestoreIssue(component) {

            override fun getTitle(context: Context): String {
                return context.getString(R.string.restore_issue_lawnicon_title)
                    .dynamicFormat(context)
            }

            override fun getContent(context: Context): String {
                return context.getString(
                    R.string.restore_issue_lawnicon_content, getPackageName(context), iconName
                ).dynamicFormat(context)
            }

        }

        /**
         *  The legacy mono icon no longer exists in the Pixel Launcher (likely an overlay that
         *  has been deleted, or if the Pixel Launcher drops icons in the future)
         */
        @Parcelize
        data class LegacyMonoIconNotFound(
            override val component: String,
            val iconName: String
        ): RestoreIssue(component){

            override fun getTitle(context: Context): String {
                return context.getString(R.string.restore_issue_legacy_mono_title)
                    .dynamicFormat(context)
            }

            override fun getContent(context: Context): String {
                return context.getString(
                    R.string.restore_issue_legacy_mono_content, getPackageName(context), iconName
                ).dynamicFormat(context)
            }

        }

        /**
         *  A backup from Android 13 using a static mono icon is trying to be applied on Android 12
         */
        @Parcelize
        data class MonoIconNotCompatible(override val component: String): RestoreIssue(component) {

            override fun getTitle(context: Context): String {
                return context.getString(R.string.restore_issue_mono_icon_not_compatible_title)
                    .dynamicFormat(context)
            }

            override fun getContent(context: Context): String {
                return context.getString(
                    R.string.restore_issue_mono_icon_not_compatible_content, getPackageName(context)
                ).dynamicFormat(context)
            }

        }

        /**
         *  The static icon associated with this app was not found in the backup
         */
        @Parcelize
        data class StaticIconNotFound(override val component: String): RestoreIssue(component) {

            override fun getTitle(context: Context): String {
                return context.getString(R.string.restore_issue_static_icon_title)
                    .dynamicFormat(context)
            }

            override fun getContent(context: Context): String {
                return context.getString(R.string.restore_issue_static_icon_content, getPackageName(context))
                    .dynamicFormat(context)
            }

        }

        abstract fun getTitle(context: Context): String
        abstract fun getContent(context: Context): String

    }

}

class BackupRestoreRepositoryImpl(
    applicationContext: Context,
    private val settingsRepository: SettingsRepository,
    private val databaseRepository: DatabaseRepository,
    private val remoteAppsRepository: RemoteAppsRepository,
    private val appsRepository: AppsRepository,
    private val iconLoaderRepository: IconLoaderRepository,
    private val rootServiceRepository: RootServiceRepository,
    private val gson: Gson
): BackupRestoreRepository {

    companion object {
        private const val FILENAME_BACKUP = "backup.json"
        private const val FILENAME_FORMAT_IMAGE = "%s.png"
        private const val FILENAME_FORMAT_MONO_IMAGE = "%s.bin"
        private const val FILENAME_FORMAT_SHORTCUT_IMAGE = "%1s_shortcut.png"
        private const val FILENAME_FORMAT_SHORTCUT_ORIGINAL_IMAGE = "%1s_shortcut_original.png"
        private const val CACHE_RESTORE_DIR = "restore"

        private const val BACKUP_NAME_FORMAT = "pixel_launcher_mods_backup_%s.zip"

        private val ZIP_MIME_TYPES = arrayOf(
            "application/zip",
            "application/octet-stream",
            "application/x-zip-compressed",
            "multipart/x-zip"
        )
    }

    private val contentResolver = applicationContext.contentResolver
    private val restoreDir = File(applicationContext.cacheDir, CACHE_RESTORE_DIR)

    override suspend fun createBackup(toUri: Uri): Boolean = withContext(Dispatchers.IO) {
        val outStream = contentResolver.openOutputStream(toUri) ?: return@withContext false
        val bufferedOutStream = outStream.buffered()
        val zipOutStream = ZipOutputStream(bufferedOutStream)
        val modifiedApps = databaseRepository.getAllModifiedApps()
        val modifiedShortcuts = databaseRepository.getAllModifiedShortcuts()
        zipOutStream.addToZip(FILENAME_BACKUP){ out ->
            val writer = out.writer()
            val jsonWriter = JsonWriter(writer)
            createBackupJson(modifiedApps, modifiedShortcuts, jsonWriter)
        }
        modifiedApps.pipeStaticIcons { icon ->
            zipOutStream.addToZip(icon.first){
                it.write(icon.second)
            }
        }
        modifiedApps.pipeMonoIcons { icon ->
            zipOutStream.addToZip(icon.first){
                it.write(icon.second)
            }
        }
        modifiedShortcuts.pipeIcons { icon ->
            zipOutStream.addToZip(icon.first){
                it.write(icon.second)
            }
        }
        modifiedShortcuts.pipeOriginalIcons { icon ->
            zipOutStream.addToZip(icon.first){
                it.write(icon.second)
            }
        }
        zipOutStream.flush()
        zipOutStream.close()
        true
    }

    private suspend fun ZipOutputStream.addToZip(
        filename: String,
        block: suspend (stream: ZipOutputStream) -> Unit
    ) {
        val entry = ZipEntry(filename)
        putNextEntry(entry)
        block(this)
        closeEntry()
    }

    private suspend fun createBackupJson(
        modifiedApps: List<ModifiedApp>,
        modifiedShortcuts: List<ModifiedShortcut>,
        outWriter: JsonWriter
    ) {
        val backup = Backup()
        backup.apps = modifiedApps.getApps()
        backup.shortcuts = modifiedShortcuts.getShortcuts()
        backup.settings = settingsRepository.getSettings()
        gson.toJson(backup, Backup::class.java, outWriter)
        outWriter.flush()
    }

    private fun List<ModifiedApp>.getApps(): List<Backup.App> {
        return map {
            Backup.App().apply {
                componentName = it.componentName
                label = it.label
                iconColor = it.iconColor
                iconType = it.iconType
                iconMetadata = it.iconMetadata
                monoIconMetadata = it.monoIconMetadata
            }
        }
    }

    private fun List<ModifiedShortcut>.getShortcuts(): List<Backup.Shortcut> {
        return map {
            Backup.Shortcut().apply {
                intent = it.intent
                title = it.title
                iconMetadata = it.iconMetadata
                originalTitle = it.originalTitle
            }
        }
    }

    private suspend fun List<ModifiedApp>.pipeStaticIcons(
        block: suspend (icon: Pair<String, ByteArray>) -> Unit
    ) {
        forEach {
            it.staticIcon?.let { icon ->
                val name = URLEncoder.encode(it.componentName.trim(), UTF_8.name())
                val filename = String.format(FILENAME_FORMAT_IMAGE, name)
                block(Pair(filename, icon))
            }
        }
    }

    private suspend fun List<ModifiedApp>.pipeMonoIcons(
        block: suspend (icon: Pair<String, ByteArray>) -> Unit
    ) {
        forEach {
            it.staticMonoIcon?.let { icon ->
                val name = URLEncoder.encode(it.componentName.trim(), UTF_8.name())
                val filename = String.format(FILENAME_FORMAT_MONO_IMAGE, name)
                block(Pair(filename, icon))
            }
        }
    }

    private suspend fun List<ModifiedShortcut>.pipeIcons(
        block: suspend (icon: Pair<String, ByteArray>) -> Unit
    ) {
        forEach {
            it.icon?.let { icon ->
                val name = URLEncoder.encode(it.intent.trim(), UTF_8.name())
                val filename = String.format(FILENAME_FORMAT_SHORTCUT_IMAGE, name)
                block(Pair(filename, icon))
            }
        }
    }

    private suspend fun List<ModifiedShortcut>.pipeOriginalIcons(
        block: suspend (icon: Pair<String, ByteArray>) -> Unit
    ) {
        forEach {
            it.originalIcon?.let { icon ->
                val name = URLEncoder.encode(it.intent.trim(), UTF_8.name())
                val filename = String.format(FILENAME_FORMAT_SHORTCUT_ORIGINAL_IMAGE, name)
                block(Pair(filename, icon))
            }
        }
    }

    private suspend fun SettingsRepository.getSettings(): Backup.Settings {
        val settings = this
        return Backup.Settings().apply {
            settings.deferredRestartMode.setIfExists(::deferredRestartMode)
            settings.autoIconPackOrder.setIfExists(::autoIconPackOrder)
            settings.hideClock.setIfExists(::hideClock)
            settings.suppressShortcutChangeListener.setIfExists(::suppressShortcutChangeListener)
            settings.hiddenComponents.setIfExists(::hiddenComponents)
            settings.widgetReplacement.setIfExists(::widgetReplacement)
            settings.recentsBackgroundTransparency.setIfExists(::recentsTransparency)
            settings.disableWallpaperScrim.setIfExists(::disableWallpaperScrim)
            settings.disableWallpaperRegionColours.setIfExists(::disableWallpaperRegionColours)
            settings.disableSmartspace.setIfExists(::disableSmartspace)
        }
    }

    private suspend fun SettingsRepository.restoreSettings(backup: Backup.Settings): List<OverlayAction> {
        val settings = this
        with(backup) {
            settings.deferredRestartMode.setIfNotNull(::deferredRestartMode)
            settings.autoIconPackOrder.setIfNotNull(::autoIconPackOrder)
            settings.hideClock.setIfNotNull(::hideClock)
            settings.suppressShortcutChangeListener.setIfNotNull(::suppressShortcutChangeListener)
        }
        val overlayActions = ArrayList<OverlayAction>()
        backup.widgetReplacement?.let {
            overlayActions.add(OverlayAction.CommitWidgetReplacement(it))
        }
        backup.hiddenComponents?.let {
            overlayActions.add(OverlayAction.CommitHiddenApps(it))
        }
        overlayActions.add(OverlayAction.CommitOverlayTweaks(
            backup.recentsTransparency,
            backup.disableWallpaperScrim,
            backup.disableWallpaperRegionColours,
            backup.disableSmartspace
        ))
        return overlayActions
    }

    private suspend fun <T> SettingsRepository.PixelLauncherModsSetting<T>.setIfExists(
        addTo: KMutableProperty0<T?>
    ) {
        if(exists()) addTo.set(get())
    }

    private suspend fun <T> SettingsRepository.PixelLauncherModsSetting<T>.setIfNotNull(
        updateFrom: KMutableProperty0<T?>
    ) {
        updateFrom.get()?.let {
            set(it)
        }
    }

    override suspend fun restoreBackup(fromUri: Uri): RestoreResult? = withContext(Dispatchers.IO) {
        val input = contentResolver.openInputStream(fromUri) ?: return@withContext null
        val nativeMono = remoteAppsRepository.areNativeThemedIconsSupported.value
            ?: return@withContext null
        val outDir = restoreDir.apply {
            mkdirs()
        }
        val zip = ZipInputStream(input)
        unzip(zip, outDir)
        zip.close()
        input.close()
        val backupJson = File(outDir, FILENAME_BACKUP)
        if(!backupJson.exists()) {
            outDir.deleteRecursively()
            return@withContext null
        }
        val backupReader = backupJson.bufferedReader()
        val backup = try {
            gson.fromJson(backupReader, Backup::class.java)
        }catch (e: Exception){
            //Failed to parse JSON
            outDir.deleteRecursively()
            return@withContext null
        }

        val remoteApps = remoteAppsRepository.getRemoteApps(false)
        val remoteShortcuts = remoteAppsRepository.getRemoteShortcuts().mapNotNull {
            (it as? RemoteAppsRepository.Shortcut.LegacyShortcut)?.shortcut
        }
        val restoreIssues = ArrayList<RestoreIssue>()
        val apps = backup.apps.mapNotNull {
            it.toAppEditorInfo(remoteApps, restoreIssues, restoreDir, nativeMono)
        }.toTypedArray()
        databaseRepository.saveAppEditorInfo(*apps)
        val shortcuts = backup.shortcuts.mapNotNull {
            it.toShortcutEditorInfo(remoteShortcuts, restoreIssues, restoreDir, nativeMono)
        }
        databaseRepository.saveModifiedShortcut(*shortcuts.map { it.first }.toTypedArray())
        remoteAppsRepository.updateShortcut(
            *shortcuts.map { it.second.toRemoteFavourite() }.toTypedArray()
        )
        val actions = settingsRepository.restoreSettings(backup.settings)
        outDir.deleteRecursively()
        rootServiceRepository.runWithRootService { it.restartLauncherImmediately() }
        RestoreResult(restoreIssues, actions)
    }

    private suspend fun Backup.App.toAppEditorInfo(
        remoteApps: List<RemoteApp>,
        restoreIssues: ArrayList<RestoreIssue>,
        restoreDir: File,
        nativeMono: Boolean
    ): AppEditorInfo? {
        val component = componentName?.trim() ?: return null //Unrecoverable
        val encodedComponent = URLEncoder.encode(component, UTF_8.name())
        val remoteApp = remoteApps.find { it.componentName == component } ?: run {
            restoreIssues.add(RestoreIssue.ComponentNotFound(component))
            return null
        }
        val staticIcon = File(
            restoreDir, String.format(FILENAME_FORMAT_IMAGE, encodedComponent)
        ).run {
            if(!exists()) return@run null
            readBytes()
        }
        val staticMonoIcon = File(
            restoreDir, String.format(FILENAME_FORMAT_MONO_IMAGE, encodedComponent)
        ).run {
            if(!exists()) return@run null
            readBytes()
        }
        val newIcon = iconMetadata?.let {
            it.toIconPickerResult(
                iconLoaderRepository, appsRepository, staticIcon
            ) ?: it.addRestoreIssueForFailure(component, restoreIssues, false, nativeMono)
        }
        val newMonoIcon = monoIconMetadata?.let {
            it.toIconPickerResult(
                iconLoaderRepository, appsRepository, staticMonoIcon
            ) ?: it.addRestoreIssueForFailure(component, restoreIssues, true, nativeMono)
        }
        val loadedIcon = newIcon?.render(false)
        val loadedMonoIcon = newMonoIcon?.render(true)
        val legacyMonoIcon = newMonoIcon?.toLegacyMonoByteArray(iconLoaderRepository)
        val modifiedApp = ModifiedApp(
            component,
            label,
            loadedIcon,
            loadedMonoIcon,
            null,
            iconColor,
            iconType ?: RemoteApp.Type.NORMAL,
            iconMetadata,
            monoIconMetadata,
            staticIcon,
            staticMonoIcon
        )
        return AppEditorInfo.merge(remoteApp, modifiedApp, nativeMono).apply {
            //Update the legacy icon after load as it is dependent on the current icon sometimes
            legacyIcon = legacyIcon?.run {
                updateLegacyIcon(loadedIcon, legacyMonoIcon)
            }
        }
    }

    private fun Backup.Shortcut.toShortcutEditorInfo(
        remoteShortcuts: List<RemoteFavourite>,
        restoreIssues: ArrayList<RestoreIssue>,
        restoreDir: File,
        nativeMono: Boolean
    ): Pair<ModifiedShortcut, ShortcutEditorInfo>? {
        val shortcutIntent = intent?.trim() ?: return null //Unrecoverable
        val encodedIntent = URLEncoder.encode(shortcutIntent, UTF_8.name())
        val remoteShortcut = remoteShortcuts.firstOrNull {
            it.intent == shortcutIntent
        } ?: run {
            restoreIssues.add(RestoreIssue.ComponentNotFound(shortcutIntent))
            return null
        }
        val staticIcon = File(
            restoreDir, String.format(FILENAME_FORMAT_SHORTCUT_IMAGE, encodedIntent)
        ).run {
            if(!exists()) return@run null
            readBytes()
        }
        val originalIcon = File(
            restoreDir, String.format(FILENAME_FORMAT_SHORTCUT_ORIGINAL_IMAGE, encodedIntent)
        ).run {
            if(!exists()) return@run null
            readBytes()
        }
        val newIcon = iconMetadata?.let {
            it.toIconPickerResult(
                iconLoaderRepository, appsRepository, staticIcon
            ) ?: it.addRestoreIssueForFailure(shortcutIntent, restoreIssues, false, nativeMono)
        }
        val loadedIcon = newIcon?.render(false)
        val modifiedShortcut = ModifiedShortcut(
            shortcutIntent,
            title,
            loadedIcon,
            iconMetadata,
            originalTitle,
            originalIcon
        )
        return Pair(modifiedShortcut, ShortcutEditorInfo.merge(remoteShortcut, modifiedShortcut))
    }

    private fun IconMetadata.addRestoreIssueForFailure(
        component: String,
        restoreIssues: ArrayList<RestoreIssue>,
        isMono: Boolean,
        nativeMono: Boolean
    ): IconPickerResult? {
        //Handle the case where a backup is being restored on an older Android version
        if(!nativeMono && isMono && this !is IconMetadata.LegacyThemedIcon) {
            restoreIssues.add(RestoreIssue.MonoIconNotCompatible(component))
            return null
        }
        val issue = when(this){
            is IconMetadata.Static -> {
                RestoreIssue.StaticIconNotFound(component)
            }
            is IconMetadata.Package -> {
                RestoreIssue.PackageNotInstalled(component, packageName)
            }
            is IconMetadata.IconPack -> {
                RestoreIssue.IconPackNotInstalled(component, packPackageName)
            }
            is IconMetadata.LegacyThemedIcon -> {
                RestoreIssue.LegacyMonoIconNotFound(component, resourceName)
            }
            is IconMetadata.Lawnicon -> {
                RestoreIssue.LawniconNotFound(component, resourceName)
            }
        }
        restoreIssues.add(issue)
        return null
    }

    private fun IconPickerResult.render(mono: Boolean): ByteArray? {
        return iconLoaderRepository.rasterIconPickerResultOptions(
            BasePickerViewModel.IconPickerResultOptions(
                this,
                mono = mono,
                loadFullRes = false
            )
        )?.run {
            val result = if(mono){
                compressRaw()
            }else{
                compress()
            }
            recycle()
            result
        }
    }

    //https://stackoverflow.com/a/64294854/1088334
    private fun unzip(inStream: ZipInputStream, location: File) {
        if (location.exists() && !location.isDirectory)
            throw IllegalStateException("Location file must be directory or not exist")

        if (!location.isDirectory) location.mkdirs()

        val locationPath = location.absolutePath.let {
            if (!it.endsWith(File.separator)) "$it${File.separator}"
            else it
        }

        var zipEntry: ZipEntry?
        var unzipFile: File
        var unzipParentDir: File?

        while (inStream.nextEntry.also { zipEntry = it } != null) {
            unzipFile = File(locationPath + zipEntry!!.name)
            if (zipEntry!!.isDirectory) {
                if (!unzipFile.isDirectory) unzipFile.mkdirs()
            } else {
                unzipParentDir = unzipFile.parentFile
                if (unzipParentDir != null && !unzipParentDir.isDirectory) {
                    unzipParentDir.mkdirs()
                }
                BufferedOutputStream(FileOutputStream(unzipFile)).use { outStream ->
                    inStream.copyTo(outStream)
                }
            }
        }
    }

    override fun generateFilename(): String {
        val timestampFormat = DateTimeFormatter.ofPattern("ddMMyy_HHmmss")
        val timestamp = timestampFormat.format(LocalDateTime.now())
        return String.format(BACKUP_NAME_FORMAT, timestamp)
    }

    override fun getMimeTypes(): Array<String> {
        return ZIP_MIME_TYPES
    }

}