package io.github.natnael_dev.dailyread.domain.tracker

import io.github.natnael_dev.dailyread.domain.time.Clock
import io.github.natnael_dev.dailyread.domain.time.LogicalDay
import io.github.natnael_dev.dailyread.domain.time.LogicalDayCalculator
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/**
 * Provisional constants for reading session intervals and activity tracking.
 * PROVISIONAL: Calibration on physical Samsung Galaxy M12 scheduled for later phase.
 */
object ReadingTrackerConstants {
    const val MIN_DWELL_SECONDS = 5L
    const val MAX_FORWARD_STEP = 5
    const val INACTIVITY_CAP_SECONDS = 60L
    const val FLUSH_INTERVAL_SECONDS = 30L
}

/**
 * Completed reading interval ready to be appended to the append-only reading ledger.
 */
data class TrackedInterval(
    val contentKey: String,
    val titleSnapshot: String,
    val startedAt: Instant,
    val endedAt: Instant,
    val zoneId: ZoneId,
    val utcOffsetSeconds: Int,
    val logicalDay: LogicalDay,
    val activeSeconds: Long,
    val positionsCounted: Int,
    val distinctPositions: Set<Int>,
    val elapsedRealtimeStart: Long,
    val clockSuspect: Boolean
)

/**
 * ReadingTracker: Active session time and position counting engine.
 *
 * Rules:
 * - Active time only while reader foregrounded + screen on + recent touch/page-turn.
 * - Provisional constants:
 *   - dwell >= 5s on position before moving forward.
 *   - forward step <= 5 positions qualifies for position counting.
 *   - inactivity cap 60s: idle time beyond 60s without interaction does not count as active.
 *   - backward moves and large jumps (>5 positions) earn zero positions.
 * - Flush triggers:
 *   - active time >= 30s.
 *   - on pause / stop.
 *   - crossing logical day boundary (split cleanly into distinct logical day intervals).
 */
class ReadingTracker(
    val contentKey: String,
    val titleSnapshot: String,
    private val clock: Clock,
    private val logicalDayCalculator: LogicalDayCalculator = LogicalDayCalculator(),
    val minDwellSeconds: Long = ReadingTrackerConstants.MIN_DWELL_SECONDS,
    val maxForwardStep: Int = ReadingTrackerConstants.MAX_FORWARD_STEP,
    val inactivityCapSeconds: Long = ReadingTrackerConstants.INACTIVITY_CAP_SECONDS,
    val flushIntervalSeconds: Long = ReadingTrackerConstants.FLUSH_INTERVAL_SECONDS
) {
    private var isSessionActive: Boolean = false
    private var currentPosition: Int = 0

    // Timing markers
    private var positionEnterInstant: Instant = Instant.EPOCH
    private var lastInteractionInstant: Instant = Instant.EPOCH
    private var lastInteractionElapsedRealtimeMs: Long = 0L
    private var intervalStartInstant: Instant = Instant.EPOCH
    private var intervalStartElapsedRealtimeMs: Long = 0L

    // Accumulators for current interval window
    private var accumulatedActiveSeconds: Long = 0L
    private val accumulatedPositions = mutableSetOf<Int>()

    fun isSessionActive(): Boolean = isSessionActive
    fun getCurrentPosition(): Int = currentPosition
    fun getAccumulatedActiveSeconds(): Long = accumulatedActiveSeconds
    fun getAccumulatedPositions(): Set<Int> = accumulatedPositions.toSet()

    /**
     * Start a new reading tracking session when book is opened or resumed in foreground.
     */
    fun startSession(initialPosition: Int, elapsedRealtimeMs: Long) {
        val now = clock.now()
        isSessionActive = true
        currentPosition = initialPosition
        positionEnterInstant = now
        lastInteractionInstant = now
        lastInteractionElapsedRealtimeMs = elapsedRealtimeMs
        intervalStartInstant = now
        intervalStartElapsedRealtimeMs = elapsedRealtimeMs
        accumulatedActiveSeconds = 0L
        accumulatedPositions.clear()
    }

    /**
     * Called on user touch, scroll, swipe, or screen tap in reader.
     */
    fun onUserInteraction(elapsedRealtimeMs: Long) {
        if (!isSessionActive) return
        val now = clock.now()
        accumulateActiveTime(now, elapsedRealtimeMs)
        lastInteractionInstant = now
        lastInteractionElapsedRealtimeMs = elapsedRealtimeMs
    }

    /**
     * Called when locator or page position advances in the reader engine.
     */
    fun onPositionChanged(newPosition: Int, elapsedRealtimeMs: Long) {
        if (!isSessionActive) return
        val now = clock.now()

        // 1. Accumulate active time up to now
        accumulateActiveTime(now, elapsedRealtimeMs)

        // 2. Evaluate dwell on the position being vacated
        val dwellDuration = Duration.between(positionEnterInstant, now)
        val dwellSeconds = if (dwellDuration.isNegative) {
            // If clock jumped backwards, fall back to monotonic elapsed delta
            (elapsedRealtimeMs - lastInteractionElapsedRealtimeMs) / 1000L
        } else {
            dwellDuration.seconds
        }

        // 3. Evaluate position advancement rules
        val step = newPosition - currentPosition
        if (step in 1..maxForwardStep && dwellSeconds >= minDwellSeconds) {
            // Qualifies for counting! Add the vacated position
            accumulatedPositions.add(currentPosition)
        } else {
            // Backward move (step < 0) or large jump (step > maxForwardStep or step == 0) or dwell < 5s:
            // "backward moves and large jumps earn nothing"
        }

        // 4. Update markers
        currentPosition = newPosition
        positionEnterInstant = now
        lastInteractionInstant = now
        lastInteractionElapsedRealtimeMs = elapsedRealtimeMs
    }

    /**
     * Periodic tick (e.g. called on interval timer or before checking flush).
     */
    fun tick(elapsedRealtimeMs: Long) {
        if (!isSessionActive) return
        val now = clock.now()
        accumulateActiveTime(now, elapsedRealtimeMs)
    }

    /**
     * Checks if current interval has reached flush criteria.
     */
    fun shouldFlush(elapsedRealtimeMs: Long): Boolean {
        if (!isSessionActive) return false
        val now = clock.now()
        accumulateActiveTime(now, elapsedRealtimeMs)

        if (accumulatedActiveSeconds >= flushIntervalSeconds) {
            return true
        }

        // Also check if logical day has crossed since interval started
        val zoneId = clock.zoneId()
        val startDay = logicalDayCalculator.computeLogicalDay(intervalStartInstant, zoneId)
        val currentDay = logicalDayCalculator.computeLogicalDay(now, zoneId)
        return startDay != currentDay
    }

    /**
     * Flush accumulated active time and positions into completed TrackedIntervals.
     * Splits intervals crossing logical-day boundary (e.g. 03:00).
     */
    fun flush(elapsedRealtimeMs: Long, isPause: Boolean = false): List<TrackedInterval> {
        if (!isSessionActive) return emptyList()
        val now = clock.now()
        accumulateActiveTime(now, elapsedRealtimeMs)

        val elapsedDeltaMs = elapsedRealtimeMs - intervalStartElapsedRealtimeMs
        val isClockSuspect = logicalDayCalculator.isClockSuspect(
            wallClockStart = intervalStartInstant,
            wallClockEnd = now,
            elapsedRealtimeDeltaMs = elapsedDeltaMs
        )

        if (accumulatedActiveSeconds == 0L && accumulatedPositions.isEmpty() && !isClockSuspect) {
            if (isPause) {
                isSessionActive = false
            } else {
                intervalStartInstant = now
                intervalStartElapsedRealtimeMs = elapsedRealtimeMs
            }
            return emptyList()
        }

        val zoneId = clock.zoneId()

        // Split at logical-day boundary if interval spanned across boundary
        val rawSplit = logicalDayCalculator.splitIntervalAtBoundary(
            start = intervalStartInstant,
            end = if (now.isAfter(intervalStartInstant)) now else intervalStartInstant.plusSeconds(accumulatedActiveSeconds),
            zoneId = zoneId
        )

        val intervals = mutableListOf<TrackedInterval>()
        val totalSpanSeconds = if (rawSplit.isNotEmpty()) {
            rawSplit.sumOf { Duration.between(it.start, it.end).seconds }.coerceAtLeast(1L)
        } else 1L

        val distinctPositionsCopy = accumulatedPositions.toSet()

        if (rawSplit.size <= 1) {
            val logicalDay = logicalDayCalculator.computeLogicalDay(intervalStartInstant, zoneId)
            val zonedDateTime = intervalStartInstant.atZone(zoneId)
            intervals.add(
                TrackedInterval(
                    contentKey = contentKey,
                    titleSnapshot = titleSnapshot,
                    startedAt = intervalStartInstant,
                    endedAt = now,
                    zoneId = zoneId,
                    utcOffsetSeconds = zonedDateTime.offset.totalSeconds,
                    logicalDay = logicalDay,
                    activeSeconds = accumulatedActiveSeconds,
                    positionsCounted = distinctPositionsCopy.size,
                    distinctPositions = distinctPositionsCopy,
                    elapsedRealtimeStart = intervalStartElapsedRealtimeMs,
                    clockSuspect = isClockSuspect
                )
            )
        } else {
            // Distribute active seconds proportionally across the split boundaries
            var remainingActiveSeconds = accumulatedActiveSeconds
            rawSplit.forEachIndexed { index, split ->
                val fraction = Duration.between(split.start, split.end).seconds.toDouble() / totalSpanSeconds
                val segmentActiveSeconds = if (index == rawSplit.lastIndex) {
                    remainingActiveSeconds
                } else {
                    (accumulatedActiveSeconds * fraction).toLong().coerceAtMost(remainingActiveSeconds)
                }
                remainingActiveSeconds -= segmentActiveSeconds

                // Positions attributed to the segment in which they occurred (or first segment)
                val segmentPositions = if (index == 0) distinctPositionsCopy else emptySet()

                intervals.add(
                    TrackedInterval(
                        contentKey = contentKey,
                        titleSnapshot = titleSnapshot,
                        startedAt = split.start,
                        endedAt = split.end,
                        zoneId = zoneId,
                        utcOffsetSeconds = split.utcOffsetSeconds,
                        logicalDay = split.logicalDay,
                        activeSeconds = segmentActiveSeconds,
                        positionsCounted = segmentPositions.size,
                        distinctPositions = segmentPositions,
                        elapsedRealtimeStart = intervalStartElapsedRealtimeMs,
                        clockSuspect = isClockSuspect
                    )
                )
            }
        }

        // Reset accumulators
        accumulatedActiveSeconds = 0L
        accumulatedPositions.clear()
        intervalStartInstant = now
        intervalStartElapsedRealtimeMs = elapsedRealtimeMs

        if (isPause) {
            isSessionActive = false
        }

        return intervals
    }

    /**
     * Pause reading session on reader backgrounding / lifecycle stop.
     */
    fun pauseSession(elapsedRealtimeMs: Long): List<TrackedInterval> {
        return flush(elapsedRealtimeMs, isPause = true)
    }

    /**
     * Accumulates active time since last interaction, respecting the inactivity cap.
     * Preserves monotonic elapsed delta when wall-clock jumps backwards (SC-19).
     */
    private fun accumulateActiveTime(now: Instant, elapsedRealtimeMs: Long) {
        val durationSinceInteraction = Duration.between(lastInteractionInstant, now)
        val secondsSinceInteraction = if (durationSinceInteraction.isNegative) {
            // Backward wall-clock jump: preserve monotonic elapsed delta with zero penalty (SC-19)
            val monotonicDeltaMs = elapsedRealtimeMs - lastInteractionElapsedRealtimeMs
            if (monotonicDeltaMs > 0) monotonicDeltaMs / 1000L else 0L
        } else {
            durationSinceInteraction.seconds
        }

        val creditableSeconds = secondsSinceInteraction.coerceAtMost(inactivityCapSeconds)

        // Only add incremental active seconds since last check
        if (creditableSeconds > 0) {
            accumulatedActiveSeconds += creditableSeconds
            lastInteractionInstant = now
            lastInteractionElapsedRealtimeMs = elapsedRealtimeMs
        }
    }
}
