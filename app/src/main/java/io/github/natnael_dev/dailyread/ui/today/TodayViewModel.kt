package io.github.natnael_dev.dailyread.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.natnael_dev.dailyread.data.db.dao.BookDao
import io.github.natnael_dev.dailyread.data.db.dao.PlanDao
import io.github.natnael_dev.dailyread.data.db.dao.ReadingLedgerDao
import io.github.natnael_dev.dailyread.data.db.entity.DailyPositionEntity
import io.github.natnael_dev.dailyread.data.db.entity.DailySummaryEntity
import io.github.natnael_dev.dailyread.data.db.entity.ReadingIntervalEntity
import io.github.natnael_dev.dailyread.domain.model.Goal
import io.github.natnael_dev.dailyread.domain.plan.ReadingPlan
import io.github.natnael_dev.dailyread.domain.plan.ReadingPlanCalculator
import io.github.natnael_dev.dailyread.domain.streak.DayStatus
import io.github.natnael_dev.dailyread.domain.streak.QualifyingDay
import io.github.natnael_dev.dailyread.domain.streak.StreakEngine
import io.github.natnael_dev.dailyread.domain.time.Clock
import io.github.natnael_dev.dailyread.domain.time.LogicalDay
import io.github.natnael_dev.dailyread.domain.time.LogicalDayCalculator
import io.github.natnael_dev.dailyread.domain.time.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

class TodayViewModel(
    private val bookDao: BookDao,
    private val readingLedgerDao: ReadingLedgerDao,
    private val planDao: PlanDao,
    private val clock: Clock = SystemClock(),
    private val logicalDayCalculator: LogicalDayCalculator = LogicalDayCalculator(),
    private val streakEngine: StreakEngine = StreakEngine()
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            loadDashboard()
        }
    }

    private suspend fun loadDashboard() {
        val now = clock.now()
        val zoneId = clock.zoneId()
        val today = logicalDayCalculator.computeLogicalDay(now, zoneId)
        val zonedDateTime = now.atZone(zoneId)
        val zoneOffsetSeconds = zonedDateTime.offset.totalSeconds

        // 1. Time greeting & date formatting
        val hour = zonedDateTime.hour
        val greeting = when (hour) {
            in 4..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
        val formattedDate = DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.ENGLISH).format(zonedDateTime)

        // 2. Goal & today progress
        val goalEntity = readingLedgerDao.getActiveGoalForDay(today.toIsoString())
        val goalPages = goalEntity?.target ?: Goal.DEFAULT.target
        val todayIso = today.toIsoString()

        val todayPositions = readingLedgerDao.getDistinctPositionCountForDay(todayIso)
        val todayIntervals = readingLedgerDao.getIntervalsForLogicalDay(todayIso)
        val todayIntervalsCount = todayIntervals.sumOf { it.positionsCounted }
        val pagesReadToday = maxOf(todayPositions, todayIntervalsCount)

        val todayActiveSeconds = todayIntervals.sumOf { it.activeSeconds }
        val estimatedMinutes = (todayActiveSeconds / 60).toInt()
        val remainingPages = (goalPages - pagesReadToday).coerceAtLeast(0)
        val progressPercent = if (goalPages > 0) {
            ((pagesReadToday.toDouble() / goalPages.toDouble()) * 100).toInt().coerceIn(0, 100)
        } else 0

        // 3. Streak calculation
        val summaries = readingLedgerDao.getAllDailySummaries()
        val summariesMap = summaries.associateBy { it.logicalDay }

        val qualifyingDaysList = mutableListOf<QualifyingDay>()
        summaries.filter { it.qualified == 1 }.forEach {
            qualifyingDaysList.add(
                QualifyingDay(
                    logicalDay = LogicalDay.parse(it.logicalDay),
                    utcOffsetSeconds = it.utcOffsetSeconds
                )
            )
        }

        // Include today if today qualified
        val isTodayQualified = pagesReadToday >= goalPages
        if (isTodayQualified && qualifyingDaysList.none { it.logicalDay == today }) {
            qualifyingDaysList.add(QualifyingDay(today, zoneOffsetSeconds))
        }

        val streakResult = streakEngine.calculateStreak(
            qualifyingDaysInput = qualifyingDaysList,
            today = today,
            currentZoneOffsetSeconds = zoneOffsetSeconds
        )

        val streakSubtitle = when {
            streakResult.currentStreak >= 14 -> "${streakResult.currentStreak / 7} weeks of daily quiet reading completed"
            streakResult.currentStreak > 1 -> "${streakResult.currentStreak} consecutive days of mindful reading"
            streakResult.currentStreak == 1 -> "1 day completed — your reading ritual has begun"
            else -> "Start your daily reading ritual today"
        }

        val freezeBadgeText = when (streakResult.freezesHeld) {
            1 -> "1 freeze safeguarded"
            2 -> "2 freezes safeguarded"
            else -> "0 freezes available"
        }

        // 4. Active Plan
        val activePlanEntity = planDao.getActivePlan()
        val (activePlanStatus, activePlanBook) = if (activePlanEntity != null) {
            val book = bookDao.getBookById(activePlanEntity.bookId)
            val pos = bookDao.getReadingPosition(activePlanEntity.bookId)
            val totalUnits = activePlanEntity.totalUnits ?: book?.totalUnits ?: 250
            val currentUnits = if (pos != null && totalUnits > 0) {
                (pos.totalProgression * totalUnits).toInt()
            } else {
                activePlanEntity.startUnits
            }
            val planDomain = ReadingPlan(
                id = activePlanEntity.id,
                bookId = activePlanEntity.bookId,
                startDay = LogicalDay.parse(activePlanEntity.startDay),
                targetDay = LogicalDay.parse(activePlanEntity.targetDay),
                startUnits = activePlanEntity.startUnits,
                totalUnits = totalUnits
            )
            Pair(ReadingPlanCalculator.calculateStatus(planDomain, today, currentUnits), book)
        } else Pair(null, null)

        // 5. Currently reading book
        val allBooks = bookDao.getAllBooks().firstOrNull() ?: emptyList()
        val lastReadBook = allBooks.maxByOrNull { it.lastOpenedAt ?: it.addedAt }
        val (lastReadPos, lastReadPage, lastReadTotal, lastReadPercent) = if (lastReadBook != null) {
            val pos = bookDao.getReadingPosition(lastReadBook.id)
            val total = lastReadBook.totalUnits ?: 250
            val current = if (pos != null) (pos.totalProgression * total).toInt().coerceIn(1, total) else 1
            val pct = if (pos != null) (pos.totalProgression * 100).toInt().coerceIn(0, 100) else 0
            listOf(pos, current, total, pct)
        } else listOf(null, 0, 0, 0)

        // 6. 30-day activity matrix (from today - 29 days up to today)
        val matrixDays = mutableListOf<ActivityDayUiModel>()
        var daysLoggedCount = 0

        val weekdayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

        for (i in 29 downTo 0) {
            val day = today.minusDays(i.toLong())
            val iso = day.toIsoString()
            val dayPages = if (day == today) {
                pagesReadToday
            } else {
                summariesMap[iso]?.positionsCounted ?: 0
            }

            if (dayPages > 0) {
                daysLoggedCount++
            }

            val status = streakResult.dayStatusMap[day] ?: when {
                day == today -> streakResult.todayStatus
                dayPages >= goalPages -> DayStatus.QUALIFIED
                dayPages > 0 -> DayStatus.QUALIFIED
                else -> DayStatus.MISSED
            }

            val level = when {
                status == DayStatus.FROZEN -> 4
                dayPages == 0 -> 0
                dayPages in 1..5 -> 1
                dayPages in 6..15 -> 2
                else -> 3
            }

            val dayOfWeekIndex = (day.date.dayOfWeek.value - 1) % 7 // Monday = 0
            val dayOfWeekInitial = weekdayLabels.getOrElse(dayOfWeekIndex) { "M" }

            matrixDays.add(
                ActivityDayUiModel(
                    logicalDay = day,
                    dayOfMonth = day.date.dayOfMonth,
                    dayOfWeekInitial = dayOfWeekInitial,
                    pagesCounted = dayPages,
                    status = status,
                    level = level,
                    isToday = day == today,
                    isFuture = false
                )
            )
        }

        val todayActivityModel = matrixDays.lastOrNull { it.isToday }
        val completionPct = if (matrixDays.isNotEmpty()) ((daysLoggedCount.toDouble() / 30.0) * 100).toInt() else 0
        val completionRateText = "$daysLoggedCount of 30 logged ($completionPct%)"

        _uiState.value = TodayUiState(
            greeting = greeting,
            userName = "Miriam",
            formattedDate = formattedDate,
            currentStreak = streakResult.currentStreak,
            bestStreak = streakResult.bestStreak,
            freezesHeld = streakResult.freezesHeld,
            streakStatus = streakResult.todayStatus,
            isAtRisk = streakResult.isAtRisk,
            isProtected = streakResult.isProtected,
            streakSubtitle = streakSubtitle,
            freezeBadgeText = freezeBadgeText,
            isPersonalBest = streakResult.currentStreak > 0 && streakResult.currentStreak == streakResult.bestStreak,

            pagesReadToday = pagesReadToday,
            goalPages = goalPages,
            progressPercent = progressPercent,
            remainingPages = remainingPages,
            estimatedMinutesToday = estimatedMinutes,

            activePlanStatus = activePlanStatus,
            activePlanBook = activePlanBook,

            lastReadBook = lastReadBook,
            lastReadPosition = lastReadPos as? io.github.natnael_dev.dailyread.data.db.entity.ReadingPositionEntity,
            lastReadCurrentPage = (lastReadPage as? Int) ?: 0,
            lastReadTotalPages = (lastReadTotal as? Int) ?: 0,
            lastReadProgressPercent = (lastReadPercent as? Int) ?: 0,

            activityMatrix = matrixDays,
            selectedDaySummary = _uiState.value.selectedDaySummary ?: todayActivityModel,
            completionRateText = completionRateText,
            toastMessage = _uiState.value.toastMessage,
            isLoaded = true
        )
    }

    fun selectDay(day: ActivityDayUiModel) {
        _uiState.value = _uiState.value.copy(selectedDaySummary = day)
    }

    fun quickAddPages(count: Int = 5) {
        viewModelScope.launch {
            val now = clock.now()
            val zoneId = clock.zoneId()
            val today = logicalDayCalculator.computeLogicalDay(now, zoneId)
            val todayIso = today.toIsoString()

            val currentDistinct = readingLedgerDao.getDistinctPositionCountForDay(todayIso)
            val startPos = currentDistinct + 1
            for (p in startPos until startPos + count) {
                readingLedgerDao.recordDailyPosition(
                    DailyPositionEntity(
                        logicalDay = todayIso,
                        contentKey = "quick-add-daily-ritual",
                        positionIndex = p,
                        dwellMs = 6000L
                    )
                )
            }

            val newTotal = readingLedgerDao.getDistinctPositionCountForDay(todayIso)
            val goal = readingLedgerDao.getActiveGoalForDay(todayIso)?.target ?: Goal.DEFAULT.target
            val qualified = if (newTotal >= goal) 1 else 0

            readingLedgerDao.upsertDailySummary(
                DailySummaryEntity(
                    logicalDay = todayIso,
                    activeSeconds = (newTotal * 30L),
                    positionsCounted = newTotal,
                    goalType = "PAGES",
                    goalTarget = goal,
                    qualified = qualified,
                    utcOffsetSeconds = now.atZone(zoneId).offset.totalSeconds
                )
            )

            _uiState.value = _uiState.value.copy(
                toastMessage = if (newTotal >= goal) "Ritual completed! 🌿" else "+$count pages recorded"
            )
            loadDashboard()
        }
    }

    fun clearToast() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }
}
