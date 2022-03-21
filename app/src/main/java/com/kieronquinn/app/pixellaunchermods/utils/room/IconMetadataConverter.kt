package com.kieronquinn.app.pixellaunchermods.utils.room

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.kieronquinn.app.pixellaunchermods.model.room.IconMetadata
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@ProvidedTypeConverter
class IconMetadataConverter: KoinComponent {

    private val gson by inject<Gson>()

    @TypeConverter
    fun StringToModifiedAppMetadata(string: String?): IconMetadata? {
        return gson.fromJson(string ?: return null, IconMetadata::class.java)
    }

    @TypeConverter
    fun IconMetadataToString(iconMetadata: IconMetadata?): String? {
        return gson.toJson(iconMetadata ?: return null)
    }

}