package io.github.natnael_dev.dailyread.ui.today

import io.github.natnael_dev.dailyread.data.db.entity.BookEntity
import io.github.natnael_dev.dailyread.data.db.entity.ReadingPositionEntity
import io.github.natnael_dev.dailyread.domain.plan.ReadingPlanStatus
import io.github.natnael_dev.dailyread.domain.streak.DayStatus
import io.github.natnael_dev.dailyread.domain.time.LogicalDay

data class ActivityDayUiModel(
    val logicalDay: LogicalDay,
    val dayOfMonth: Int,
    val dayOfWeekInitial: String,
    val pagesCounted: Int,
    val status: DayStatus,
    val level: Int, // 0: None, 1: 1-5p, 2: 6-15p, 3: Goal met, 4: Frozen
    val isToday: Boolean,
    val isFuture: Boolean
)

data class TodayUiState(
    val greeting: String = "Good morning",
    val userName: String = "Reader",
    val formattedDate: String = "",
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val freezesHeld: Int = 0,
    val streakStatus: DayStatus = DayStatus.TODAY_IN_PROGRESS,
    val isAtRisk: Boolean = false,
    val isProtected: Boolean = false,
    val streakSubtitle: String = "Start your daily reading ritual",
    val freezeBadgeText: String = "0 freezes safeguarded",
    val isPersonalBest: Boolean = false,

    // Daily progress
    val pagesReadToday: Int = 0,
    val goalPages: Int = 10,
    val progressPercent: Int = 0,
    val remainingPages: Int = 10,
    val estimatedMinutesToday: Int = 0,

    // Active plan
    val activePlanStatus: ReadingPlanStatus? = null,
    val activePlanBook: BookEntity? = null,

    // Currently reading
    val lastReadBook: BookEntity? = null,
    val lastReadPosition: ReadingPositionEntity? = null,
    val lastReadProgressPercent: Int = 0,
    val lastReadCurrentPage: Int = 0,
    val lastReadTotalPages: Int = 0,

    // 30-day activity matrix
    val activityMatrix: List<ActivityDayUiModel> = emptyList(),
    val selectedDaySummary: ActivityDayUiModel? = null,
    val completionRateText: String = "",

    val toastMessage: String? = null,
    val isLoaded: Boolean = false
)
