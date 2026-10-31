package io.github.natnael_dev.dailyread.ui.plans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.natnael_dev.dailyread.data.db.dao.BookDao
import io.github.natnael_dev.dailyread.data.db.dao.PlanDao
import io.github.natnael_dev.dailyread.data.db.entity.PlanEntity
import io.github.natnael_dev.dailyread.domain.plan.ReadingPlan
import io.github.natnael_dev.dailyread.domain.plan.ReadingPlanCalculator
import io.github.natnael_dev.dailyread.domain.time.Clock
import io.github.natnael_dev.dailyread.domain.time.LogicalDay
import io.github.natnael_dev.dailyread.domain.time.LogicalDayCalculator
import io.github.natnael_dev.dailyread.domain.time.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class PlansViewModel(
    private val planDao: PlanDao,
    private val bookDao: BookDao,
    private val clock: Clock = SystemClock(),
    private val logicalDayCalculator: LogicalDayCalculator = LogicalDayCalculator()
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlansUiState())
    val uiState: StateFlow<PlansUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            loadPlans()
        }
    }

    private suspend fun loadPlans() {
        val now = clock.now()
        val zoneId = clock.zoneId()
        val today = logicalDayCalculator.computeLogicalDay(now, zoneId)

        val books = bookDao.getAllBooks().firstOrNull() ?: emptyList()
        val allPlans = planDao.getAllPlansFlow().firstOrNull() ?: emptyList()
        val activePlanEntities = allPlans.filter { it.state == "ACTIVE" }

        val statusList = activePlanEntities.map { entity ->
            val book = books.firstOrNull { it.id == entity.bookId }
            val pos = bookDao.getReadingPosition(entity.bookId)
            val totalUnits = entity.totalUnits ?: book?.totalUnits ?: 250
            val currentUnits = if (pos != null && totalUnits > 0) {
                (pos.totalProgression * totalUnits).toInt()
            } else {
                entity.startUnits
            }

            val domainPlan = ReadingPlan(
                id = entity.id,
                bookId = entity.bookId,
                startDay = LogicalDay.parse(entity.startDay),
                targetDay = LogicalDay.parse(entity.targetDay),
                startUnits = entity.startUnits,
                totalUnits = totalUnits
            )
            ReadingPlanCalculator.calculateStatus(domainPlan, today, currentUnits)
        }

        val defaultSelectedBook = books.firstOrNull()
        val targetDate = today.plusDays(14)
        val defaultQuota = if (defaultSelectedBook != null) {
            val total = defaultSelectedBook.totalUnits ?: 250
            (total + 13) / 14
        } else null

        _uiState.value = PlansUiState(
            activePlans = statusList,
            availableBooks = books,
            selectedBookId = defaultSelectedBook?.id,
            targetDaysOut = 14,
            targetFinishDate = targetDate,
            calculatedDailyQuota = defaultQuota,
            isLoaded = true
        )
    }

    fun openCreateDialog() {
        _uiState.value = _uiState.value.copy(isCreateDialogOpen = true)
    }

    fun closeCreateDialog() {
        _uiState.value = _uiState.value.copy(isCreateDialogOpen = false)
    }

    fun selectBook(bookId: Long) {
        val book = _uiState.value.availableBooks.firstOrNull { it.id == bookId }
        val days = _uiState.value.targetDaysOut
        val total = book?.totalUnits ?: 250
        val quota = if (days > 0) (total + days - 1) / days else total
        _uiState.value = _uiState.value.copy(
            selectedBookId = bookId,
            calculatedDailyQuota = quota
        )
    }

    fun setTargetDays(days: Int) {
        val now = clock.now()
        val zoneId = clock.zoneId()
        val today = logicalDayCalculator.computeLogicalDay(now, zoneId)
        val target = today.plusDays(days.toLong())

        val bookId = _uiState.value.selectedBookId
        val book = _uiState.value.availableBooks.firstOrNull { it.id == bookId }
        val total = book?.totalUnits ?: 250
        val quota = if (days > 0) (total + days - 1) / days else total

        _uiState.value = _uiState.value.copy(
            targetDaysOut = days,
            targetFinishDate = target,
            calculatedDailyQuota = quota
        )
    }

    fun createPlan(bookId: Long, targetDay: LogicalDay) {
        viewModelScope.launch {
            val now = clock.now()
            val zoneId = clock.zoneId()
            val today = logicalDayCalculator.computeLogicalDay(now, zoneId)

            val book = bookDao.getBookById(bookId)
            val pos = bookDao.getReadingPosition(bookId)
            val totalUnits = book?.totalUnits ?: 250
            val startUnits = if (pos != null) (pos.totalProgression * totalUnits).toInt() else 0

            val planEntity = PlanEntity(
                bookId = bookId,
                startDay = today.toIsoString(),
                targetDay = targetDay.toIsoString(),
                startUnits = startUnits,
                totalUnits = totalUnits,
                state = "ACTIVE"
            )

            planDao.insertPlan(planEntity)
            _uiState.value = _uiState.value.copy(isCreateDialogOpen = false)
            loadPlans()
        }
    }

    fun deletePlan(planId: Long) {
        viewModelScope.launch {
            planDao.deletePlan(planId)
            loadPlans()
        }
    }
}
