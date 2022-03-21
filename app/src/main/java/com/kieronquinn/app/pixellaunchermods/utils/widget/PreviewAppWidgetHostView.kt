package com.kieronquinn.app.pixellaunchermods.utils.widget

import android.appwidget.AppWidgetHostView
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.RemoteViews
import com.kieronquinn.app.pixellaunchermods.R
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

/**
 * [AppWidgetHostView] which exposes changes in a Flow ([onChanged]). This view will also show
 * [R.layout.widget_error] as the error view, rather than the default, instructing the user to try
 * a different widget.
 */
class PreviewAppWidgetHostView(context: Context): AppWidgetHostView(context) {

    private val layoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    interface OnChangedListener {
        fun onChanged()
    }

    var onChangedListener: OnChangedListener? = null

    override fun updateAppWidget(remoteViews: RemoteViews?) {
        super.updateAppWidget(remoteViews)
        onChangedListener?.onChanged()
    }

    override fun getErrorView(): View {
        return layoutInflater.inflate(R.layout.widget_error, null, false)
    }

    fun onChanged() = callbackFlow {
        val listener = object: OnChangedListener {
            override fun onChanged() {
                trySend(Unit)
            }
        }
        onChangedListener = listener
        trySend(Unit)
        awaitClose {
            onChangedListener = null
        }
    }

}