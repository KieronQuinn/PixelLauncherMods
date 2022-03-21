package com.kieronquinn.app.pixellaunchermods.model.icon

import android.content.pm.ApplicationInfo
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ApplicationIcon(
    val applicationInfo: ApplicationInfo,
    val shrinkNonAdaptiveIcons: Boolean,
    val mono: Boolean = false
): Parcelable
