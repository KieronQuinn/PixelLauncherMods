package com.kieronquinn.app.pixellaunchermods.model.remote

import android.os.Bundle
import android.os.Parcelable
import androidx.core.os.bundleOf
import kotlinx.parcelize.Parcelize

@Parcelize
data class RemoteFavourite(
    val title: String?,
    val intent: String?,
    val cellX: Int,
    val cellY: Int,
    val spanX: Int,
    val spanY: Int,
    val screen: Int,
    val appWidgetProvider: String?,
    val appWidgetId: Int,
    val icon: ByteArray?,
    val type: Type
): Parcelable {

    enum class Type(val remoteType: Int) {
        APP(0), SHORTCUT(1), APP_SHORTCUT(6), WIDGET(4)
    }

    companion object {
        private const val KEY_TITLE = "title"
        private const val KEY_INTENT = "intent"
        private const val KEY_CELL_X = "cell_x"
        private const val KEY_CELL_Y = "cell_y"
        private const val KEY_SPAN_X = "span_x"
        private const val KEY_SPAN_Y = "span_y"
        private const val KEY_SCREEN = "screen"
        private const val KEY_APP_WIDGET_PROVIDER = "app_widget_provider"
        private const val KEY_APP_WIDGET_ID = "app_widget_id"
        private const val KEY_ICON = "icon"
        private const val KEY_TYPE = "type"
    }

    constructor(bundle: Bundle): this(
        bundle.getString(KEY_TITLE),
        bundle.getString(KEY_INTENT),
        bundle.getInt(KEY_CELL_X),
        bundle.getInt(KEY_CELL_Y),
        bundle.getInt(KEY_SPAN_X),
        bundle.getInt(KEY_SPAN_Y),
        bundle.getInt(KEY_SCREEN),
        bundle.getString(KEY_APP_WIDGET_PROVIDER),
        bundle.getInt(KEY_APP_WIDGET_ID),
        bundle.getByteArray(KEY_ICON),
        bundle.getSerializable(KEY_TYPE) as Type
    )

    fun toBundle(): Bundle {
        return bundleOf(
            KEY_TITLE to title,
            KEY_INTENT to intent,
            KEY_CELL_X to cellX,
            KEY_CELL_Y to cellY,
            KEY_SPAN_X to spanX,
            KEY_SPAN_Y to spanY,
            KEY_SCREEN to screen,
            KEY_APP_WIDGET_PROVIDER to appWidgetProvider,
            KEY_APP_WIDGET_ID to appWidgetId,
            KEY_ICON to icon,
            KEY_TYPE to type
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RemoteFavourite

        if (title != other.title) return false
        if (intent != other.intent) return false
        if (cellX != other.cellX) return false
        if (cellY != other.cellY) return false
        if (spanX != other.spanX) return false
        if (spanY != other.spanY) return false
        if (screen != other.screen) return false
        if (appWidgetProvider != other.appWidgetProvider) return false
        if (appWidgetId != other.appWidgetId) return false
        if (icon != null) {
            if (other.icon == null) return false
            if (!icon.contentEquals(other.icon)) return false
        } else if (other.icon != null) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = title?.hashCode() ?: 0
        result = 31 * result + (intent?.hashCode() ?: 0)
        result = 31 * result + cellX
        result = 31 * result + cellY
        result = 31 * result + spanX
        result = 31 * result + spanY
        result = 31 * result + screen
        result = 31 * result + (appWidgetProvider?.hashCode() ?: 0)
        result = 31 * result + appWidgetId
        result = 31 * result + (icon?.contentHashCode() ?: 0)
        result = 31 * result + type.hashCode()
        return result
    }

}