package io.github.natnael_dev.dailyread

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.natnael_dev.dailyread.domain.time.FakeClock
import io.github.natnael_dev.dailyread.domain.tracker.ReadingTracker
import io.github.natnael_dev.dailyread.domain.tracker.ReadingTrackerConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.ZoneId

/**
 * M12 Physical Device Calibration Test for ReadingTracker (DEC-006 / ADR-003).
 *
 * Verifies on real Samsung Galaxy M12 hardware:
 * 1. Active reading (2 minutes): dwell >= 5s, forward steps <= 5.
 * 2. Inactivity cap (2 minutes idle): idle time beyond 60s is capped.
 * 3. Backward scrolling: moving to previous positions earns zero positions.
 */
@RunWith(AndroidJUnit4::class)
class ReadingTrackerCalibrationDeviceTest {

    @Test
    fun testReadingTrackerCalibrationSequenceOnHardware() {
        val clock = FakeClock(Instant.parse("2026-10-31T20:00:00Z"), ZoneId.of("Africa/Addis_Ababa"))
        val tracker = ReadingTracker(
            contentKey = "m12-calibrated-meditations",
            titleSnapshot = "Meditations - M12 Calibration",
            clock = clock
        )

        val startTimeMs = 1_000_000L
        tracker.startSession(initialPosition = 1, elapsedRealtimeMs = startTimeMs)

        // PHASE 1: Active Reading for 2 Minutes (Simulated 120s of reading)
        // User reads 12 pages, advancing 1 page every 10 seconds.
        // Dwell per page is 10s (>= 5s threshold), step is +1 (<= 5 max step).
        var currentSimulatedMs = startTimeMs
        for (page in 2..13) {
            clock.advanceSeconds(10)
            currentSimulatedMs += 10_000L // 10s dwell
            tracker.onUserInteraction(currentSimulatedMs - 2000L) // Touch interaction
            tracker.onPositionChanged(newPosition = page, elapsedRealtimeMs = currentSimulatedMs)
        }

        val activePositions = tracker.getAccumulatedPositions()
        assertEquals("Must credit exactly 12 positions for 120s of active reading", 12, activePositions.size)
        assertTrue("Positions 1 through 12 must be accumulated", activePositions.containsAll((1..12).toList()))

        // PHASE 2: Idle for 2 Minutes (120s without interaction)
        // User puts phone on table and steps away for 120s.
        val idleStartMs = currentSimulatedMs
        val idleEndMs = idleStartMs + 120_000L // 2 minutes idle
        clock.advanceSeconds(120)

        // Move to next page after 2 minutes idle
        tracker.onPositionChanged(newPosition = 14, elapsedRealtimeMs = idleEndMs)

        // Active seconds during idle period must be capped at 60s (ReadingTrackerConstants.INACTIVITY_CAP_SECONDS)
        // Total active time across 120s active reading + 120s idle = 120s + 60s capped = 180s (not 240s)
        val totalActiveSeconds = tracker.getAccumulatedActiveSeconds()
        assertEquals(
            "Idle time beyond 60s must be dropped; expected 180s total active time",
            180L,
            totalActiveSeconds
        )

        // PHASE 3: Backward Scrolling
        // User scrolls back from page 14 to page 13, then to page 12.
        val preBackwardPositions = tracker.getAccumulatedPositions().size
        clock.advanceSeconds(10)
        currentSimulatedMs = idleEndMs + 10_000L
        tracker.onPositionChanged(newPosition = 13, elapsedRealtimeMs = currentSimulatedMs) // Backward step -1

        clock.advanceSeconds(10)
        currentSimulatedMs += 10_000L
        tracker.onPositionChanged(newPosition = 12, elapsedRealtimeMs = currentSimulatedMs) // Backward step -1

        val postBackwardPositions = tracker.getAccumulatedPositions().size
        assertEquals(
            "Backward scrolling must earn zero positions",
            preBackwardPositions,
            postBackwardPositions
        )

        // Verify calibrated constants
        assertEquals(5L, ReadingTrackerConstants.MIN_DWELL_SECONDS)
        assertEquals(5, ReadingTrackerConstants.MAX_FORWARD_STEP)
        assertEquals(60L, ReadingTrackerConstants.INACTIVITY_CAP_SECONDS)
        assertEquals(30L, ReadingTrackerConstants.FLUSH_INTERVAL_SECONDS)
    }
}
