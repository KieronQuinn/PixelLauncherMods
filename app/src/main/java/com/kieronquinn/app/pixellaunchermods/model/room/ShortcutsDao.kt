package com.kieronquinn.app.pixellaunchermods.model.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ShortcutsDao {

    @Query("select * from shortcuts")
    fun getAll(): Flow<List<ModifiedShortcut>>

    @Query("select * from shortcuts where intent=:intent")
    fun getByIntent(intent: String): Flow<List<ModifiedShortcut>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg shortcut: ModifiedShortcut)

    @Query("delete from shortcuts where intent=:intent")
    fun delete(intent: String)

}