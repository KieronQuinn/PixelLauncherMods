package com.kieronquinn.app.pixellaunchermods.model.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppsDao {

    @Query("select * from apps")
    fun getAll(): Flow<List<ModifiedApp>>

    @Query("select * from apps where componentName=:componentName")
    fun getByComponentName(componentName: String): Flow<List<ModifiedApp>>

    @Insert(onConflict = REPLACE)
    fun insert(vararg app: ModifiedApp)

    @Query("delete from apps where componentName=:componentName")
    fun delete(componentName: String)

    @Query("delete from apps")
    fun deleteAll()

}