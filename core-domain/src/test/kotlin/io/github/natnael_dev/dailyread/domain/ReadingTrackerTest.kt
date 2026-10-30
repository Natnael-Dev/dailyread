package io.github.natnael_dev.dailyread.domain

import io.github.natnael_dev.dailyread.domain.time.FakeClock
import io.github.natnael_dev.dailyread.domain.time.LogicalDay
import io.github.natnael_dev.dailyread.domain.time.LogicalDayCalculator
import io.github.natnael_dev.dailyread.domain.tracker.ReadingTracker
import io.github.natnael_dev.dailyread.domain.tracker.ReadingTrackerConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class ReadingTrackerTest {

    private val zoneId = ZoneId.of("Africa/Addis_Ababa") // UTC+3
    private lateinit var clock: FakeClock
    private lateinit var logicalDayCalculator: LogicalDayCalculator
    private lateinit var tracker: ReadingTracker

    @Before
    fun setup() {
        // Start at 2026-10-30 10:00:00 UTC (13:00:00 local, well inside daytime)
        clock = FakeClock(Instant.parse("2026-10-30T10:00:00Z"), zoneId)
        logicalDayCalculator = LogicalDayCalculator()
        tracker = ReadingTracker(
            contentKey = "book-sha256-test",
            titleSnapshot = "Walden",
            clock = clock,
            logicalDayCalculator = logicalDayCalculator
        )
    }

    @Test
    fun `SC-TRK-01 Qualifying dwell and forward step counts position and active time`() {
        tracker.startSession(initialPosition = 1, elapsedRealtimeMs = 10_000L)

        // Dwell on position 1 for 15 seconds (>= 5s provisional constant)
        clock.advanceSeconds(15)
        tracker.onPositionChanged(newPosition = 2, elapsedRealtimeMs = 25_000L)

        assertEquals(1, tracker.getAccumulatedPositions().size)
        assertTrue(tracker.getAccumulatedPositions().contains(1))
        assertEquals(15L, tracker.getAccumulatedActiveSeconds())
        assertEquals(2, tracker.getCurrentPosition())
    }

    @Test
    fun `SC-TRK-02 Dwell under 5s does not count position`() {
        tracker.startSession(initialPosition = 1, elapsedRealtimeMs = 10_000L)

        // Dwell for only 3 seconds (< 5s provisional constant)
        clock.advanceSeconds(3)
        tracker.onPositionChanged(newPosition = 2, elapsedRealtimeMs = 13_000L)

        assertEquals(0, tracker.getAccumulatedPositions().size)
        assertEquals(3L, tracker.getAccumulatedActiveSeconds()) // Active time is credited, but position is not
        assertEquals(2, tracker.getCurrentPosition())
    }

    @Test
    fun `SC-TRK-03 Forward jump exceeding 5 positions earns zero positions`() {
        tracker.startSession(initialPosition = 10, elapsedRealtimeMs = 10_000L)

        // Dwell for 20 seconds, then jump forward 8 positions (10 -> 18, > 5)
        clock.advanceSeconds(20)
        tracker.onPositionChanged(newPosition = 18, elapsedRealtimeMs = 30_000L)

        assertEquals(0, tracker.getAccumulatedPositions().size)
        assertEquals(18, tracker.getCurrentPosition())
    }

    @Test
    fun `SC-TRK-04 Backward page turn earns zero positions`() {
        tracker.startSession(initialPosition = 15, elapsedRealtimeMs = 10_000L)

        // Dwell for 10 seconds, then navigate backward (15 -> 14)
        clock.advanceSeconds(10)
        tracker.onPositionChanged(newPosition = 14, elapsedRealtimeMs = 20_000L)

        assertEquals(0, tracker.getAccumulatedPositions().size)
        assertEquals(14, tracker.getCurrentPosition())
    }

    @Test
    fun `SC-TRK-05 Inactivity exceeding 60s caps active reading time at 60s`() {
        tracker.startSession(initialPosition = 1, elapsedRealtimeMs = 10_000L)

        // User reads, leaves device unattended for 5 minutes (300 seconds)
        clock.advanceSeconds(300)
        tracker.onUserInteraction(elapsedRealtimeMs = 310_000L)

        // Active time should be capped at 60s (ReadingTrackerConstants.INACTIVITY_CAP_SECONDS)
        assertEquals(ReadingTrackerConstants.INACTIVITY_CAP_SECONDS, tracker.getAccumulatedActiveSeconds())
    }

    @Test
    fun `SC-TRK-06 Flush condition triggers when accumulated active time reaches 30s`() {
        tracker.startSession(initialPosition = 1, elapsedRealtimeMs = 10_000L)

        clock.advanceSeconds(25)
        tracker.onUserInteraction(elapsedRealtimeMs = 35_000L)
        assertFalse(tracker.shouldFlush(elapsedRealtimeMs = 35_000L))

        clock.advanceSeconds(6)
        tracker.onUserInteraction(elapsedRealtimeMs = 41_000L)
        assertTrue(tracker.shouldFlush(elapsedRealtimeMs = 41_000L))

        val intervals = tracker.flush(elapsedRealtimeMs = 41_000L)
        assertEquals(1, intervals.size)
        assertEquals(31L, intervals[0].activeSeconds)
        assertEquals("Walden", intervals[0].titleSnapshot)
        assertEquals("book-sha256-test", intervals[0].contentKey)

        // Accumulator reset
        assertEquals(0L, tracker.getAccumulatedActiveSeconds())
        assertFalse(tracker.shouldFlush(elapsedRealtimeMs = 41_000L))
    }

    @Test
    fun `SC-TRK-07 PauseSession flushes interval and terminates session`() {
        tracker.startSession(initialPosition = 5, elapsedRealtimeMs = 10_000L)

        clock.advanceSeconds(12)
        tracker.onPositionChanged(newPosition = 6, elapsedRealtimeMs = 22_000L)

        val intervals = tracker.pauseSession(elapsedRealtimeMs = 22_000L)
        assertEquals(1, intervals.size)
        assertEquals(12L, intervals[0].activeSeconds)
        assertEquals(1, intervals[0].positionsCounted)
        assertFalse(tracker.isSessionActive())
    }

    @Test
    fun `SC-TRK-08 Interval crossing 03-00 logical day boundary splits cleanly into two intervals`() {
        // Set clock to 02:50 local (23:50 UTC prior day)
        // In UTC+3, 03:00 local is 00:00 UTC
        val preBoundaryInstant = Instant.parse("2026-10-29T23:50:00Z") // 02:50 local
        clock.set(preBoundaryInstant, zoneId)

        tracker.startSession(initialPosition = 20, elapsedRealtimeMs = 10_000L)

        // Read for 60 seconds (10 mins clock span: from 02:50 to 03:05 local)
        clock.advanceSeconds(900) // 15 minutes span across 03:00 boundary
        tracker.onUserInteraction(elapsedRealtimeMs = 910_000L)

        assertTrue(tracker.shouldFlush(elapsedRealtimeMs = 910_000L))
        val intervals = tracker.flush(elapsedRealtimeMs = 910_000L)

        assertEquals(2, intervals.size)
        // Interval 1 belongs to logical day 2026-10-29
        assertEquals(LogicalDay.parse("2026-10-29"), intervals[0].logicalDay)
        // Interval 2 belongs to logical day 2026-10-30
        assertEquals(LogicalDay.parse("2026-10-30"), intervals[1].logicalDay)

        // Total active seconds preserved across the split
        val totalActive = intervals.sumOf { it.activeSeconds }
        assertEquals(ReadingTrackerConstants.INACTIVITY_CAP_SECONDS, totalActive)
    }

    @Test
    fun `SC-TRK-09 Backward wall-clock jump flags clockSuspect true and preserves monotonic active time`() {
        tracker.startSession(initialPosition = 1, elapsedRealtimeMs = 10_000L)

        // Realtime moves forward 20s, but wall clock is adjusted backward by 1 hour
        clock.set(Instant.parse("2026-10-30T09:00:00Z"), zoneId) // 1 hour earlier!

        val intervals = tracker.flush(elapsedRealtimeMs = 30_000L)
        assertEquals(1, intervals.size)
        assertTrue(intervals[0].clockSuspect)
        assertEquals(20L, intervals[0].activeSeconds) // SC-19 zero penalty
    }

    @Test
    fun `SC-TRK-10 Table-driven realistic reading sequence verification`() {
        data class ReaderStep(
            val delaySeconds: Long,
            val actionType: String, // "MOVE" or "TOUCH"
            val targetPosition: Int,
            val expectedAccumulatedPositions: Int,
            val expectedCurrentPosition: Int
        )

        val scenario = listOf(
            ReaderStep(delaySeconds = 10, actionType = "MOVE", targetPosition = 2, expectedAccumulatedPositions = 1, expectedCurrentPosition = 2), // dwell 10s >= 5s, step 1 -> counted
            ReaderStep(delaySeconds = 2, actionType = "MOVE", targetPosition = 3, expectedAccumulatedPositions = 1, expectedCurrentPosition = 3),  // dwell 2s < 5s, step 1 -> not counted
            ReaderStep(delaySeconds = 15, actionType = "MOVE", targetPosition = 4, expectedAccumulatedPositions = 2, expectedCurrentPosition = 4), // dwell 15s >= 5s, step 1 -> counted
            ReaderStep(delaySeconds = 20, actionType = "MOVE", targetPosition = 10, expectedAccumulatedPositions = 2, expectedCurrentPosition = 10), // dwell 20s, step 6 > 5 -> jump rejected!
            ReaderStep(delaySeconds = 8, actionType = "MOVE", targetPosition = 9, expectedAccumulatedPositions = 2, expectedCurrentPosition = 9),  // dwell 8s, step -1 -> backward rejected!
            ReaderStep(delaySeconds = 12, actionType = "MOVE", targetPosition = 11, expectedAccumulatedPositions = 3, expectedCurrentPosition = 11) // dwell 12s, step 2 <= 5 -> counted!
        )

        tracker.startSession(initialPosition = 1, elapsedRealtimeMs = 0L)
        var elapsedRealtime = 0L

        for ((index, step) in scenario.withIndex()) {
            clock.advanceSeconds(step.delaySeconds)
            elapsedRealtime += step.delaySeconds * 1000

            when (step.actionType) {
                "MOVE" -> tracker.onPositionChanged(step.targetPosition, elapsedRealtime)
                "TOUCH" -> tracker.onUserInteraction(elapsedRealtime)
            }

            assertEquals(
                "Step $index failed accumulated positions count",
                step.expectedAccumulatedPositions,
                tracker.getAccumulatedPositions().size
            )
            assertEquals(
                "Step $index failed current position",
                step.expectedCurrentPosition,
                tracker.getCurrentPosition()
            )
        }
    }
}
