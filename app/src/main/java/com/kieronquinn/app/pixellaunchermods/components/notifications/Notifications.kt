package com.kieronquinn.app.pixellaunchermods.components.notifications

import android.Manifest
import android.app.Activity
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.kieronquinn.app.pixellaunchermods.R
import android.app.NotificationChannel as AndroidNotificationChannel

fun Context.createNotification(
    channel: NotificationChannel,
    builder: (NotificationCompat.Builder) -> Unit
): Notification {
    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val notificationChannel =
        AndroidNotificationChannel(
            channel.id,
            getString(channel.titleRes),
            channel.importance
        ).apply {
            description = getString(channel.descRes)
        }
    notificationManager.createNotificationChannel(notificationChannel)
    return NotificationCompat.Builder(this, channel.id).apply(builder).build()
}

fun Activity.requestNotificationPermission() {
    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    //We don't actually care about the result - this is purely for better UX
    requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
}

enum class NotificationChannel(
    val id: String,
    val importance: Int,
    val titleRes: Int,
    val descRes: Int
) {
    FOREGROUND_SERVICE(
        "foreground_service",
        NotificationManager.IMPORTANCE_LOW,
        R.string.notification_channel_title_foreground_service,
        R.string.notification_channel_desc_foreground_service
    ),
    ICON_APPLYING(
        "icon_applying",
        NotificationManager.IMPORTANCE_MIN,
        R.string.notification_channel_title_icon_applying,
        R.string.notification_channel_desc_icon_applying
    ),
    UPDATE(
        "update",
        NotificationManager.IMPORTANCE_DEFAULT,
        R.string.notification_channel_title_update,
        R.string.notification_channel_desc_update
    )
}

enum class NotificationId {
    BUFFER, //Foreground ID cannot be 0
    FOREGROUND_SERVICE,
    ICON_APPLYING,
    PROXY_WIDGET_CLICK, //Not actually an ID but used for PendingIntent request code
    UPDATE
}