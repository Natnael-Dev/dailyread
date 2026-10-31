package io.github.natnael_dev.dailyread.widget

import io.github.natnael_dev.dailyread.data.db.dao.ReadingLedgerDao
import io.github.natnael_dev.dailyread.data.db.dao.SettingsDao
import io.github.natnael_dev.dailyread.data.db.entity.BookEntity
import io.github.natnael_dev.dailyread.data.db.entity.DailyPositionEntity
import io.github.natnael_dev.dailyread.data.db.entity.DailySummaryEntity
import io.github.natnael_dev.dailyread.data.db.entity.GoalVersionEntity
import io.github.natnael_dev.dailyread.data.db.entity.ReadingIntervalEntity
import io.github.natnael_dev.dailyread.data.db.entity.ReadingPositionEntity
import io.github.natnael_dev.dailyread.data.db.entity.SettingEntity
import io.github.natnael_dev.dailyread.data.db.entity.WidgetSnapshotEntity
import io.github.natnael_dev.dailyread.data.ingest.FakeBookDao
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class FakeSettingsDao : SettingsDao {
    private val settings = mutableMapOf<String, String>()
    var snapshot: WidgetSnapshotEntity? = null

    override suspend fun getSetting(key: String): String? = settings[key]
    override suspend fun setSetting(setting: SettingEntity) { settings[setting.key] = setting.value }
    override suspend fun upsertWidgetSnapshot(snapshot: WidgetSnapshotEntity) { this.snapshot = snapshot }
    override suspend fun getWidgetSnapshot(): WidgetSnapshotEntity? = snapshot
    override suspend fun getAllSettings(): List<SettingEntity> =
        settings.map { SettingEntity(it.key, it.value) }
    override suspend fun insertSettings(settings: List<SettingEntity>) {
        for (s in settings) {
            this.settings[s.key] = s.value
        }
    }
}

class FakeReadingLedgerDao : ReadingLedgerDao {
    private val positions = mutableListOf<DailyPositionEntity>()
    private val summaries = mutableListOf<DailySummaryEntity>()
    private val goals = mutableListOf<GoalVersionEntity>()

    override suspend fun insertInterval(interval: ReadingIntervalEntity): Long = 1L
    override suspend fun updateInterval(interval: ReadingIntervalEntity) {}
    override suspend fun getOpenInterval(): ReadingIntervalEntity? = null
    override suspend fun getIntervalsForLogicalDay(logicalDay: String): List<ReadingIntervalEntity> = emptyList()

    override suspend fun recordDailyPosition(dailyPosition: DailyPositionEntity) {
        positions.add(dailyPosition)
    }

    override suspend fun getDistinctPositionCountForDay(logicalDay: String): Int =
        positions.filter { it.logicalDay == logicalDay }.map { it.positionIndex }.distinct().size

    override suspend fun upsertDailySummary(summary: DailySummaryEntity) {
        summaries.removeAll { it.logicalDay == summary.logicalDay }
        summaries.add(summary)
    }

    override suspend fun getDailySummary(logicalDay: String): DailySummaryEntity? =
        summaries.firstOrNull { it.logicalDay == logicalDay }

    override suspend fun getAllDailySummaries(): List<DailySummaryEntity> = summaries

    override suspend fun getQualifyingDays(): List<DailySummaryEntity> =
        summaries.filter { it.qualified == 1 }

    override suspend fun insertGoalVersion(goalVersion: GoalVersionEntity): Long {
        goals.add(goalVersion)
        return goals.size.toLong()
    }

    override suspend fun getActiveGoalForDay(logicalDay: String): GoalVersionEntity? =
        goals.filter { it.effectiveFromDay <= logicalDay }.maxByOrNull { it.effectiveFromDay }

    override suspend fun getAllIntervals(): List<ReadingIntervalEntity> = emptyList()
    override suspend fun insertIntervals(intervals: List<ReadingIntervalEntity>) {}
    override suspend fun getAllDailyPositions(): List<DailyPositionEntity> = positions.toList()
    override suspend fun insertDailyPositions(positions: List<DailyPositionEntity>) {
        this.positions.addAll(positions)
    }
    override suspend fun insertDailySummaries(summaries: List<DailySummaryEntity>) {
        for (s in summaries) {
            this.summaries.removeAll { it.logicalDay == s.logicalDay }
            this.summaries.add(s)
        }
    }
    override suspend fun getAllGoalVersions(): List<GoalVersionEntity> = goals.toList()
    override suspend fun insertGoalVersions(goalVersions: List<GoalVersionEntity>) {
        goals.addAll(goalVersions)
    }
}

class WidgetStateDeriverTest {

    private lateinit var fakeBookDao: FakeBookDao
    private lateinit var fakeLedgerDao: FakeReadingLedgerDao
    private lateinit var fakeSettingsDao: FakeSettingsDao

    @Before
    fun setup() {
        fakeBookDao = FakeBookDao()
        fakeLedgerDao = FakeReadingLedgerDao()
        fakeSettingsDao = FakeSettingsDao()
    }

    @Test
    fun derivesStateFromRoomAccurately() = runBlocking {
        val zone = ZoneId.of("UTC")
        val now = Instant.parse("2026-10-31T12:00:00Z") // 12:00 noon UTC -> logical day 2026-10-31

        // Insert book and reading position
        val bookId = fakeBookDao.insertBook(
            BookEntity(
                contentKey = "walden-key",
                format = "EPUB",
                title = "Walden",
                author = "Henry David Thoreau",
                sizeBytes = 1024L,
                addedAt = 1000L,
                lastOpenedAt = 5000L
            )
        )
        fakeBookDao.upsertReadingPosition(
            ReadingPositionEntity(
                bookId = bookId,
                locatorJson = "{}",
                totalProgression = 0.45,
                updatedAt = 5000L
            )
        )

        // Insert streak: past 5 days qualified
        for (i in 26..30) {
            fakeLedgerDao.upsertDailySummary(
                DailySummaryEntity(
                    logicalDay = "2026-10-$i",
                    activeSeconds = 1200L,
                    positionsCounted = 15,
                    goalType = "PAGES",
                    goalTarget = 10,
                    qualified = 1,
                    utcOffsetSeconds = 0
                )
            )
        }

        // Today: read 6 pages out of 10
        for (p in 1..6) {
            fakeLedgerDao.recordDailyPosition(
                DailyPositionEntity(
                    logicalDay = "2026-10-31",
                    contentKey = "walden-key",
                    positionIndex = p,
                    dwellMs = 6000L
                )
            )
        }

        val state = WidgetStateDeriver.deriveState(
            bookDao = fakeBookDao,
            readingLedgerDao = fakeLedgerDao,
            settingsDao = fakeSettingsDao,
            nowInstant = now,
            zoneId = zone
        )

        assertEquals("Walden", state.lastBookTitle)
        assertEquals(bookId, state.lastBookId)
        assertEquals(45, state.lastBookProgression)
        assertEquals(6, state.pagesReadToday)
        assertEquals(10, state.dailyGoal)
        assertEquals(5, state.streak)
        assertEquals(false, state.todayQualified)

        // Verify snapshot was persisted
        assertNotNull(fakeSettingsDao.snapshot)
        assertEquals("2026-10-31", fakeSettingsDao.snapshot!!.logicalDay)
        assertEquals(5, fakeSettingsDao.snapshot!!.streak)
    }

    @Test
    fun goalMetMarksTodayQualifiedTrue() = runBlocking {
        val zone = ZoneId.of("UTC")
        val now = Instant.parse("2026-10-31T12:00:00Z")

        for (p in 1..10) {
            fakeLedgerDao.recordDailyPosition(
                DailyPositionEntity(
                    logicalDay = "2026-10-31",
                    contentKey = "k",
                    positionIndex = p,
                    dwellMs = 6000L
                )
            )
        }

        val state = WidgetStateDeriver.deriveState(
            bookDao = fakeBookDao,
            readingLedgerDao = fakeLedgerDao,
            settingsDao = fakeSettingsDao,
            nowInstant = now,
            zoneId = zone
        )

        assertEquals(10, state.pagesReadToday)
        assertTrue(state.todayQualified)
        assertEquals(1, fakeSettingsDao.snapshot?.todayQualified)
    }
}
