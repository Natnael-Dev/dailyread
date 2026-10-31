package io.github.natnael_dev.dailyread.ui.today

import io.github.natnael_dev.dailyread.data.db.dao.PlanDao
import io.github.natnael_dev.dailyread.data.db.dao.ReadingLedgerDao
import io.github.natnael_dev.dailyread.data.db.entity.BookEntity
import io.github.natnael_dev.dailyread.data.db.entity.DailyPositionEntity
import io.github.natnael_dev.dailyread.data.db.entity.DailySummaryEntity
import io.github.natnael_dev.dailyread.data.db.entity.GoalVersionEntity
import io.github.natnael_dev.dailyread.data.db.entity.PlanEntity
import io.github.natnael_dev.dailyread.data.db.entity.ReadingIntervalEntity
import io.github.natnael_dev.dailyread.data.ingest.FakeBookDao
import io.github.natnael_dev.dailyread.domain.streak.DayStatus
import io.github.natnael_dev.dailyread.domain.time.FakeClock
import io.github.natnael_dev.dailyread.domain.time.LogicalDay
import io.github.natnael_dev.dailyread.domain.time.LogicalDayCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class FakeReadingLedgerDao : ReadingLedgerDao {
    val intervals = mutableListOf<ReadingIntervalEntity>()
    val dailyPositions = mutableListOf<DailyPositionEntity>()
    val dailySummaries = mutableMapOf<String, DailySummaryEntity>()
    val goalVersions = mutableListOf<GoalVersionEntity>()
    private var nextIntervalId = 1L

    override suspend fun insertInterval(interval: ReadingIntervalEntity): Long {
        val id = nextIntervalId++
        intervals.add(interval.copy(id = id))
        return id
    }

    override suspend fun updateInterval(interval: ReadingIntervalEntity) {
        val idx = intervals.indexOfFirst { it.id == interval.id }
        if (idx >= 0) intervals[idx] = interval
    }

    override suspend fun getOpenInterval(): ReadingIntervalEntity? =
        intervals.firstOrNull { it.closed == 0 }

    override suspend fun getIntervalsForLogicalDay(logicalDay: String): List<ReadingIntervalEntity> =
        intervals.filter { it.logicalDay == logicalDay }

    override suspend fun recordDailyPosition(dailyPosition: DailyPositionEntity) {
        if (dailyPositions.none {
                it.logicalDay == dailyPosition.logicalDay &&
                it.contentKey == dailyPosition.contentKey &&
                it.positionIndex == dailyPosition.positionIndex
            }
        ) {
            dailyPositions.add(dailyPosition)
        }
    }

    override suspend fun getDistinctPositionCountForDay(logicalDay: String): Int =
        dailyPositions.filter { it.logicalDay == logicalDay }
            .distinctBy { it.positionIndex }
            .size

    override suspend fun upsertDailySummary(summary: DailySummaryEntity) {
        dailySummaries[summary.logicalDay] = summary
    }

    override suspend fun getDailySummary(logicalDay: String): DailySummaryEntity? =
        dailySummaries[logicalDay]

    override suspend fun getAllDailySummaries(): List<DailySummaryEntity> =
        dailySummaries.values.sortedBy { it.logicalDay }

    override suspend fun getQualifyingDays(): List<DailySummaryEntity> =
        dailySummaries.values.filter { it.qualified == 1 }.sortedBy { it.logicalDay }

    override suspend fun insertGoalVersion(goalVersion: GoalVersionEntity): Long {
        goalVersions.add(goalVersion)
        return goalVersion.id
    }

    override suspend fun getActiveGoalForDay(logicalDay: String): GoalVersionEntity? =
        goalVersions.filter { it.effectiveFromDay <= logicalDay }
            .maxByOrNull { it.effectiveFromDay }

    override suspend fun getAllIntervals(): List<ReadingIntervalEntity> = intervals.toList()
    override suspend fun insertIntervals(intervals: List<ReadingIntervalEntity>) {
        this.intervals.addAll(intervals)
    }
    override suspend fun getAllDailyPositions(): List<DailyPositionEntity> = dailyPositions.toList()
    override suspend fun insertDailyPositions(positions: List<DailyPositionEntity>) {
        dailyPositions.addAll(positions)
    }
    override suspend fun insertDailySummaries(summaries: List<DailySummaryEntity>) {
        for (s in summaries) {
            dailySummaries[s.logicalDay] = s
        }
    }
    override suspend fun getAllGoalVersions(): List<GoalVersionEntity> = goalVersions.toList()
    override suspend fun insertGoalVersions(goalVersions: List<GoalVersionEntity>) {
        this.goalVersions.addAll(goalVersions)
    }
}

class FakePlanDao : PlanDao {
    val plans = mutableMapOf<Long, PlanEntity>()
    private val plansFlow = MutableStateFlow<List<PlanEntity>>(emptyList())
    private var nextId = 1L

    private fun updateFlow() {
        plansFlow.value = plans.values.toList()
    }

    override suspend fun insertPlan(plan: PlanEntity): Long {
        val id = if (plan.id == 0L) nextId++ else plan.id
        plans[id] = plan.copy(id = id)
        updateFlow()
        return id
    }

    override suspend fun updatePlan(plan: PlanEntity) {
        plans[plan.id] = plan
        updateFlow()
    }

    override suspend fun getActivePlan(): PlanEntity? =
        plans.values.firstOrNull { it.state == "ACTIVE" }

    override fun getActivePlanFlow(): Flow<PlanEntity?> =
        MutableStateFlow(plans.values.firstOrNull { it.state == "ACTIVE" })

    override suspend fun getActivePlanForBook(bookId: Long): PlanEntity? =
        plans.values.firstOrNull { it.bookId == bookId && it.state == "ACTIVE" }

    override fun getActivePlanForBookFlow(bookId: Long): Flow<PlanEntity?> =
        MutableStateFlow(plans.values.firstOrNull { it.bookId == bookId && it.state == "ACTIVE" })

    override fun getAllPlansFlow(): Flow<List<PlanEntity>> = plansFlow

    override suspend fun getAllPlansSync(): List<PlanEntity> = plans.values.toList().sortedBy { it.id }

    override suspend fun insertPlans(plans: List<PlanEntity>) {
        for (p in plans) {
            val id = if (p.id == 0L) nextId++ else p.id
            this.plans[id] = p.copy(id = id)
        }
        updateFlow()
    }

    override suspend fun deletePlan(id: Long) {
        plans[id]?.let {
            plans[id] = it.copy(state = "DELETED")
            updateFlow()
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var bookDao: FakeBookDao
    private lateinit var readingLedgerDao: FakeReadingLedgerDao
    private lateinit var planDao: FakePlanDao
    private lateinit var clock: FakeClock
    private val calculator = LogicalDayCalculator()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        bookDao = FakeBookDao()
        readingLedgerDao = FakeReadingLedgerDao()
        planDao = FakePlanDao()

        // 2026-10-24 10:00 UTC+03:00
        val instant = Instant.parse("2026-10-24T07:00:00Z")
        clock = FakeClock(instant, ZoneId.of("UTC+03:00"))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `30-day activity matrix contains exactly 30 days ending today`() = runTest(testDispatcher) {
        val viewModel = TodayViewModel(
            bookDao = bookDao,
            readingLedgerDao = readingLedgerDao,
            planDao = planDao,
            clock = clock,
            logicalDayCalculator = calculator
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(30, state.activityMatrix.size)

        val firstDay = state.activityMatrix.first()
        val lastDay = state.activityMatrix.last()

        val expectedToday = LogicalDay.parse("2026-10-24")
        val expectedStart = expectedToday.minusDays(29)

        assertEquals(expectedStart, firstDay.logicalDay)
        assertEquals(expectedToday, lastDay.logicalDay)
        assertTrue(lastDay.isToday)
        assertEquals(10, state.goalPages)
        assertEquals(10, state.remainingPages)
    }

    @Test
    fun `today progress ring reflects pages read against goal`() = runTest(testDispatcher) {
        val todayIso = "2026-10-24"
        for (p in 1..7) {
            readingLedgerDao.recordDailyPosition(
                DailyPositionEntity(todayIso, "test-book", p, 6000L)
            )
        }

        val viewModel = TodayViewModel(
            bookDao = bookDao,
            readingLedgerDao = readingLedgerDao,
            planDao = planDao,
            clock = clock,
            logicalDayCalculator = calculator
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(7, state.pagesReadToday)
        assertEquals(10, state.goalPages)
        assertEquals(70, state.progressPercent)
        assertEquals(3, state.remainingPages)
    }

    @Test
    fun `streak engine accurately reports consecutive days and freeze held`() = runTest(testDispatcher) {
        // Setup past 7 consecutive qualifying days (Oct 17 to Oct 23)
        for (i in 17..23) {
            val dayIso = "2026-10-$i"
            readingLedgerDao.upsertDailySummary(
                DailySummaryEntity(
                    logicalDay = dayIso,
                    activeSeconds = 600L,
                    positionsCounted = 15,
                    goalType = "PAGES",
                    goalTarget = 10,
                    qualified = 1,
                    utcOffsetSeconds = 10800
                )
            )
        }

        val viewModel = TodayViewModel(
            bookDao = bookDao,
            readingLedgerDao = readingLedgerDao,
            planDao = planDao,
            clock = clock,
            logicalDayCalculator = calculator
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        // 7 days completed, today in progress -> streak is 7, at risk, 1 freeze earned
        assertEquals(7, state.currentStreak)
        assertEquals(1, state.freezesHeld)
        assertTrue(state.isAtRisk)
        assertEquals("1 freeze safeguarded", state.freezeBadgeText)
    }

    @Test
    fun `quickAddPages increments pages and reaches ritual goal`() = runTest(testDispatcher) {
        val viewModel = TodayViewModel(
            bookDao = bookDao,
            readingLedgerDao = readingLedgerDao,
            planDao = planDao,
            clock = clock,
            logicalDayCalculator = calculator
        )
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.pagesReadToday)

        viewModel.quickAddPages(5)
        advanceUntilIdle()

        assertEquals(5, viewModel.uiState.value.pagesReadToday)
        assertEquals(50, viewModel.uiState.value.progressPercent)
        assertEquals("+5 pages recorded", viewModel.uiState.value.toastMessage)

        viewModel.quickAddPages(5)
        advanceUntilIdle()

        assertEquals(10, viewModel.uiState.value.pagesReadToday)
        assertEquals(100, viewModel.uiState.value.progressPercent)
        assertEquals("Ritual completed! 🌿", viewModel.uiState.value.toastMessage)
    }

    @Test
    fun `03_00 rollover assigns early morning reading before 3am to previous logical day`() = runTest(testDispatcher) {
        // 02:30 AM on Oct 25 local time -> Logical Day should be Oct 24
        val earlyMorning = Instant.parse("2026-10-24T23:30:00Z") // 02:30 in UTC+03:00
        val rolloverClock = FakeClock(earlyMorning, ZoneId.of("UTC+03:00"))

        val viewModel = TodayViewModel(
            bookDao = bookDao,
            readingLedgerDao = readingLedgerDao,
            planDao = planDao,
            clock = rolloverClock,
            logicalDayCalculator = calculator
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val lastDay = state.activityMatrix.last()
        assertEquals(LogicalDay.parse("2026-10-24"), lastDay.logicalDay)
    }

    @Test
    fun `active reading plan is loaded and status correctly mapped`() = runTest(testDispatcher) {
        val bookId = bookDao.insertBook(
            BookEntity(
                contentKey = "autumn-book",
                format = "EPUB",
                title = "The Nature of Thought",
                author = "E. R. Harrison",
                sizeBytes = 2048L,
                totalUnits = 310,
                addedAt = 1000L
            )
        )

        planDao.insertPlan(
            PlanEntity(
                bookId = bookId,
                startDay = "2026-10-01",
                targetDay = "2026-11-15",
                startUnits = 0,
                totalUnits = 310,
                state = "ACTIVE"
            )
        )

        val viewModel = TodayViewModel(
            bookDao = bookDao,
            readingLedgerDao = readingLedgerDao,
            planDao = planDao,
            clock = clock,
            logicalDayCalculator = calculator
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.activePlanStatus)
        assertNotNull(state.activePlanBook)
        assertEquals("The Nature of Thought", state.activePlanBook?.title)
        assertTrue(state.activePlanStatus!!.dailyQuota > 0)
    }
}
