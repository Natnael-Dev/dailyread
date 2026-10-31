package io.github.natnael_dev.dailyread.domain.plan

import io.github.natnael_dev.dailyread.domain.time.LogicalDay
import kotlin.math.ceil

/**
 * Domain representation of a reading plan for a specific book.
 */
data class ReadingPlan(
    val id: Long = 0,
    val bookId: Long,
    val startDay: LogicalDay,
    val targetDay: LogicalDay,
    val startUnits: Int,
    val totalUnits: Int,
    val state: PlanState = PlanState.ACTIVE
)

enum class PlanState {
    ACTIVE,
    DONE,
    DELETED
}

data class ReadingPlanStatus(
    val plan: ReadingPlan,
    val dailyQuota: Int,
    val remainingDays: Long,
    val remainingUnits: Int,
    val expectedUnitsByToday: Int,
    val actualUnits: Int,
    val isOnTrack: Boolean,
    val progressPercent: Int
)

object ReadingPlanCalculator {

    /**
     * Calculates the plan status given the plan, current logical day, and user's current progress in units.
     */
    fun calculateStatus(
        plan: ReadingPlan,
        today: LogicalDay,
        currentUnits: Int
    ): ReadingPlanStatus {
        val totalPlanUnits = (plan.totalUnits - plan.startUnits).coerceAtLeast(0)
        val remainingUnits = (plan.totalUnits - currentUnits).coerceAtLeast(0)

        val totalDays = (plan.startDay.daysUntil(plan.targetDay) + 1).coerceAtLeast(1)
        val remainingDays = (today.daysUntil(plan.targetDay) + 1).coerceAtLeast(0)

        val dailyQuota = if (remainingUnits == 0) {
            0
        } else if (remainingDays <= 1) {
            remainingUnits
        } else {
            ceil(remainingUnits.toDouble() / remainingDays).toInt()
        }

        val elapsedDays = (plan.startDay.daysUntil(today)).coerceAtLeast(0)
        val expectedUnitsByToday = if (totalDays <= 0 || elapsedDays == 0L) {
            plan.startUnits
        } else {
            val plannedProgress = (totalPlanUnits.toDouble() * (elapsedDays.toDouble() / totalDays.toDouble()))
            (plan.startUnits + plannedProgress.toInt()).coerceAtMost(plan.totalUnits)
        }

        // On track if currentUnits >= expectedUnitsByToday or book finished
        val isOnTrack = remainingUnits == 0 || currentUnits >= expectedUnitsByToday

        val progressPercent = if (plan.totalUnits > 0) {
            ((currentUnits.toDouble() / plan.totalUnits.toDouble()) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }

        return ReadingPlanStatus(
            plan = plan,
            dailyQuota = dailyQuota,
            remainingDays = remainingDays,
            remainingUnits = remainingUnits,
            expectedUnitsByToday = expectedUnitsByToday,
            actualUnits = currentUnits,
            isOnTrack = isOnTrack,
            progressPercent = progressPercent
        )
    }
}
