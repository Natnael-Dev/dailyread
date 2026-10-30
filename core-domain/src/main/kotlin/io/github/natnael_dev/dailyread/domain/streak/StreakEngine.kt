package io.github.natnael_dev.dailyread.domain.streak

import io.github.natnael_dev.dailyread.domain.time.LogicalDay
import kotlin.math.max

data class QualifyingDay(
    val logicalDay: LogicalDay,
    val utcOffsetSeconds: Int
) : Comparable<QualifyingDay> {
    override fun compareTo(other: QualifyingDay): Int = logicalDay.compareTo(other.logicalDay)
}

enum class DayStatus {
    QUALIFIED,
    FROZEN,
    MISSED,
    TODAY_IN_PROGRESS,
    TODAY_QUALIFIED,
    FUTURE_BLANK
}

data class StreakResult(
    val currentStreak: Int,
    val bestStreak: Int,
    val freezesHeld: Int,
    val todayStatus: DayStatus,
    val isAtRisk: Boolean,
    val isProtected: Boolean,
    val frozenDays: Set<LogicalDay>,
    val dayStatusMap: Map<LogicalDay, DayStatus>
)

class StreakEngine {

    companion object {
        const val MAX_FREEZES = 2
        const val DAYS_PER_FREEZE = 7
    }

    fun calculateStreak(
        qualifyingDaysInput: List<QualifyingDay>,
        today: LogicalDay,
        currentZoneOffsetSeconds: Int
    ): StreakResult {
        val qualifyingDays = qualifyingDaysInput
            .distinctBy { it.logicalDay }
            .sorted()

        if (qualifyingDays.isEmpty()) {
            return StreakResult(
                currentStreak = 0,
                bestStreak = 0,
                freezesHeld = 0,
                todayStatus = DayStatus.TODAY_IN_PROGRESS,
                isAtRisk = false,
                isProtected = false,
                frozenDays = emptySet(),
                dayStatusMap = mapOf(today to DayStatus.TODAY_IN_PROGRESS)
            )
        }

        var run = 0
        var freezes = 0
        var best = 0
        val frozenDays = mutableSetOf<LogicalDay>()
        val dayStatusMap = mutableMapOf<LogicalDay, DayStatus>()

        // Process all qualifying days strictly in chronological order
        for (i in qualifyingDays.indices) {
            val cur = qualifyingDays[i]

            if (i == 0) {
                run = 1
                if (run % DAYS_PER_FREEZE == 0 && freezes < MAX_FREEZES) {
                    freezes++
                }
                best = max(best, run)
                dayStatusMap[cur.logicalDay] = DayStatus.QUALIFIED
            } else {
                val prev = qualifyingDays[i - 1]
                var gap = (prev.logicalDay.daysUntil(cur.logicalDay) - 1).toInt()

                // Travel grace (DEC-025): if recorded UTC offset changed, forgive 1 missed day
                if (cur.utcOffsetSeconds != prev.utcOffsetSeconds) {
                    gap = max(0, gap - 1)
                }

                if (gap == 0) {
                    run++
                } else if (freezes >= gap) {
                    // Freezes cover the whole gap (DEC-024)
                    freezes -= gap
                    // Mark intermediate calendar days as frozen
                    var d = prev.logicalDay.plusDays(1)
                    while (d < cur.logicalDay) {
                        frozenDays.add(d)
                        dayStatusMap[d] = DayStatus.FROZEN
                        d = d.plusDays(1)
                    }
                    run++
                } else {
                    // Gap not covered: streak breaks, run resets to 1, freezes are NOT spent (DEC-024)
                    var d = prev.logicalDay.plusDays(1)
                    while (d < cur.logicalDay) {
                        dayStatusMap[d] = DayStatus.MISSED
                        d = d.plusDays(1)
                    }
                    run = 1
                }

                // Earning freezes: after run += 1, if run is multiple of 7 and freezes < 2 (SC-05, SC-21)
                if (run % DAYS_PER_FREEZE == 0 && freezes < MAX_FREEZES) {
                    freezes++
                }
                best = max(best, run)
                dayStatusMap[cur.logicalDay] = DayStatus.QUALIFIED
            }
        }

        // Evaluate today state
        val lastQualifying = qualifyingDays.last()
        val todayIsQualifying = lastQualifying.logicalDay == today

        val currentStreak: Int
        val todayStatus: DayStatus
        val isAtRisk: Boolean
        val isProtected: Boolean

        if (todayIsQualifying) {
            currentStreak = run
            todayStatus = DayStatus.TODAY_QUALIFIED
            isAtRisk = false
            isProtected = false
            dayStatusMap[today] = DayStatus.TODAY_QUALIFIED
        } else if (lastQualifying.logicalDay > today) {
            // Evaluated with future data
            currentStreak = run
            todayStatus = DayStatus.TODAY_QUALIFIED
            isAtRisk = false
            isProtected = false
        } else {
            var gap = (lastQualifying.logicalDay.daysUntil(today) - 1).toInt()

            if (currentZoneOffsetSeconds != lastQualifying.utcOffsetSeconds) {
                gap = max(0, gap - 1)
            }

            if (gap == 0) {
                // Yesterday was qualifying, today in progress -> streak alive and at risk
                currentStreak = run
                todayStatus = DayStatus.TODAY_IN_PROGRESS
                isAtRisk = true
                isProtected = false
            } else if (freezes >= gap) {
                // Missed day(s) before today, but covered by freezes
                currentStreak = run
                todayStatus = DayStatus.TODAY_IN_PROGRESS
                isAtRisk = false
                isProtected = true
            } else {
                // Gap cannot be covered -> streak is 0
                currentStreak = 0
                todayStatus = DayStatus.TODAY_IN_PROGRESS
                isAtRisk = false
                isProtected = false
            }
            dayStatusMap[today] = DayStatus.TODAY_IN_PROGRESS
        }

        return StreakResult(
            currentStreak = currentStreak,
            bestStreak = best,
            freezesHeld = freezes,
            todayStatus = todayStatus,
            isAtRisk = isAtRisk,
            isProtected = isProtected,
            frozenDays = frozenDays,
            dayStatusMap = dayStatusMap
        )
    }
}
