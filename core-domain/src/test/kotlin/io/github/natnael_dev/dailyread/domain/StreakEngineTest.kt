package io.github.natnael_dev.dailyread.domain

import io.github.natnael_dev.dailyread.domain.streak.DayStatus
import io.github.natnael_dev.dailyread.domain.streak.QualifyingDay
import io.github.natnael_dev.dailyread.domain.streak.StreakEngine
import io.github.natnael_dev.dailyread.domain.time.LogicalDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreakEngineTest {

    private val engine = StreakEngine()
    private val defaultOffset = 10800 // UTC+3 (3 hours = 10800 seconds)

    private fun qDay(dayStr: String, offset: Int = defaultOffset): QualifyingDay =
        QualifyingDay(LogicalDay.parse(dayStr), offset)

    @Test
    fun `SC-01 First ever qualifying day`() {
        val today = LogicalDay.parse("2026-10-01")
        val result = engine.calculateStreak(
            qualifyingDaysInput = listOf(qDay("2026-10-01")),
            today = today,
            currentZoneOffsetSeconds = defaultOffset
        )

        assertEquals(1, result.currentStreak)
        assertEquals(1, result.bestStreak)
        assertEquals(0, result.freezesHeld)
        assertEquals(DayStatus.TODAY_QUALIFIED, result.todayStatus)
        assertFalse(result.isAtRisk)
        assertFalse(result.isProtected)
    }

    @Test
    fun `SC-02 Three consecutive qualifying days`() {
        val today = LogicalDay.parse("2026-10-03")
        val days = listOf(
            qDay("2026-10-01"),
            qDay("2026-10-02"),
            qDay("2026-10-03")
        )
        val result = engine.calculateStreak(days, today, defaultOffset)

        assertEquals(3, result.currentStreak)
        assertEquals(3, result.bestStreak)
        assertEquals(0, result.freezesHeld)
        assertEquals(DayStatus.TODAY_QUALIFIED, result.todayStatus)
    }

    @Test
    fun `SC-03 Yesterday qualified, today not yet - streak alive and at risk`() {
        val today = LogicalDay.parse("2026-10-04")
        val days = listOf(
            qDay("2026-10-01"),
            qDay("2026-10-02"),
            qDay("2026-10-03")
        )
        val result = engine.calculateStreak(days, today, defaultOffset)

        assertEquals(3, result.currentStreak)
        assertEquals(3, result.bestStreak)
        assertEquals(DayStatus.TODAY_IN_PROGRESS, result.todayStatus)
        assertTrue(result.isAtRisk)
        assertFalse(result.isProtected)
    }

    @Test
    fun `SC-04 One missed day with zero freezes resets run but preserves best`() {
        val today = LogicalDay.parse("2026-10-05")
        // Qualified on 1st, 2nd, 3rd. Missed 4th. Qualified on 5th.
        val days = listOf(
            qDay("2026-10-01"),
            qDay("2026-10-02"),
            qDay("2026-10-03"),
            qDay("2026-10-05")
        )
        val result = engine.calculateStreak(days, today, defaultOffset)

        assertEquals(1, result.currentStreak)
        assertEquals(3, result.bestStreak)
        assertEquals(0, result.freezesHeld)
        assertEquals(DayStatus.TODAY_QUALIFIED, result.todayStatus)
    }

    @Test
    fun `SC-05 Freezes earned at 7, 14, 21 consecutive days and capped at 2`() {
        // 7 consecutive days
        val days7 = (1..7).map { qDay(String.format("2026-10-%02d", it)) }
        val r7 = engine.calculateStreak(days7, LogicalDay.parse("2026-10-07"), defaultOffset)
        assertEquals(7, r7.currentStreak)
        assertEquals(1, r7.freezesHeld)

        // 14 consecutive days
        val days14 = (1..14).map { qDay(String.format("2026-10-%02d", it)) }
        val r14 = engine.calculateStreak(days14, LogicalDay.parse("2026-10-14"), defaultOffset)
        assertEquals(14, r14.currentStreak)
        assertEquals(2, r14.freezesHeld)

        // 21 consecutive days (capped at 2)
        val days21 = (1..21).map { qDay(String.format("2026-10-%02d", it)) }
        val r21 = engine.calculateStreak(days21, LogicalDay.parse("2026-10-21"), defaultOffset)
        assertEquals(21, r21.currentStreak)
        assertEquals(2, r21.freezesHeld)
    }

    @Test
    fun `SC-06 After 7 days, one missed day, then qualifying day spends freeze and continues`() {
        // Qualified 1..7 (earned 1 freeze). Missed 8. Qualified 9.
        val days = (1..7).map { qDay(String.format("2026-10-%02d", it)) } + listOf(qDay("2026-10-09"))
        val today = LogicalDay.parse("2026-10-09")
        val result = engine.calculateStreak(days, today, defaultOffset)

        assertEquals(8, result.currentStreak)
        assertEquals(8, result.bestStreak)
        assertEquals(0, result.freezesHeld) // 1 freeze spent
        assertTrue(result.frozenDays.contains(LogicalDay.parse("2026-10-08")))
        assertEquals(DayStatus.FROZEN, result.dayStatusMap[LogicalDay.parse("2026-10-08")])
    }

    @Test
    fun `SC-07 One freeze held, two missed days - streak breaks and freeze is kept`() {
        // Qualified 1..7 (earned 1 freeze). Missed 8 and 9. Qualified 10.
        val days = (1..7).map { qDay(String.format("2026-10-%02d", it)) } + listOf(qDay("2026-10-10"))
        val today = LogicalDay.parse("2026-10-10")
        val result = engine.calculateStreak(days, today, defaultOffset)

        assertEquals(1, result.currentStreak) // Reset to 1
        assertEquals(7, result.bestStreak) // Best preserved
        assertEquals(1, result.freezesHeld) // Freeze not spent because gap was 2 > 1
        assertEquals(DayStatus.MISSED, result.dayStatusMap[LogicalDay.parse("2026-10-08")])
        assertEquals(DayStatus.MISSED, result.dayStatusMap[LogicalDay.parse("2026-10-09")])
    }

    @Test
    fun `SC-08 Two freezes held, two missed days - continues with zero freezes left`() {
        // Qualified 1..14 (earned 2 freezes). Missed 15 and 16. Qualified 17.
        val days = (1..14).map { qDay(String.format("2026-10-%02d", it)) } + listOf(qDay("2026-10-17"))
        val today = LogicalDay.parse("2026-10-17")
        val result = engine.calculateStreak(days, today, defaultOffset)

        assertEquals(15, result.currentStreak)
        assertEquals(15, result.bestStreak)
        assertEquals(0, result.freezesHeld)
        assertTrue(result.frozenDays.contains(LogicalDay.parse("2026-10-15")))
        assertTrue(result.frozenDays.contains(LogicalDay.parse("2026-10-16")))
    }

    @Test
    fun `SC-09 One missed day, zero freezes, offset changed (+3 to +1) - travel grace forgives gap`() {
        val days = listOf(
            qDay("2026-10-01", offset = 10800), // UTC+3
            qDay("2026-10-03", offset = 3600)   // UTC+1 (travel event, missed 2026-10-02)
        )
        val today = LogicalDay.parse("2026-10-03")
        val result = engine.calculateStreak(days, today, currentZoneOffsetSeconds = 3600)

        assertEquals(2, result.currentStreak)
        assertEquals(2, result.bestStreak)
        assertEquals(0, result.freezesHeld)
    }

    @Test
    fun `SC-21 Freeze earned on 7th qualifying day, not 7th calendar day`() {
        // Start with 1..7 (earn freeze 1). Miss day 8.
        // Day 9 qualifies (spends freeze, run=8, freezes=0).
        // Next qualifying days: 10, 11, 12, 13, 14, 15 (run reaches 14).
        // 14th qualifying day earns freeze 1 (7th qualifying day after run was at 7).
        val days = (1..7).map { qDay(String.format("2026-10-%02d", it)) } +
                listOf(qDay("2026-10-09")) +
                (10..15).map { qDay(String.format("2026-10-%02d", it)) }

        val today = LogicalDay.parse("2026-10-15")
        val result = engine.calculateStreak(days, today, defaultOffset)

        assertEquals(14, result.currentStreak)
        assertEquals(1, result.freezesHeld) // Earned 1 freeze because run reached 14!
    }
}
