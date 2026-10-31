package io.github.natnael_dev.dailyread.ui.plans

import io.github.natnael_dev.dailyread.data.db.entity.BookEntity
import io.github.natnael_dev.dailyread.domain.plan.ReadingPlanStatus
import io.github.natnael_dev.dailyread.domain.time.LogicalDay

data class PlansUiState(
    val activePlans: List<ReadingPlanStatus> = emptyList(),
    val availableBooks: List<BookEntity> = emptyList(),
    val isCreateDialogOpen: Boolean = false,
    val selectedBookId: Long? = null,
    val targetDaysOut: Int = 14,
    val targetFinishDate: LogicalDay? = null,
    val calculatedDailyQuota: Int? = null,
    val isLoaded: Boolean = false
)
