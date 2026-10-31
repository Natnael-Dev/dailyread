package io.github.natnael_dev.dailyread

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.natnael_dev.dailyread.data.db.DailyReadDatabase
import io.github.natnael_dev.dailyread.data.db.entity.BookEntity
import io.github.natnael_dev.dailyread.data.db.entity.DailyPositionEntity
import io.github.natnael_dev.dailyread.data.db.entity.DailySummaryEntity
import io.github.natnael_dev.dailyread.data.db.entity.PlanEntity
import io.github.natnael_dev.dailyread.data.db.entity.ReadingPositionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TodayDashboardDeviceTest {

    private lateinit var db: DailyReadDatabase

    @Before
    fun setup(): Unit = runBlocking(Dispatchers.IO) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = DailyReadDatabase.getInstance(context)

        // 1. Ensure a book exists
        val bookDao = db.bookDao()
        val book = BookEntity(
            contentKey = "nature-of-thought-key",
            format = "EPUB",
            title = "The Nature of Thought",
            author = "E. R. Harrison",
            sizeBytes = 2048576L,
            totalUnits = 310,
            lastOpenedAt = System.currentTimeMillis(),
            addedAt = System.currentTimeMillis(),
            fingerprintState = "READY"
        )
        val existing = bookDao.getBookByContentKey(book.contentKey)
        val bookId = if (existing != null) {
            bookDao.updateBook(book.copy(id = existing.id))
            existing.id
        } else {
            bookDao.insertBook(book)
        }

        // 2. Insert reading position (page 142 of 310 -> ~46%)
        bookDao.upsertReadingPosition(
            ReadingPositionEntity(
                bookId = bookId,
                locatorJson = """{"href":"/ch7.xhtml","type":"application/xhtml+xml","locations":{"progression":0.46,"totalProgression":0.46}}""",
                totalProgression = 0.46,
                updatedAt = System.currentTimeMillis()
            )
        )

        // 3. Insert streak history: past 7 days qualified (Oct 24 is today)
        val ledgerDao = db.readingLedgerDao()
        for (day in 17..23) {
            val iso = "2026-10-$day"
            ledgerDao.upsertDailySummary(
                DailySummaryEntity(
                    logicalDay = iso,
                    activeSeconds = 1200L,
                    positionsCounted = 15,
                    goalType = "PAGES",
                    goalTarget = 10,
                    qualified = 1,
                    utcOffsetSeconds = 10800
                )
            )
        }

        // 4. Insert active reading plan for this book
        val planDao = db.planDao()
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
    }

    @Test
    fun testTodayDashboardLaunchesSuccessfully() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        assertNotNull(scenario)

        scenario.onActivity { activity ->
            assertNotNull(activity)
        }

        scenario.close()
    }

    @Test
    fun testContinueReadingDeepLinkTarget() = runBlocking(Dispatchers.IO) {
        val bookDao = db.bookDao()
        val book = bookDao.getBookByContentKey("nature-of-thought-key")
        assertNotNull(book)

        val position = bookDao.getReadingPosition(book!!.id)
        assertNotNull(position)
        assertEquals(0.46, position!!.totalProgression, 0.01)

        val activePlan = db.planDao().getActivePlan()
        assertNotNull(activePlan)
        assertEquals(book.id, activePlan!!.bookId)
    }

    @Test
    fun testQuickAddPagesIncrementsDatabase() = runBlocking(Dispatchers.IO) {
        val ledgerDao = db.readingLedgerDao()
        val todayIso = "2026-10-24"

        val initialCount = ledgerDao.getDistinctPositionCountForDay(todayIso)

        ledgerDao.recordDailyPosition(
            DailyPositionEntity(
                logicalDay = todayIso,
                contentKey = "nature-of-thought-key",
                positionIndex = initialCount + 1,
                dwellMs = 6000L
            )
        )

        val newCount = ledgerDao.getDistinctPositionCountForDay(todayIso)
        assertEquals(initialCount + 1, newCount)
    }
}
