package com.kieronquinn.app.pixellaunchermods.model.ipc

import android.os.IBinder
import android.os.Parcelable
import com.android.internal.appwidget.IAppWidgetService
import kotlinx.parcelize.Parcelize

@Parcelize
data class ProxyAppWidgetServiceContainer(val proxy: IBinder): Parcelable
