package io.github.natnael_dev.dailyread.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    const val WORK_NAME_DAILY_REMINDER = "dailyread_daily_reminder"
    const val WORK_NAME_URGENCY_REMINDER = "dailyread_urgency_reminder"

    const val DEFAULT_REMINDER_HOUR = 20 // 8:00 PM standard habit reminder
    const val DEFAULT_REMINDER_MINUTE = 0

    const val URGENCY_REMINDER_HOUR = 1  // 1:00 AM (2 hours before 03:00 rollover)
    const val URGENCY_REMINDER_MINUTE = 0

    /**
     * Calculates the initial delay in milliseconds to reach the target hour:minute.
     * Guaranteed to return a value in [0, 24 hours].
     */
    fun calculateInitialDelayMs(
        targetHour: Int,
        targetMinute: Int,
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        val nowCal = Calendar.getInstance(timeZone).apply { timeInMillis = nowMillis }
        val targetCal = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (targetCal.timeInMillis <= nowCal.timeInMillis) {
            targetCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        return (targetCal.timeInMillis - nowCal.timeInMillis).coerceAtLeast(0)
    }

    /**
     * Schedules inexact daily reminders via WorkManager.
     * Uses NO exact alarm permissions. WorkManager handles battery optimization and doze gracefully.
     */
    fun scheduleReminders(context: Context) {
        val workManager = WorkManager.getInstance(context)

        // 1. Primary daily reminder (e.g. 20:00)
        val dailyDelay = calculateInitialDelayMs(DEFAULT_REMINDER_HOUR, DEFAULT_REMINDER_MINUTE)
        val dailyRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(dailyDelay, TimeUnit.MILLISECONDS)
            .addTag("reminder")
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME_DAILY_REMINDER,
            ExistingPeriodicWorkPolicy.UPDATE,
            dailyRequest
        )

        // 2. Secondary urgency escalation reminder (01:00 AM)
        val urgencyDelay = calculateInitialDelayMs(URGENCY_REMINDER_HOUR, URGENCY_REMINDER_MINUTE)
        val urgencyRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(urgencyDelay, TimeUnit.MILLISECONDS)
            .addTag("urgency")
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME_URGENCY_REMINDER,
            ExistingPeriodicWorkPolicy.UPDATE,
            urgencyRequest
        )
    }

    fun cancelReminders(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(WORK_NAME_DAILY_REMINDER)
        workManager.cancelUniqueWork(WORK_NAME_URGENCY_REMINDER)
    }
}
