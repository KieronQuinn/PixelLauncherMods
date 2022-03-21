package com.kieronquinn.app.pixellaunchermods.model.tweaks

import android.appwidget.AppWidgetHostView
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

enum class WidgetReplacement {
    NONE, TOP, BOTTOM
}

@Parcelize
data class ParceledWidgetReplacement(val widgetReplacement: WidgetReplacement): Parcelable

data class WidgetReplacementOptions(
    val widgetView: AppWidgetHostView,
    val widgetReplacement: WidgetReplacement
)