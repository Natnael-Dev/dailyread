package io.github.natnael_dev.dailyread.ui.settings

import io.github.natnael_dev.dailyread.data.backup.BackupEngine
import io.github.natnael_dev.dailyread.data.backup.BackupSummary
import io.github.natnael_dev.dailyread.data.db.dao.ReadingLedgerDao
import io.github.natnael_dev.dailyread.data.db.dao.SettingsDao
import io.github.natnael_dev.dailyread.data.db.entity.DailyPositionEntity
import io.github.natnael_dev.dailyread.data.db.entity.DailySummaryEntity
import io.github.natnael_dev.dailyread.data.db.entity.GoalVersionEntity
import io.github.natnael_dev.dailyread.data.db.entity.ReadingIntervalEntity
import io.github.natnael_dev.dailyread.data.db.entity.SettingEntity
import io.github.natnael_dev.dailyread.data.db.entity.WidgetSnapshotEntity
import io.github.natnael_dev.dailyread.domain.time.FakeClock
import io.github.natnael_dev.dailyread.domain.time.LogicalDayCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant
import java.time.ZoneId

class FakeSettingsDao : SettingsDao {
    val settingsMap = mutableMapOf<String, String>()

    override suspend fun getSetting(key: String): String? = settingsMap[key]

    override suspend fun setSetting(setting: SettingEntity) {
        settingsMap[setting.key] = setting.value
    }

    override suspend fun upsertWidgetSnapshot(snapshot: WidgetSnapshotEntity) {}
    override suspend fun getWidgetSnapshot(): WidgetSnapshotEntity? = null
    override suspend fun getAllSettings(): List<SettingEntity> =
        settingsMap.map { SettingEntity(it.key, it.value) }
    override suspend fun insertSettings(settings: List<SettingEntity>) {
        for (s in settings) {
            settingsMap[s.key] = s.value
        }
    }
}

class FakeSettingsReadingLedgerDao : ReadingLedgerDao {
    val goalVersions = mutableListOf<GoalVersionEntity>()

    override suspend fun insertInterval(interval: ReadingIntervalEntity): Long = 1L
    override suspend fun updateInterval(interval: ReadingIntervalEntity) {}
    override suspend fun getOpenInterval(): ReadingIntervalEntity? = null
    override suspend fun getIntervalsForLogicalDay(logicalDay: String): List<ReadingIntervalEntity> = emptyList()
    override suspend fun recordDailyPosition(dailyPosition: DailyPositionEntity) {}
    override suspend fun getDistinctPositionCountForDay(logicalDay: String): Int = 0
    override suspend fun upsertDailySummary(summary: DailySummaryEntity) {}
    override suspend fun getDailySummary(logicalDay: String): DailySummaryEntity? = null
    override suspend fun getAllDailySummaries(): List<DailySummaryEntity> = emptyList()
    override suspend fun getQualifyingDays(): List<DailySummaryEntity> = emptyList()

    override suspend fun insertGoalVersion(goalVersion: GoalVersionEntity): Long {
        goalVersions.add(goalVersion)
        return goalVersion.id
    }

    override suspend fun getActiveGoalForDay(logicalDay: String): GoalVersionEntity? =
        goalVersions.filter { it.effectiveFromDay <= logicalDay }.maxByOrNull { it.effectiveFromDay }

    override suspend fun getAllIntervals(): List<ReadingIntervalEntity> = emptyList()
    override suspend fun insertIntervals(intervals: List<ReadingIntervalEntity>) {}
    override suspend fun getAllDailyPositions(): List<DailyPositionEntity> = emptyList()
    override suspend fun insertDailyPositions(positions: List<DailyPositionEntity>) {}
    override suspend fun insertDailySummaries(summaries: List<DailySummaryEntity>) {}
    override suspend fun getAllGoalVersions(): List<GoalVersionEntity> = goalVersions.toList()
    override suspend fun insertGoalVersions(goalVersions: List<GoalVersionEntity>) {
        this.goalVersions.addAll(goalVersions)
    }
}

class FakeBackupEngine : BackupEngine(null) {
    var exportCalled = false
    var importCalled = false
    var shouldFail = false
    var mockSummary = BackupSummary(
        booksCount = 5,
        bookSourcesCount = 5,
        readingPositionsCount = 3,
        bookmarksCount = 2,
        intervalsCount = 10,
        dailyPositionsCount = 15,
        dailySummariesCount = 7,
        goalVersionsCount = 1,
        plansCount = 1,
        settingsCount = 3
    )

    override suspend fun exportToStream(outputStream: OutputStream) {
        if (shouldFail) throw RuntimeException("Export disk error")
        exportCalled = true
        outputStream.write("{}".toByteArray(Charsets.UTF_8))
    }

    override suspend fun importFromStream(inputStream: InputStream): BackupSummary {
        if (shouldFail) throw RuntimeException("Corrupted backup json")
        importCalled = true
        return mockSummary
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var settingsDao: FakeSettingsDao
    private lateinit var readingLedgerDao: FakeSettingsReadingLedgerDao
    private lateinit var backupEngine: FakeBackupEngine
    private lateinit var clock: FakeClock
    private val logicalDayCalculator = LogicalDayCalculator()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        settingsDao = FakeSettingsDao()
        readingLedgerDao = FakeSettingsReadingLedgerDao()
        backupEngine = FakeBackupEngine()
        clock = FakeClock(Instant.parse("2026-10-31T10:00:00Z"), ZoneId.of("UTC"))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialStateDefaults() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("SYSTEM", state.themeMode)
        assertEquals(10, state.dailyGoalPages)
        assertFalse(state.isExporting)
        assertFalse(state.isRestoring)
        assertFalse(state.showRelinkPrompt)
        assertFalse(state.showLicensesDialog)
    }

    @Test
    fun testSetAppThemePersistsSetting() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.setAppTheme("DARK")
        advanceUntilIdle()

        assertEquals("DARK", viewModel.uiState.value.themeMode)
        assertEquals("DARK", settingsDao.getSetting("app_theme"))

        viewModel.setAppTheme("LIGHT")
        advanceUntilIdle()

        assertEquals("LIGHT", viewModel.uiState.value.themeMode)
        assertEquals("LIGHT", settingsDao.getSetting("app_theme"))
    }

    @Test
    fun testSetDailyGoalClampsAndPersists() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.setDailyGoal(25)
        advanceUntilIdle()

        assertEquals(25, viewModel.uiState.value.dailyGoalPages)
        assertEquals("25", settingsDao.getSetting("daily_goal_target"))
        val goalEntity = readingLedgerDao.getActiveGoalForDay("2026-10-31")
        assertNotNull(goalEntity)
        assertEquals(25, goalEntity?.target)

        // Test clamping: -5 becomes 1, 150 becomes 100
        viewModel.setDailyGoal(-5)
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.dailyGoalPages)

        viewModel.setDailyGoal(150)
        advanceUntilIdle()
        assertEquals(100, viewModel.uiState.value.dailyGoalPages)
    }

    @Test
    fun testExportBackupSuccess() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val out = ByteArrayOutputStream()
        viewModel.exportBackup(out)
        advanceUntilIdle()

        assertTrue(backupEngine.exportCalled)
        assertFalse(viewModel.uiState.value.isExporting)
        assertEquals("Backup exported successfully!", viewModel.uiState.value.exportSuccessMessage)
    }

    @Test
    fun testRestoreBackupTriggersRelinkPrompt_BS04() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val input = ByteArrayInputStream("{}".toByteArray(Charsets.UTF_8))
        viewModel.restoreBackup(input)
        advanceUntilIdle()

        assertTrue(backupEngine.importCalled)
        assertFalse(viewModel.uiState.value.isRestoring)
        assertNotNull(viewModel.uiState.value.restoreSummary)
        assertEquals(5, viewModel.uiState.value.restoreSummary?.booksCount)
        // CRITICAL UX (BS-04): Show relink prompt after import
        assertTrue(viewModel.uiState.value.showRelinkPrompt)

        // User dismisses prompt
        viewModel.dismissRelinkPrompt()
        assertFalse(viewModel.uiState.value.showRelinkPrompt)
    }

    @Test
    fun testLicensesDialogToggle() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.setLicensesDialogVisible(true)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showLicensesDialog)

        viewModel.setLicensesDialogVisible(false)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.showLicensesDialog)
    }

    private fun createViewModel(): SettingsViewModel {
        return SettingsViewModel(
            settingsDao = settingsDao,
            readingLedgerDao = readingLedgerDao,
            backupEngine = backupEngine,
            clock = clock,
            logicalDayCalculator = logicalDayCalculator,
            ioDispatcher = testDispatcher
        )
    }
}
