package com.kieronquinn.app.pixellaunchermods.model.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.kieronquinn.app.pixellaunchermods.utils.room.IconMetadataConverter

@Database(entities = [ModifiedApp::class, ModifiedShortcut::class], version = 1)
abstract class PixelLauncherModsDatabase: RoomDatabase() {

    abstract fun appsDao(): AppsDao
    abstract fun shortcutsDao(): ShortcutsDao

}

private const val DATABASE_NAME = "pixellaunchermods.db"

fun getRoomDatabase(context: Context) =
    Room.databaseBuilder(context, PixelLauncherModsDatabase::class.java, DATABASE_NAME)
        .addTypeConverter(IconMetadataConverter())
        .build()