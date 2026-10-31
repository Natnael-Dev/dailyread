package io.github.natnael_dev.dailyread.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderCopySelectorTest {

    private val selector = ReminderCopySelector()

    @Test
    fun goalAlreadyMetSuppressesNotification() {
        val result = selector.selectReminderCopy(
            streakDays = 5,
            pagesReadToday = 10,
            dailyGoalPages = 10,
            activeBookTitle = "The Nature of Thought",
            consecutiveInactiveDays = 0,
            hourOfDay = 20,
            variant = 0
        )
        assertNull("Notification must be suppressed when daily goal is satisfied", result)
    }

    @Test
    fun consecutiveInactiveDaysThreeOrMoreSelectsSurrenderHook() {
        val result = selector.selectReminderCopy(
            streakDays = 0,
            pagesReadToday = 0,
            dailyGoalPages = 10,
            activeBookTitle = null,
            consecutiveInactiveDays = 3,
            hourOfDay = 20,
            variant = 0
        )
        assertNotNull(result)
        assertEquals(BanditTone.SURRENDER, result!!.tone)
        assertTrue(result.title.contains("stepping back", ignoreCase = true))
        assertTrue(result.message.contains("stop reminding", ignoreCase = true))
    }

    @Test
    fun urgentWindowWithActiveStreakSelectsUrgencyEscalation() {
        val result = selector.selectReminderCopy(
            streakDays = 14,
            pagesReadToday = 3,
            dailyGoalPages = 10,
            activeBookTitle = "Meditations",
            consecutiveInactiveDays = 0,
            hourOfDay = 1, // 01:00 AM (urgent rollover window)
            variant = 0
        )
        assertNotNull(result)
        assertEquals(BanditTone.URGENCY_ESCALATION, result!!.tone)
        assertTrue(result.message.contains("03:00", ignoreCase = true))
        assertTrue(result.message.contains("14-day streak", ignoreCase = true))
        assertTrue(result.message.contains("7 pages", ignoreCase = true)) // 10 - 3 = 7
    }

    @Test
    fun lossAversionCopyFormatsStreakAndRemainingPages() {
        val result = selector.selectReminderCopy(
            streakDays = 7,
            pagesReadToday = 2,
            dailyGoalPages = 10,
            activeBookTitle = "The Republic",
            consecutiveInactiveDays = 0,
            hourOfDay = 20,
            forcedTone = BanditTone.LOSS_AVERSION
        )
        assertNotNull(result)
        assertEquals(BanditTone.LOSS_AVERSION, result!!.tone)
        assertTrue(result.message.contains("7-day streak", ignoreCase = true))
        assertTrue(result.message.contains("8 pages", ignoreCase = true))
    }

    @Test
    fun contentCuriosityIncludesBookTitleWhenAvailable() {
        val result = selector.selectReminderCopy(
            streakDays = 3,
            pagesReadToday = 0,
            dailyGoalPages = 10,
            activeBookTitle = "War and Peace",
            consecutiveInactiveDays = 0,
            hourOfDay = 19,
            forcedTone = BanditTone.CONTENT_CURIOSITY
        )
        assertNotNull(result)
        assertEquals(BanditTone.CONTENT_CURIOSITY, result!!.tone)
        assertTrue(result.message.contains("War and Peace", ignoreCase = true))
    }

    @Test
    fun banditRotationProducesDistinctTonesAcrossVariants() {
        val tone0 = selector.selectReminderCopy(
            streakDays = 4,
            pagesReadToday = 0,
            dailyGoalPages = 10,
            activeBookTitle = "Book A",
            consecutiveInactiveDays = 0,
            hourOfDay = 19,
            variant = 0
        )!!.tone

        val tone1 = selector.selectReminderCopy(
            streakDays = 4,
            pagesReadToday = 0,
            dailyGoalPages = 10,
            activeBookTitle = "Book A",
            consecutiveInactiveDays = 0,
            hourOfDay = 19,
            variant = 1
        )!!.tone

        val tone2 = selector.selectReminderCopy(
            streakDays = 4,
            pagesReadToday = 0,
            dailyGoalPages = 10,
            activeBookTitle = "Book A",
            consecutiveInactiveDays = 0,
            hourOfDay = 19,
            variant = 2
        )!!.tone

        val tones = setOf(tone0, tone1, tone2)
        assertEquals("Variants 0, 1, 2 must rotate across distinct behavioral tones", 3, tones.size)
    }
}
