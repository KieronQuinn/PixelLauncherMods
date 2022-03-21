package com.kieronquinn.app.pixellaunchermods.repositories

import com.kieronquinn.app.pixellaunchermods.model.editor.AppEditorInfo
import com.kieronquinn.app.pixellaunchermods.model.room.ModifiedApp
import com.kieronquinn.app.pixellaunchermods.model.room.ModifiedShortcut
import com.kieronquinn.app.pixellaunchermods.model.room.PixelLauncherModsDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface DatabaseRepository {

    fun getAllModifiedAppsAsFlow(): Flow<List<ModifiedApp>>
    suspend fun getAllModifiedApps(): List<ModifiedApp>
    suspend fun saveAppEditorInfo(vararg appEditorInfo: AppEditorInfo)
    suspend fun getModifiedApp(componentName: String): Flow<ModifiedApp?>
    suspend fun removeModifiedApp(componentName: String)

    suspend fun getAllModifiedShortcuts(): List<ModifiedShortcut>
    suspend fun saveModifiedShortcut(vararg modifiedShortcut: ModifiedShortcut)
    suspend fun getModifiedShortcut(intent: String): Flow<ModifiedShortcut?>
    suspend fun removeModifiedShortcut(intent: String)

    suspend fun clearAllIcons()

}

class DatabaseRepositoryImpl(
    database: PixelLauncherModsDatabase
): DatabaseRepository {

    private val appsDao = database.appsDao()
    private val shortcutDao = database.shortcutsDao()

    override fun getAllModifiedAppsAsFlow(): Flow<List<ModifiedApp>> {
        return appsDao.getAll()
    }

    override suspend fun getAllModifiedApps() = withContext(Dispatchers.IO) {
        appsDao.getAll().first()
    }

    override suspend fun saveAppEditorInfo(vararg appEditorInfo: AppEditorInfo) {
        val modifiedApps = appEditorInfo.map { info ->
            ModifiedApp(
                info.componentName,
                if(info.hasChangedLabel) info.label else null,
                if(info.hasChangedIcon) info.icon else null,
                if(info.hasChangedMonoIcon) info.monoIcon else null,
                if(info.hasChangedIcon || info.hasChangedMonoIcon) {
                    info.legacyIcon
                }else null,
                if(info.hasChangedIcon) info.iconColor else null,
                info.iconType,
                info.iconMetadata,
                info.monoIconMetadata,
                info.staticIcon,
                info.staticMonoIcon
            )
        }.toTypedArray()
        saveModifiedApp(*modifiedApps)
    }

    private suspend fun saveModifiedApp(vararg modifiedApp: ModifiedApp) = withContext(Dispatchers.IO) {
        appsDao.insert(*modifiedApp)
    }

    override suspend fun getModifiedApp(componentName: String) = withContext(Dispatchers.IO) {
        appsDao.getByComponentName(componentName).map { it.firstOrNull() }
    }

    override suspend fun removeModifiedApp(componentName: String) = withContext(Dispatchers.IO) {
        appsDao.delete(componentName)
    }

    override suspend fun getAllModifiedShortcuts() = withContext(Dispatchers.IO) {
        shortcutDao.getAll().first()
    }

    override suspend fun saveModifiedShortcut(vararg modifiedShortcut: ModifiedShortcut) = withContext(Dispatchers.IO) {
        shortcutDao.insert(*modifiedShortcut)
    }

    override suspend fun getModifiedShortcut(intent: String) = withContext(Dispatchers.IO) {
        shortcutDao.getByIntent(intent).map { it.firstOrNull() }
    }

    override suspend fun removeModifiedShortcut(intent: String) = withContext(Dispatchers.IO) {
        shortcutDao.delete(intent)
    }

    override suspend fun clearAllIcons() = withContext(Dispatchers.IO) {
        appsDao.deleteAll()
    }

}