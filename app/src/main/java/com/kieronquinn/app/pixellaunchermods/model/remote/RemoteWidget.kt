package com.kieronquinn.app.pixellaunchermods.model.remote

import android.os.Bundle
import android.os.Parcelable
import androidx.core.os.bundleOf
import kotlinx.parcelize.Parcelize

@Parcelize
data class RemoteWidget(
    val appWidgetId: Int,
    var spanX: Int,
    var spanY: Int
) : Parcelable {

    companion object {
        private const val KEY_APP_WIDGET_ID = "app_widget_id"
        private const val KEY_SPAN_X = "span_x"
        private const val KEY_SPAN_Y = "span_y"
    }


    constructor(bundle: Bundle): this(
        bundle.getInt(KEY_APP_WIDGET_ID),
        bundle.getInt(KEY_SPAN_X),
        bundle.getInt(KEY_SPAN_Y)
    )

    fun toBundle(): Bundle {
        return bundleOf(
            KEY_APP_WIDGET_ID to appWidgetId,
            KEY_SPAN_X to spanX,
            KEY_SPAN_Y to spanY,
        )
    }

}