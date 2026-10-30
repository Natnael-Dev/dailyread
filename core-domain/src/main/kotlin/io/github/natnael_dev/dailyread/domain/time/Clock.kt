package io.github.natnael_dev.dailyread.domain.time

import java.time.Instant
import java.time.ZoneId

interface Clock {
    fun now(): Instant
    fun zoneId(): ZoneId
}

class SystemClock(
    private val defaultZoneId: ZoneId = ZoneId.systemDefault()
) : Clock {
    override fun now(): Instant = Instant.now()
    override fun zoneId(): ZoneId = defaultZoneId
}

class FakeClock(
    private var currentInstant: Instant,
    private var currentZoneId: ZoneId
) : Clock {
    override fun now(): Instant = currentInstant
    override fun zoneId(): ZoneId = currentZoneId

    fun set(instant: Instant, zoneId: ZoneId = currentZoneId) {
        currentInstant = instant
        currentZoneId = zoneId
    }

    fun advanceSeconds(seconds: Long) {
        currentInstant = currentInstant.plusSeconds(seconds)
    }
}
