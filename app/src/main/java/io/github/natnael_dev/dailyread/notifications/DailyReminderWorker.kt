package io.github.natnael_dev.dailyread.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.natnael_dev.dailyread.MainActivity
import io.github.natnael_dev.dailyread.data.db.DailyReadDatabase
import io.github.natnael_dev.dailyread.domain.streak.QualifyingDay
import io.github.natnael_dev.dailyread.domain.streak.StreakEngine
import io.github.natnael_dev.dailyread.domain.time.LogicalDay
import io.github.natnael_dev.dailyread.domain.time.LogicalDayCalculator
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.util.Calendar

class DailyReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.i(TAG, "DailyReminderWorker executing on thread=${Thread.currentThread().name}")

        val database = DailyReadDatabase.getInstance(applicationContext)

        // 1. Check user preference (DEC-034: Default is ON)
        val remindersSetting = database.settingsDao().getSetting("reminders_enabled")
        val remindersEnabled = remindersSetting?.toBooleanStrictOrNull() ?: true
        if (!remindersEnabled) {
            Log.i(TAG, "Reminders disabled by user preference. Suppressing.")
            return Result.success()
        }

        // 2. Check system notification permission
        val notificationManager = NotificationManagerCompat.from(applicationContext)
        if (!notificationManager.areNotificationsEnabled()) {
            Log.i(TAG, "Notification permission denied at system level. Suppressing.")
            return Result.success()
        }

        // 3. Compute logical day & current status
        val zoneId = ZoneId.systemDefault()
        val nowInstant = Instant.now()
        val calculator = LogicalDayCalculator()
        val today = calculator.computeLogicalDay(nowInstant, zoneId)
        val todayIso = today.toIsoString()

        val ledgerDao = database.readingLedgerDao()
        val pagesReadToday = ledgerDao.getDistinctPositionCountForDay(todayIso)
        val goalEntity = ledgerDao.getActiveGoalForDay(todayIso)
        val dailyGoal = goalEntity?.target ?: 10 // DEC-041: Default daily goal is 10 pages

        // 4. If goal already met today, suppress notification
        if (pagesReadToday >= dailyGoal) {
            Log.i(TAG, "Goal already met today ($pagesReadToday / $dailyGoal pages). Suppressing reminder.")
            return Result.success()
        }

        // 5. Query streak and active book
        val qualifyingEntities = ledgerDao.getQualifyingDays()
        val qualifyingDays = qualifyingEntities.map {
            QualifyingDay(
                logicalDay = LogicalDay.parse(it.logicalDay),
                utcOffsetSeconds = it.utcOffsetSeconds
            )
        }
        val streakResult = StreakEngine().calculateStreak(
            qualifyingDaysInput = qualifyingDays,
            today = today,
            currentZoneOffsetSeconds = zoneId.rules.getOffset(nowInstant).totalSeconds
        )

        val books = database.bookDao().getAllBooks().first()
        val lastBook = books.filter { it.lastOpenedAt != null }
            .maxByOrNull { it.lastOpenedAt ?: 0L }
            ?: books.firstOrNull()

        // 6. Compute consecutive inactive days
        var consecutiveInactive = 0
        var checkDay = today.minusDays(1)
        val qualifiedSet = qualifyingDays.map { it.logicalDay }.toSet()
        while (!qualifiedSet.contains(checkDay) && consecutiveInactive < 10) {
            consecutiveInactive++
            checkDay = checkDay.minusDays(1)
        }

        val hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val variant = (System.currentTimeMillis() / (1000 * 60 * 60 * 24)).toInt()

        // 7. Select psychological hook copy
        val copy = ReminderCopySelector().selectReminderCopy(
            streakDays = streakResult.currentStreak,
            pagesReadToday = pagesReadToday,
            dailyGoalPages = dailyGoal,
            activeBookTitle = lastBook?.title,
            consecutiveInactiveDays = consecutiveInactive,
            hourOfDay = hourOfDay,
            variant = variant
        ) ?: run {
            Log.i(TAG, "Copy selector returned null. Suppressing reminder.")
            return Result.success()
        }

        // 8. Post notification
        postNotification(copy, lastBook?.id)
        return Result.success()
    }

    private fun postNotification(copy: ReminderCopy, lastBookId: Long?) {
        createNotificationChannel()

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (lastBookId != null) {
                putExtra("bookId", lastBookId)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val priority = if (copy.isUrgent) {
            NotificationCompat.PRIORITY_HIGH
        } else {
            NotificationCompat.PRIORITY_DEFAULT
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(copy.title)
            .setContentText(copy.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(copy.message))
            .setPriority(priority)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            val manager = NotificationManagerCompat.from(applicationContext)
            manager.notify(NOTIFICATION_ID, notification)
            Log.i(TAG, "Posted notification: title='${copy.title}' tone=${copy.tone}")
        } catch (e: SecurityException) {
            Log.w(TAG, "Failed to post notification due to security exception: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Reading Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily reading habit reminders and streak protection alerts"
            }
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val TAG = "DailyReminderWorker"
        const val CHANNEL_ID = "dailyread_reminders"
        const val NOTIFICATION_ID = 4001
    }
}
