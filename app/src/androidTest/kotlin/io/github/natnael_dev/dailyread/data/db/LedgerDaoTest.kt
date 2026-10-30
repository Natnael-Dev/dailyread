package io.github.natnael_dev.dailyread.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.natnael_dev.dailyread.data.db.dao.BookDao
import io.github.natnael_dev.dailyread.data.db.dao.ReadingLedgerDao
import io.github.natnael_dev.dailyread.data.db.dao.SettingsDao
import io.github.natnael_dev.dailyread.data.db.entity.BookEntity
import io.github.natnael_dev.dailyread.data.db.entity.BookSourceEntity
import io.github.natnael_dev.dailyread.data.db.entity.DailyPositionEntity
import io.github.natnael_dev.dailyread.data.db.entity.DailySummaryEntity
import io.github.natnael_dev.dailyread.data.db.entity.ReadingIntervalEntity
import io.github.natnael_dev.dailyread.data.db.entity.SettingEntity
import io.github.natnael_dev.dailyread.data.db.entity.WidgetSnapshotEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class LedgerDaoTest {

    private lateinit var db: DailyReadDatabase
    private lateinit var bookDao: BookDao
    private lateinit var ledgerDao: ReadingLedgerDao
    private lateinit var settingsDao: SettingsDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, DailyReadDatabase::class.java)
            .build()
        bookDao = db.bookDao()
        ledgerDao = db.readingLedgerDao()
        settingsDao = db.settingsDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun bookDao_insertAndQueryRoundTrip() = runBlocking(Dispatchers.IO) {
        val book = BookEntity(
            contentKey = "content-hash-test-001",
            format = "EPUB",
            title = "Test Book Title",
            author = "Test Author",
            sizeBytes = 1048576L,
            addedAt = 1700000000000L
        )
        val id = bookDao.insertBook(book)
        val retrieved = bookDao.getBookById(id)

        assertNotNull(retrieved)
        assertEquals("Test Book Title", retrieved?.title)
        assertEquals("EPUB", retrieved?.format)
        assertEquals("content-hash-test-001", retrieved?.contentKey)

        val retrievedByKey = bookDao.getBookByContentKey("content-hash-test-001")
        assertNotNull(retrievedByKey)
        assertEquals(id, retrievedByKey?.id)
    }

    @Test
    fun bookDao_sourceInsertAndStateUpdate() = runBlocking(Dispatchers.IO) {
        val bookId = bookDao.insertBook(
            BookEntity(
                contentKey = "book-with-sources",
                format = "PDF",
                title = "PDF Book",
                sizeBytes = 2048576L,
                addedAt = 1700000000000L
            )
        )

        val source = BookSourceEntity(
            bookId = bookId,
            documentUri = "content://com.android.providers.media.documents/document/1234",
            documentId = "doc-1234",
            displayName = "sample.pdf",
            sizeBytes = 2048576L,
            lastModified = 1700000000000L,
            lastSeenAt = 1700000000000L,
            state = "PRESENT"
        )
        val sourceId = bookDao.insertBookSource(source)

        val sourcesBefore = bookDao.getSourcesForBook(bookId)
        assertEquals(1, sourcesBefore.size)
        assertEquals("PRESENT", sourcesBefore[0].state)

        bookDao.updateBookSourceState(sourceId, "MISSING", 1700000500000L)
        val sourcesAfter = bookDao.getSourcesForBook(bookId)
        assertEquals(1, sourcesAfter.size)
        assertEquals("MISSING", sourcesAfter[0].state)
    }

    @Test
    fun ledgerDao_insertAndQueryIntervalRoundTrip() = runBlocking(Dispatchers.IO) {
        val interval = ReadingIntervalEntity(
            contentKey = "content-hash-test-001",
            titleSnapshot = "Test Book Title",
            startedAt = 1700001000000L,
            endedAt = 1700001900000L,
            zoneId = "Africa/Addis_Ababa",
            utcOffsetSeconds = 10800,
            logicalDay = "2026-10-15",
            activeSeconds = 850L,
            positionsCounted = 12,
            elapsedRealtimeStart = 500000L,
            closed = 1
        )

        val id = ledgerDao.insertInterval(interval)
        val intervals = ledgerDao.getIntervalsForLogicalDay("2026-10-15")

        assertEquals(1, intervals.size)
        assertEquals(id, intervals[0].id)
        assertEquals(12, intervals[0].positionsCounted)
        assertEquals(850L, intervals[0].activeSeconds)
    }

    @Test
    fun ledgerDao_orderedQueriesForDayIntervalsAndQualifyingDays() = runBlocking(Dispatchers.IO) {
        // Insert intervals with different start times
        val int1 = ReadingIntervalEntity(
            contentKey = "book-1",
            titleSnapshot = "Book 1",
            startedAt = 1700001000000L,
            endedAt = 1700001500000L,
            zoneId = "UTC",
            utcOffsetSeconds = 0,
            logicalDay = "2026-10-15",
            activeSeconds = 400L,
            positionsCounted = 5,
            elapsedRealtimeStart = 10000L,
            closed = 1
        )
        val int2 = ReadingIntervalEntity(
            contentKey = "book-1",
            titleSnapshot = "Book 1",
            startedAt = 1700002000000L,
            endedAt = 1700002600000L,
            zoneId = "UTC",
            utcOffsetSeconds = 0,
            logicalDay = "2026-10-15",
            activeSeconds = 500L,
            positionsCounted = 7,
            elapsedRealtimeStart = 20000L,
            closed = 1
        )
        ledgerDao.insertInterval(int2) // Insert later one first
        ledgerDao.insertInterval(int1)

        val intervals = ledgerDao.getIntervalsForLogicalDay("2026-10-15")
        assertEquals(2, intervals.size)
        // Must be returned in ascending started_at order
        assertEquals(1700001000000L, intervals[0].startedAt)
        assertEquals(1700002000000L, intervals[1].startedAt)

        // Insert qualifying day summaries
        val sum1 = DailySummaryEntity("2026-10-10", 600, 10, "PAGES", 10, qualified = 1, 0)
        val sum2 = DailySummaryEntity("2026-10-12", 600, 10, "PAGES", 10, qualified = 1, 0)
        val sum3 = DailySummaryEntity("2026-10-11", 300, 4, "PAGES", 10, qualified = 0, 0) // not qualified
        ledgerDao.upsertDailySummary(sum2)
        ledgerDao.upsertDailySummary(sum1)
        ledgerDao.upsertDailySummary(sum3)

        val qualifying = ledgerDao.getQualifyingDays()
        assertEquals(2, qualifying.size)
        assertEquals("2026-10-10", qualifying[0].logicalDay)
        assertEquals("2026-10-12", qualifying[1].logicalDay)
    }

    @Test
    fun ledgerDao_dailyPositionConflictIgnoreAndDistinctCount() = runBlocking(Dispatchers.IO) {
        val pos1 = DailyPositionEntity("2026-10-15", "book-1", 10, 8000L)
        val pos2 = DailyPositionEntity("2026-10-15", "book-1", 11, 6000L)
        val posDuplicate = DailyPositionEntity("2026-10-15", "book-1", 10, 15000L) // same position 10

        ledgerDao.recordDailyPosition(pos1)
        ledgerDao.recordDailyPosition(pos2)
        ledgerDao.recordDailyPosition(posDuplicate) // Should be ignored via ON CONFLICT IGNORE

        val count = ledgerDao.getDistinctPositionCountForDay("2026-10-15")
        assertEquals(2, count)
    }

    @Test
    fun ledgerDao_dailySummaryUpsert() = runBlocking(Dispatchers.IO) {
        val initial = DailySummaryEntity("2026-10-15", 300, 5, "PAGES", 10, qualified = 0, 0)
        ledgerDao.upsertDailySummary(initial)

        val before = ledgerDao.getDailySummary("2026-10-15")
        assertEquals(0, before?.qualified)
        assertEquals(5, before?.positionsCounted)

        val updated = DailySummaryEntity("2026-10-15", 900, 12, "PAGES", 10, qualified = 1, 0)
        ledgerDao.upsertDailySummary(updated)

        val after = ledgerDao.getDailySummary("2026-10-15")
        assertEquals(1, after?.qualified)
        assertEquals(12, after?.positionsCounted)
    }

    @Test
    fun settingsDao_insertAndQueryRoundTripAndUpsert() = runBlocking(Dispatchers.IO) {
        settingsDao.setSetting(SettingEntity("day_start", "03:00"))
        val val1 = settingsDao.getSetting("day_start")
        assertEquals("03:00", val1)

        settingsDao.setSetting(SettingEntity("day_start", "05:00"))
        val val2 = settingsDao.getSetting("day_start")
        assertEquals("05:00", val2)
    }

    @Test
    fun settingsDao_widgetSnapshotUpsertAndQuery() = runBlocking(Dispatchers.IO) {
        val snapshot = WidgetSnapshotEntity(
            id = 1,
            logicalDay = "2026-10-15",
            todayQualified = 1,
            streak = 7,
            freezes = 1,
            lastBookId = 42L,
            computedAt = 1700000000000L
        )
        settingsDao.upsertWidgetSnapshot(snapshot)

        val retrieved = settingsDao.getWidgetSnapshot()
        assertNotNull(retrieved)
        assertEquals("2026-10-15", retrieved?.logicalDay)
        assertEquals(7, retrieved?.streak)
        assertEquals(1, retrieved?.freezes)
        assertEquals(42L, retrieved?.lastBookId)
    }
}
