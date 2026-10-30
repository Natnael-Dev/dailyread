package io.github.natnael_dev.dailyread.domain.time

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class LogicalDay(val date: LocalDate) : Comparable<LogicalDay> {
    fun toIsoString(): String = date.toString()

    fun plusDays(days: Long): LogicalDay = LogicalDay(date.plusDays(days))
    fun minusDays(days: Long): LogicalDay = LogicalDay(date.minusDays(days))

    fun daysUntil(other: LogicalDay): Long = ChronoUnit.DAYS.between(date, other.date)

    override fun compareTo(other: LogicalDay): Int = date.compareTo(other.date)

    override fun toString(): String = toIsoString()

    companion object {
        fun parse(isoString: String): LogicalDay = LogicalDay(LocalDate.parse(isoString))
        fun of(year: Int, month: Int, dayOfMonth: Int): LogicalDay =
            LogicalDay(LocalDate.of(year, month, dayOfMonth))
    }
}

data class SplitInterval(
    val logicalDay: LogicalDay,
    val start: Instant,
    val end: Instant,
    val zoneId: ZoneId,
    val utcOffsetSeconds: Int
)

class LogicalDayCalculator(
    val defaultDayStart: LocalTime = DEFAULT_DAY_START
) {
    companion object {
        val DEFAULT_DAY_START: LocalTime = LocalTime.of(3, 0)
    }

    fun computeLogicalDay(
        instant: Instant,
        zoneId: ZoneId,
        dayStart: LocalTime = defaultDayStart
    ): LogicalDay {
        val zonedDateTime = instant.atZone(zoneId)
        val localTime = zonedDateTime.toLocalTime()
        val localDate = zonedDateTime.toLocalDate()

        val logicalDate = if (localTime.isBefore(dayStart)) {
            localDate.minusDays(1)
        } else {
            localDate
        }
        return LogicalDay(logicalDate)
    }

    fun isFutureDated(eventInstant: Instant, now: Instant): Boolean {
        return eventInstant.isAfter(now)
    }

    /**
     * Flags backward wall clock jumps when monotonic/elapsedRealtime advanced normally (SC-19).
     */
    fun isClockSuspect(
        wallClockStart: Instant,
        wallClockEnd: Instant,
        elapsedRealtimeDeltaMs: Long
    ): Boolean {
        return elapsedRealtimeDeltaMs > 0 && wallClockEnd.isBefore(wallClockStart)
    }

    /**
     * Splits an interval crossing logical day boundaries (e.g. 03:00) into separate intervals.
     */
    fun splitIntervalAtBoundary(
        start: Instant,
        end: Instant,
        zoneId: ZoneId,
        dayStart: LocalTime = defaultDayStart
    ): List<SplitInterval> {
        if (!start.isBefore(end)) return emptyList()

        val results = mutableListOf<SplitInterval>()
        var cursor = start

        while (cursor.isBefore(end)) {
            val cursorZoned = cursor.atZone(zoneId)
            val currentLogicalDay = computeLogicalDay(cursor, zoneId, dayStart)

            // The next boundary instant is the dayStart time for (currentLogicalDay.date + 1 day)
            val nextBoundaryLocal = LocalDateTime.of(currentLogicalDay.date.plusDays(1), dayStart)
            val nextBoundaryInstant = nextBoundaryLocal.atZone(zoneId).toInstant()

            val segmentEnd = if (end.isBefore(nextBoundaryInstant)) end else nextBoundaryInstant
            val offsetSeconds = cursorZoned.offset.totalSeconds

            results.add(
                SplitInterval(
                    logicalDay = currentLogicalDay,
                    start = cursor,
                    end = segmentEnd,
                    zoneId = zoneId,
                    utcOffsetSeconds = offsetSeconds
                )
            )
            cursor = segmentEnd
        }

        return results
    }
}
