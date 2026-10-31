package io.github.natnael_dev.dailyread.widget

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import io.github.natnael_dev.dailyread.notifications.ReminderScheduler
import java.util.concurrent.TimeUnit

object WidgetRolloverScheduler {

    const val WORK_NAME_WIDGET_ROLLOVER = "dailyread_widget_rollover"
    const val ROLLOVER_HOUR = 3 // 03:00 AM logical midnight rollover
    const val ROLLOVER_MINUTE = 0

    /**
     * Schedules the next 03:00 logical day rollover refresh using inexact WorkManager.
     * ZERO exact-alarm permissions required.
     */
    fun scheduleNextRollover(context: Context) {
        val delayMs = ReminderScheduler.calculateInitialDelayMs(
            targetHour = ROLLOVER_HOUR,
            targetMinute = ROLLOVER_MINUTE
        )

        val request = OneTimeWorkRequestBuilder<WidgetRolloverWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .addTag("rollover")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME_WIDGET_ROLLOVER,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
