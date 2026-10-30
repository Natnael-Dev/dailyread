package io.github.natnael_dev.dailyread.domain.model

import io.github.natnael_dev.dailyread.domain.time.LogicalDay

enum class GoalType {
    PAGES,
    MINUTES
}

data class Goal(
    val type: GoalType,
    val target: Int
) {
    fun isQualified(positionsCounted: Int, activeSeconds: Long): Boolean {
        return when (type) {
            GoalType.PAGES -> positionsCounted >= target
            GoalType.MINUTES -> activeSeconds >= target * 60L
        }
    }

    companion object {
        val DEFAULT: Goal = Goal(GoalType.PAGES, 10)
    }
}

data class GoalVersion(
    val id: Long = 0,
    val effectiveFromDay: LogicalDay,
    val goal: Goal
)
