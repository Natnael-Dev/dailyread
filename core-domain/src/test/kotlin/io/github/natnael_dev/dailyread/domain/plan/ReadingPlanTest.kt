package io.github.natnael_dev.dailyread.domain.plan

import io.github.natnael_dev.dailyread.domain.time.LogicalDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingPlanTest {

    @Test
    fun `plan starting today calculates correct daily quota and on track`() {
        val startDay = LogicalDay.parse("2026-10-01")
        val targetDay = LogicalDay.parse("2026-10-10") // 10 days total (Oct 1 to Oct 10 inclusive)
        val plan = ReadingPlan(
            id = 1,
            bookId = 42,
            startDay = startDay,
            targetDay = targetDay,
            startUnits = 0,
            totalUnits = 100
        )

        // Day 1 (today is startDay)
        val statusDay1 = ReadingPlanCalculator.calculateStatus(
            plan = plan,
            today = startDay,
            currentUnits = 0
        )

        // 100 units / 10 days = 10 units/day
        assertEquals(10, statusDay1.dailyQuota)
        assertEquals(10L, statusDay1.remainingDays)
        assertEquals(100, statusDay1.remainingUnits)
        assertEquals(0, statusDay1.expectedUnitsByToday)
        assertTrue(statusDay1.isOnTrack)
        assertEquals(0, statusDay1.progressPercent)
    }

    @Test
    fun `midway plan on track when reading ahead of schedule`() {
        val startDay = LogicalDay.parse("2026-10-01")
        val targetDay = LogicalDay.parse("2026-10-10") // 10 days
        val plan = ReadingPlan(
            id = 1,
            bookId = 42,
            startDay = startDay,
            targetDay = targetDay,
            startUnits = 0,
            totalUnits = 100
        )

        // Day 5 (Oct 5): elapsed 4 days, remaining 6 days (Oct 5..10 inclusive)
        // User has read 50 pages (expected ~40 pages)
        val statusDay5 = ReadingPlanCalculator.calculateStatus(
            plan = plan,
            today = LogicalDay.parse("2026-10-05"),
            currentUnits = 50
        )

        // 50 remaining units / 6 days = ceil(8.33) = 9 pages/day
        assertEquals(9, statusDay5.dailyQuota)
        assertEquals(6L, statusDay5.remainingDays)
        assertEquals(50, statusDay5.remainingUnits)
        assertEquals(40, statusDay5.expectedUnitsByToday)
        assertTrue(statusDay5.isOnTrack)
        assertEquals(50, statusDay5.progressPercent)
    }

    @Test
    fun `midway plan behind when reading lags expected progress`() {
        val startDay = LogicalDay.parse("2026-10-01")
        val targetDay = LogicalDay.parse("2026-10-10") // 10 days
        val plan = ReadingPlan(
            id = 1,
            bookId = 42,
            startDay = startDay,
            targetDay = targetDay,
            startUnits = 0,
            totalUnits = 100
        )

        // Day 6 (Oct 6): elapsed 5 days, expected 50 pages. User only read 20 pages.
        val statusDay6 = ReadingPlanCalculator.calculateStatus(
            plan = plan,
            today = LogicalDay.parse("2026-10-06"),
            currentUnits = 20
        )

        // 80 remaining units / 5 days = 16 pages/day
        assertEquals(16, statusDay6.dailyQuota)
        assertEquals(5L, statusDay6.remainingDays)
        assertEquals(80, statusDay6.remainingUnits)
        assertEquals(50, statusDay6.expectedUnitsByToday)
        assertFalse(statusDay6.isOnTrack)
        assertEquals(20, statusDay6.progressPercent)
    }

    @Test
    fun `completed book has zero quota and 100 percent progress`() {
        val startDay = LogicalDay.parse("2026-10-01")
        val targetDay = LogicalDay.parse("2026-10-10")
        val plan = ReadingPlan(
            id = 1,
            bookId = 42,
            startDay = startDay,
            targetDay = targetDay,
            startUnits = 0,
            totalUnits = 100
        )

        val statusCompleted = ReadingPlanCalculator.calculateStatus(
            plan = plan,
            today = LogicalDay.parse("2026-10-08"),
            currentUnits = 100
        )

        assertEquals(0, statusCompleted.dailyQuota)
        assertEquals(0, statusCompleted.remainingUnits)
        assertTrue(statusCompleted.isOnTrack)
        assertEquals(100, statusCompleted.progressPercent)
    }

    @Test
    fun `plan starting with non-zero startUnits correctly calculates delta`() {
        val startDay = LogicalDay.parse("2026-10-01")
        val targetDay = LogicalDay.parse("2026-10-05") // 5 days (Oct 1..5)
        // Book has 300 pages, but user started plan at page 100 -> 200 pages to read
        val plan = ReadingPlan(
            id = 1,
            bookId = 42,
            startDay = startDay,
            targetDay = targetDay,
            startUnits = 100,
            totalUnits = 300
        )

        val status = ReadingPlanCalculator.calculateStatus(
            plan = plan,
            today = startDay,
            currentUnits = 100
        )

        // 200 pages / 5 days = 40 pages/day
        assertEquals(40, status.dailyQuota)
        assertEquals(200, status.remainingUnits)
        assertEquals(5L, status.remainingDays)
        assertTrue(status.isOnTrack)
        assertEquals(33, status.progressPercent) // 100 / 300 = 33%
    }

    @Test
    fun `target day in past handles zero or negative days gracefully`() {
        val startDay = LogicalDay.parse("2026-10-01")
        val targetDay = LogicalDay.parse("2026-10-05")
        val plan = ReadingPlan(
            id = 1,
            bookId = 42,
            startDay = startDay,
            targetDay = targetDay,
            startUnits = 0,
            totalUnits = 100
        )

        // Today is past target day (Oct 10), 30 pages read
        val statusOverdue = ReadingPlanCalculator.calculateStatus(
            plan = plan,
            today = LogicalDay.parse("2026-10-10"),
            currentUnits = 30
        )

        assertEquals(0L, statusOverdue.remainingDays)
        assertEquals(70, statusOverdue.dailyQuota) // All remaining units
        assertFalse(statusOverdue.isOnTrack)
    }
}
