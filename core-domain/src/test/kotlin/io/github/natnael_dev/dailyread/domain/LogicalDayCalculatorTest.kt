package io.github.natnael_dev.dailyread.domain

import io.github.natnael_dev.dailyread.domain.time.LogicalDay
import io.github.natnael_dev.dailyread.domain.time.LogicalDayCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class LogicalDayCalculatorTest {

    private val calculator = LogicalDayCalculator()
    private val zoneId = ZoneId.of("Africa/Addis_Ababa") // UTC+3

    @Test
    fun `SC-10 Reading at 02-59 vs 03-01 local assigns correct logical day`() {
        val dt259 = LocalDateTime.of(2026, 10, 15, 2, 59, 0)
        val instant259 = dt259.atZone(zoneId).toInstant()

        val dt301 = LocalDateTime.of(2026, 10, 15, 3, 1, 0)
        val instant301 = dt301.atZone(zoneId).toInstant()

        val day259 = calculator.computeLogicalDay(instant259, zoneId)
        val day301 = calculator.computeLogicalDay(instant301, zoneId)

        // 02:59 belongs to previous logical day (Oct 14)
        assertEquals(LogicalDay.parse("2026-10-14"), day259)
        // 03:01 belongs to current logical day (Oct 15)
        assertEquals(LogicalDay.parse("2026-10-15"), day301)
    }

    @Test
    fun `SC-11 Interval 02-59 to 03-05 is split at 03-00 boundary`() {
        val startLocal = LocalDateTime.of(2026, 10, 15, 2, 59, 0)
        val endLocal = LocalDateTime.of(2026, 10, 15, 3, 5, 0)

        val start = startLocal.atZone(zoneId).toInstant()
        val end = endLocal.atZone(zoneId).toInstant()

        val split = calculator.splitIntervalAtBoundary(start, end, zoneId)

        assertEquals(2, split.size)

        // Segment 1: 02:59 to 03:00 (Logical day: 2026-10-14)
        assertEquals(LogicalDay.parse("2026-10-14"), split[0].logicalDay)
        assertEquals(start, split[0].start)
        val boundaryInstant = LocalDateTime.of(2026, 10, 15, 3, 0, 0).atZone(zoneId).toInstant()
        assertEquals(boundaryInstant, split[0].end)

        // Segment 2: 03:00 to 03:05 (Logical day: 2026-10-15)
        assertEquals(LogicalDay.parse("2026-10-15"), split[1].logicalDay)
        assertEquals(boundaryInstant, split[1].start)
        assertEquals(end, split[1].end)
    }

    @Test
    fun `SC-12 Interval dated after now is identified as future dated`() {
        val now = Instant.parse("2026-10-15T12:00:00Z")
        val future = Instant.parse("2026-10-15T12:05:00Z")
        val past = Instant.parse("2026-10-15T11:55:00Z")

        assertTrue(calculator.isFutureDated(future, now))
        assertFalse(calculator.isFutureDated(past, now))
        assertFalse(calculator.isFutureDated(now, now))
    }

    @Test
    fun `SC-20 Changing dayStart from 03-00 to 05-00 applies cleanly`() {
        val dt0400 = LocalDateTime.of(2026, 10, 15, 4, 0, 0)
        val instant0400 = dt0400.atZone(zoneId).toInstant()

        val dayWith0300 = calculator.computeLogicalDay(instant0400, zoneId, dayStart = LocalTime.of(3, 0))
        val dayWith0500 = calculator.computeLogicalDay(instant0400, zoneId, dayStart = LocalTime.of(5, 0))

        // Under 03:00 start, 04:00 is on Oct 15
        assertEquals(LogicalDay.parse("2026-10-15"), dayWith0300)
        // Under 05:00 start, 04:00 is on Oct 14
        assertEquals(LogicalDay.parse("2026-10-14"), dayWith0500)
    }
}
