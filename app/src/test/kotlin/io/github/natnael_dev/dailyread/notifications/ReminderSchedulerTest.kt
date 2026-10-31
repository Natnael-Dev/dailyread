package io.github.natnael_dev.dailyread.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ReminderSchedulerTest {

    private val timeZone = TimeZone.getTimeZone("UTC")

    @Test
    fun targetTimeInFutureTodayReturnsPositiveDelayWithinDay() {
        // Current time: 14:00 UTC
        val calendar = Calendar.getInstance(timeZone).apply {
            set(2026, Calendar.OCTOBER, 31, 14, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = calendar.timeInMillis

        // Target: 20:00 UTC (6 hours later)
        val delayMs = ReminderScheduler.calculateInitialDelayMs(
            targetHour = 20,
            targetMinute = 0,
            nowMillis = now,
            timeZone = timeZone
        )

        val expectedMs = 6 * 60 * 60 * 1000L
        assertEquals("Delay should be exactly 6 hours", expectedMs, delayMs)
    }

    @Test
    fun targetTimeAlreadyPassedTodaySchedulesForTomorrow() {
        // Current time: 21:00 UTC
        val calendar = Calendar.getInstance(timeZone).apply {
            set(2026, Calendar.OCTOBER, 31, 21, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = calendar.timeInMillis

        // Target: 20:00 UTC (past by 1 hour) -> should be 23 hours later tomorrow
        val delayMs = ReminderScheduler.calculateInitialDelayMs(
            targetHour = 20,
            targetMinute = 0,
            nowMillis = now,
            timeZone = timeZone
        )

        val expectedMs = 23 * 60 * 60 * 1000L
        assertEquals("Delay should schedule for next day at 20:00 (23 hours)", expectedMs, delayMs)
    }

    @Test
    fun targetUrgencyWindowNear0100SchedulesCorrectly() {
        // Current time: 20:00 UTC on Oct 31
        val calendar = Calendar.getInstance(timeZone).apply {
            set(2026, Calendar.OCTOBER, 31, 20, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = calendar.timeInMillis

        // Target: 01:00 AM (5 hours later)
        val delayMs = ReminderScheduler.calculateInitialDelayMs(
            targetHour = 1,
            targetMinute = 0,
            nowMillis = now,
            timeZone = timeZone
        )

        val expectedMs = 5 * 60 * 60 * 1000L
        assertEquals("Delay should be 5 hours until 01:00 AM", expectedMs, delayMs)
    }

    @Test
    fun delayIsAlwaysNonNegativeAndUnder24Hours() {
        for (h in 0..23) {
            val calendar = Calendar.getInstance(timeZone).apply {
                set(2026, Calendar.OCTOBER, 31, h, 30, 0)
            }
            val delayMs = ReminderScheduler.calculateInitialDelayMs(
                targetHour = 20,
                targetMinute = 0,
                nowMillis = calendar.timeInMillis,
                timeZone = timeZone
            )
            assertTrue("Delay must be >= 0", delayMs >= 0)
            assertTrue("Delay must be <= 24 hours", delayMs <= 24 * 60 * 60 * 1000L)
        }
    }
}
