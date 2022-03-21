package com.kieronquinn.app.pixellaunchermods.model.editor

import android.content.Intent
import android.os.Parcelable
import com.kieronquinn.app.pixellaunchermods.model.icon.ApplicationIcon
import kotlinx.parcelize.Parcelize

@Parcelize
data class IconPack(
    val packageName: String,
    val label: CharSequence,
    val iconPackIcon: ApplicationIcon,
    val externalIntent: Intent?
) : Parcelable