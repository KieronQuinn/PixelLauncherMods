package com.kieronquinn.app.pixellaunchermods.model.icon

import android.net.Uri
import android.os.Parcelable
import com.kieronquinn.app.pixellaunchermods.model.room.IconMetadata
import com.kieronquinn.app.pixellaunchermods.repositories.IconLoaderRepository
import com.kieronquinn.app.pixellaunchermods.ui.screens.iconpicker.BasePickerViewModel
import com.kieronquinn.app.pixellaunchermods.utils.extensions.compress
import com.kieronquinn.app.pixellaunchermods.utils.extensions.compressRaw
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize

sealed class IconPickerResult: Parcelable {

    @Parcelize
    data class IconPackIcon(
        val iconPackPackageName: String,
        val iconResource: String,
        val isAdaptiveIcon: Boolean
    ) : IconPickerResult() {
        override fun toModifiedAppMetadata(applyType: IconMetadata.ApplyType): IconMetadata {
            return IconMetadata.IconPack(
                applyType,
                iconPackPackageName,
                iconResource,
                isAdaptiveIcon
            )
        }
    }

    @Parcelize
    data class PackageIcon(
        val applicationIcon: ApplicationIcon
    ) : IconPickerResult() {
        override fun toModifiedAppMetadata(applyType: IconMetadata.ApplyType): IconMetadata {
            return IconMetadata.Package(
                applyType,
                applicationIcon.applicationInfo.packageName,
                applicationIcon.shrinkNonAdaptiveIcons
            )
        }
    }

    @Parcelize
    data class LegacyThemedIcon(
        val resourceId: Int,
        val resourceName: String
    ) : IconPickerResult() {
        override fun toModifiedAppMetadata(applyType: IconMetadata.ApplyType): IconMetadata {
            return IconMetadata.LegacyThemedIcon(
                applyType,
                resourceName
            )
        }
    }

    @Parcelize
    data class Lawnicon(
        val resourceId: Int,
        val resourceName: String
    ) : IconPickerResult() {
        override fun toModifiedAppMetadata(applyType: IconMetadata.ApplyType): IconMetadata {
            return IconMetadata.Lawnicon(
                applyType,
                resourceName
            )
        }
    }

    @Parcelize
    data class UriIcon(
        val uri: Uri
    ) : IconPickerResult() {
        override fun toModifiedAppMetadata(applyType: IconMetadata.ApplyType): IconMetadata {
            return IconMetadata.Static(applyType)
        }
    }

    /**
     *  Only used when restoring icons, never used during pick process
     */
    @Parcelize
    data class BitmapIcon(
        val bitmapBytes: ByteArray
    ) : IconPickerResult() {

        override fun toModifiedAppMetadata(applyType: IconMetadata.ApplyType): IconMetadata {
            return IconMetadata.Static(applyType)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as BitmapIcon

            if (!bitmapBytes.contentEquals(other.bitmapBytes)) return false

            return true
        }

        override fun hashCode(): Int {
            return bitmapBytes.contentHashCode()
        }


    }

    abstract fun toModifiedAppMetadata(applyType: IconMetadata.ApplyType): IconMetadata

    suspend fun toByteArray(
        iconLoaderRepository: IconLoaderRepository, loadFullRes: Boolean = false
    ): ByteArray? = withContext(Dispatchers.IO) {
        val loadIcon = { fullRes: Boolean ->
            val bitmap = iconLoaderRepository.rasterIconPickerResultOptions(
                BasePickerViewModel.IconPickerResultOptions(
                    this@IconPickerResult,
                    mono = false,
                    loadFullRes = fullRes
                )
            )
            bitmap?.compress()
        }
        if (loadFullRes) {
            return@withContext if (this@IconPickerResult is UriIcon) {
                loadIcon(true)
            } else null
        } else loadIcon(false)
    }

    suspend fun toMonoByteArray(
        iconLoaderRepository: IconLoaderRepository, loadFullRes: Boolean = false
    ): ByteArray? = withContext(Dispatchers.IO) {
        val loadIcon = { fullRes: Boolean ->
            val bitmap = iconLoaderRepository.rasterIconPickerResultOptions(
                BasePickerViewModel.IconPickerResultOptions(
                    this@IconPickerResult,
                    mono = true,
                    loadFullRes = fullRes
                )
            )
            bitmap?.compressRaw()
        }
        if (loadFullRes) {
            return@withContext if (this@IconPickerResult is UriIcon) {
                loadIcon(true)
            } else null
        } else loadIcon(false)
    }

    suspend fun toLegacyMonoByteArray(
        iconLoaderRepository: IconLoaderRepository
    ): ByteArray? = withContext(Dispatchers.IO) {
        if (this@IconPickerResult !is LegacyThemedIcon) {
            //Only LegacyThemedIcons can be used for legacy mono icons
            return@withContext null
        }
        iconLoaderRepository.createLegacyThemedIcon(resourceId).toLegacyByteArray()
    }

}