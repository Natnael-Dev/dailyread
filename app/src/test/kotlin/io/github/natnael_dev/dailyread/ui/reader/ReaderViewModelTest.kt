package io.github.natnael_dev.dailyread.ui.reader

import io.github.natnael_dev.dailyread.data.db.dao.BookmarkDao
import io.github.natnael_dev.dailyread.data.db.dao.ReadingLedgerDao
import io.github.natnael_dev.dailyread.data.db.entity.BookEntity
import io.github.natnael_dev.dailyread.data.db.entity.BookmarkEntity
import io.github.natnael_dev.dailyread.data.db.entity.DailyPositionEntity
import io.github.natnael_dev.dailyread.data.db.entity.DailySummaryEntity
import io.github.natnael_dev.dailyread.data.db.entity.GoalVersionEntity
import io.github.natnael_dev.dailyread.data.db.entity.ReadingIntervalEntity
import io.github.natnael_dev.dailyread.data.db.entity.ReadingPositionEntity
import io.github.natnael_dev.dailyread.data.ingest.FakeBookDao
import io.github.natnael_dev.dailyread.domain.time.FakeClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
import java.time.Instant
import java.time.ZoneId

class FakeBookmarkDao : BookmarkDao {
    private val bookmarks = mutableMapOf<Long, BookmarkEntity>()
    private val bookmarksFlow = MutableStateFlow<List<BookmarkEntity>>(emptyList())
    private var nextId = 1L

    private fun updateFlow() {
        bookmarksFlow.value = bookmarks.values.toList().sortedByDescending { it.createdAt }
    }

    override suspend fun insertBookmark(bookmark: BookmarkEntity): Long {
        val id = if (bookmark.id == 0L) nextId++ else bookmark.id
        val entity = bookmark.copy(id = id)
        bookmarks[id] = entity
        updateFlow()
        return id
    }

    override fun getBookmarksForBook(bookId: Long): Flow<List<BookmarkEntity>> {
        return bookmarksFlow.map { list -> list.filter { it.bookId == bookId } }
    }

    override suspend fun getBookmarksForBookList(bookId: Long): List<BookmarkEntity> {
        return bookmarks.values.filter { it.bookId == bookId }.sortedByDescending { it.createdAt }
    }

    override suspend fun deleteBookmark(id: Long) {
        bookmarks.remove(id)
        updateFlow()
    }

    override suspend fun getBookmarkByLocator(bookId: Long, locatorJson: String): BookmarkEntity? {
        return bookmarks.values.firstOrNull { it.bookId == bookId && it.locatorJson == locatorJson }
    }
}

class FakeReadingLedgerDao : ReadingLedgerDao {
    val recordedIntervals = mutableListOf<ReadingIntervalEntity>()
    val recordedDailyPositions = mutableListOf<DailyPositionEntity>()

    override suspend fun insertInterval(interval: ReadingIntervalEntity): Long {
        recordedIntervals.add(interval)
        return interval.id
    }

    override suspend fun updateInterval(interval: ReadingIntervalEntity) {}
    override suspend fun getOpenInterval(): ReadingIntervalEntity? = null
    override suspend fun getIntervalsForLogicalDay(logicalDay: String): List<ReadingIntervalEntity> =
        recordedIntervals.filter { it.logicalDay == logicalDay }

    override suspend fun recordDailyPosition(dailyPosition: DailyPositionEntity) {
        if (recordedDailyPositions.none {
                it.logicalDay == dailyPosition.logicalDay &&
                it.contentKey == dailyPosition.contentKey &&
                it.positionIndex == dailyPosition.positionIndex
            }
        ) {
            recordedDailyPositions.add(dailyPosition)
        }
    }

    override suspend fun getDistinctPositionCountForDay(logicalDay: String): Int =
        recordedDailyPositions.count { it.logicalDay == logicalDay }

    override suspend fun upsertDailySummary(summary: DailySummaryEntity) {}
    override suspend fun getDailySummary(logicalDay: String): DailySummaryEntity? = null
    override suspend fun getAllDailySummaries(): List<DailySummaryEntity> = emptyList()
    override suspend fun getQualifyingDays(): List<DailySummaryEntity> = emptyList()
    override suspend fun insertGoalVersion(goalVersion: GoalVersionEntity): Long = 1L
    override suspend fun getActiveGoalForDay(logicalDay: String): GoalVersionEntity? = null
}

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val zoneId = ZoneId.of("UTC")
    private lateinit var clock: FakeClock
    private lateinit var bookDao: FakeBookDao
    private lateinit var bookmarkDao: FakeBookmarkDao
    private lateinit var readingLedgerDao: FakeReadingLedgerDao
    private lateinit var viewModel: ReaderViewModel

    private val testBook = BookEntity(
        id = 1L,
        contentKey = "walden-hash-123",
        format = "EPUB",
        title = "Walden",
        author = "Henry David Thoreau",
        sizeBytes = 250_000L,
        totalUnits = 310,
        addedAt = 1000L
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        clock = FakeClock(Instant.parse("2026-10-30T10:00:00Z"), zoneId)
        bookDao = FakeBookDao()
        bookmarkDao = FakeBookmarkDao()
        readingLedgerDao = FakeReadingLedgerDao()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialLoadRestoresSavedLocator() = runTest(testDispatcher) {
        bookDao.insertBook(testBook)
        val initialLocator = """{"href":"/chapter1.xhtml","type":"application/xhtml+xml","locations":{"progression":0.45,"position":142}}"""
        bookDao.upsertReadingPosition(
            ReadingPositionEntity(
                bookId = 1L,
                locatorJson = initialLocator,
                totalProgression = 0.45,
                updatedAt = 2000L
            )
        )

        viewModel = ReaderViewModel(
            bookId = 1L,
            bookDao = bookDao,
            bookmarkDao = bookmarkDao,
            readingLedgerDao = readingLedgerDao,
            clock = clock,
            ioDispatcher = testDispatcher
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Walden", state.title)
        assertEquals(ReaderFormat.EPUB, state.format)
        assertEquals(initialLocator, state.currentLocatorJson)
        assertEquals(0.45, state.progression, 0.001)
    }

    @Test
    fun testLocatorChangedUpdatesUiStateAndTracksReading() = runTest(testDispatcher) {
        bookDao.insertBook(testBook)
        viewModel = ReaderViewModel(
            bookId = 1L,
            bookDao = bookDao,
            bookmarkDao = bookmarkDao,
            readingLedgerDao = readingLedgerDao,
            clock = clock,
            ioDispatcher = testDispatcher
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val newLocator = """{"href":"/chapter2.xhtml","locations":{"progression":0.52,"position":160}}"""
        viewModel.onLocatorChanged(
            locatorJson = newLocator,
            progression = 0.52,
            pageNumber = 160,
            chapterTitle = "Where I Lived"
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(newLocator, state.currentLocatorJson)
        assertEquals(0.52, state.progression, 0.001)
        assertEquals(160, state.currentPage)
        assertEquals("Where I Lived", state.chapterTitle)
    }

    @Test
    fun testToggleBookmarkAddsAndRemovesBookmark() = runTest(testDispatcher) {
        bookDao.insertBook(testBook)
        viewModel = ReaderViewModel(
            bookId = 1L,
            bookDao = bookDao,
            bookmarkDao = bookmarkDao,
            readingLedgerDao = readingLedgerDao,
            clock = clock,
            ioDispatcher = testDispatcher
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val locator = """{"href":"/chapter1.xhtml","locations":{"position":142}}"""
        viewModel.onLocatorChanged(locatorJson = locator, progression = 0.45, pageNumber = 142)
        advanceUntilIdle()

        // 1. Toggle ON: bookmark should be added
        viewModel.toggleBookmark()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isBookmarked)
        assertEquals(1, viewModel.uiState.value.bookmarks.size)
        assertEquals(locator, viewModel.uiState.value.bookmarks[0].locatorJson)

        // 2. Toggle OFF: bookmark should be removed
        viewModel.toggleBookmark()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isBookmarked)
        assertEquals(0, viewModel.uiState.value.bookmarks.size)
    }

    @Test
    fun testThemeAndTypographySettingsUpdateState() = runTest(testDispatcher) {
        bookDao.insertBook(testBook)
        viewModel = ReaderViewModel(
            bookId = 1L,
            bookDao = bookDao,
            bookmarkDao = bookmarkDao,
            readingLedgerDao = readingLedgerDao,
            clock = clock,
            ioDispatcher = testDispatcher
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.setTheme(ReaderTheme.DARK)
        assertEquals(ReaderTheme.DARK, viewModel.uiState.value.theme)

        viewModel.setTheme(ReaderTheme.AMOLED)
        assertEquals(ReaderTheme.AMOLED, viewModel.uiState.value.theme)

        viewModel.setFontSize(1.4)
        assertEquals(1.4, viewModel.uiState.value.fontSizeMultiplier, 0.001)

        viewModel.setFontFamily(ReaderFontFamily.MANROPE)
        assertEquals(ReaderFontFamily.MANROPE, viewModel.uiState.value.fontFamily)
    }

    @Test
    fun testOnPausePersistsLocatorAndFlushesReadingInterval() = runTest(testDispatcher) {
        bookDao.insertBook(testBook)
        viewModel = ReaderViewModel(
            bookId = 1L,
            bookDao = bookDao,
            bookmarkDao = bookmarkDao,
            readingLedgerDao = readingLedgerDao,
            clock = clock,
            ioDispatcher = testDispatcher
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val locator = """{"href":"/chapter3.xhtml","locations":{"progression":0.60,"position":180}}"""
        viewModel.onLocatorChanged(locatorJson = locator, progression = 0.60, pageNumber = 180)

        // Advance clock by 25s
        clock.advanceSeconds(25)
        viewModel.onUserInteraction()
        advanceUntilIdle()

        // Call onPause()
        viewModel.onPause()
        advanceUntilIdle()

        // 1. Reading position must be persisted in DB
        val savedPosition = bookDao.getReadingPosition(1L)
        assertNotNull(savedPosition)
        assertEquals(locator, savedPosition?.locatorJson)
        assertEquals(0.60, savedPosition?.totalProgression ?: 0.0, 0.001)

        // 2. Reading interval must be flushed to ledger
        assertEquals(1, readingLedgerDao.recordedIntervals.size)
        val interval = readingLedgerDao.recordedIntervals[0]
        assertEquals("walden-hash-123", interval.contentKey)
        assertEquals("Walden", interval.titleSnapshot)
        assertEquals(25L, interval.activeSeconds)
        assertEquals(1, interval.closed)
    }
}
