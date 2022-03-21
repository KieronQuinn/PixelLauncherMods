package com.kieronquinn.app.pixellaunchermods.work

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.kieronquinn.app.pixellaunchermods.BuildConfig
import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.components.notifications.NotificationChannel
import com.kieronquinn.app.pixellaunchermods.components.notifications.NotificationId
import com.kieronquinn.app.pixellaunchermods.components.notifications.createNotification
import com.kieronquinn.app.pixellaunchermods.repositories.SettingsRepository
import com.kieronquinn.app.pixellaunchermods.repositories.UpdateRepository
import com.kieronquinn.app.pixellaunchermods.ui.activities.MainActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class UpdateCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams), KoinComponent {

    companion object {
        private const val UPDATE_CHECK_WORK_TAG = "plm_update_check"
        private const val UPDATE_CHECK_HOUR = 12L

        private fun clearCheckWorker(context: Context){
            val workManager = WorkManager.getInstance(context)
            workManager.cancelAllWorkByTag(UPDATE_CHECK_WORK_TAG)
        }

        fun queueCheckWorker(context: Context){
            clearCheckWorker(context)
            val checkWorker = Builder().build()
            val workManager = WorkManager.getInstance(context)
            workManager.enqueueUniquePeriodicWork(UPDATE_CHECK_WORK_TAG, ExistingPeriodicWorkPolicy.REPLACE, checkWorker)
        }
    }

    private val updateRepository by inject<UpdateRepository>()
    private val settingsRepository by inject<SettingsRepository>()
    private val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override fun doWork(): Result {
        GlobalScope.launch {
            //Reject if we're not rooted
            if(!settingsRepository.shouldLaunchService.get()) return@launch
            //Reject if there's no update
            val update = updateRepository.getUpdate() ?: return@launch
            applicationContext.createNotification(NotificationChannel.UPDATE) {
                val content = applicationContext.getString(R.string.notification_title_content, BuildConfig.VERSION_NAME, update.versionName)
                val title = applicationContext.getString(R.string.notification_title_update)
                it.setOngoing(false)
                it.setAutoCancel(true)
                it.setSmallIcon(R.drawable.ic_notification)
                it.setContentTitle(title)
                it.setContentText(content)
                it.setCategory(Notification.CATEGORY_SERVICE)
                it.setStyle(NotificationCompat.BigTextStyle(it).bigText(content))
                it.priority = NotificationCompat.PRIORITY_HIGH
                it.setContentIntent(
                    PendingIntent.getActivity(
                        applicationContext,
                        NotificationId.UPDATE.ordinal,
                        Intent(applicationContext, MainActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE
                    )
                )
            }.also {
                notificationManager.notify(NotificationId.UPDATE.ordinal, it)
            }
        }
        return Result.success()
    }

    class Builder {
        fun build() : PeriodicWorkRequest {
            val delay = if (LocalDateTime.now().hour < UPDATE_CHECK_HOUR) {
                Duration.between(ZonedDateTime.now(), ZonedDateTime.now().toLocalDate().atStartOfDay(ZoneId.systemDefault()).plusHours(UPDATE_CHECK_HOUR)).toMinutes()
            } else {
                Duration.between(ZonedDateTime.now(), ZonedDateTime.now().toLocalDate().atStartOfDay(ZoneId.systemDefault()).plusDays(1).plusHours(UPDATE_CHECK_HOUR)).toMinutes()
            }

            val constraints: Constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            return PeriodicWorkRequest.Builder(UpdateCheckWorker::class.java, 24, TimeUnit.HOURS).addTag(UPDATE_CHECK_WORK_TAG)
                .setInitialDelay(delay, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, PeriodicWorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
                .build()
        }
    }

}