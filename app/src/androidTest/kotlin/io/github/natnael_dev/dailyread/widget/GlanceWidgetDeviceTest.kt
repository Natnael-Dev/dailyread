package io.github.natnael_dev.dailyread.widget

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker.Result
import androidx.work.testing.TestListenableWorkerBuilder
import io.github.natnael_dev.dailyread.MainActivity
import io.github.natnael_dev.dailyread.data.db.DailyReadDatabase
import io.github.natnael_dev.dailyread.data.db.entity.BookEntity
import io.github.natnael_dev.dailyread.data.db.entity.DailyPositionEntity
import io.github.natnael_dev.dailyread.data.db.entity.DailySummaryEntity
import io.github.natnael_dev.dailyread.data.db.entity.ReadingPositionEntity
import io.github.natnael_dev.dailyread.domain.time.LogicalDayCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class GlanceWidgetDeviceTest {

    private lateinit var context: Context
    private lateinit var db: DailyReadDatabase
    private var testBookId: Long = 0L

    @Before
    fun setup(): Unit = runBlocking(Dispatchers.IO) {
        context = ApplicationProvider.getApplicationContext()
        db = DailyReadDatabase.getInstance(context)

        // Clear transient test data from previous runs to ensure test isolation
        db.openHelper.writableDatabase.execSQL("DELETE FROM daily_position")
        db.openHelper.writableDatabase.execSQL("DELETE FROM daily_summary")

        // 1. Ensure test book exists
        val bookDao = db.bookDao()
        val book = BookEntity(
            contentKey = "widget-glance-test-key",
            format = "EPUB",
            title = "Principles of Psychology",
            author = "William James",
            sizeBytes = 1048576L,
            totalUnits = 400,
            lastOpenedAt = System.currentTimeMillis(),
            addedAt = System.currentTimeMillis(),
            fingerprintState = "READY"
        )
        val existing = bookDao.getBookByContentKey(book.contentKey)
        testBookId = if (existing != null) {
            bookDao.updateBook(book.copy(id = existing.id))
            existing.id
        } else {
            bookDao.insertBook(book)
        }

        // 2. Insert reading position (progression 52%)
        bookDao.upsertReadingPosition(
            ReadingPositionEntity(
                bookId = testBookId,
                locatorJson = """{"href":"/ch5.xhtml","locations":{"progression":0.52,"totalProgression":0.52}}""",
                totalProgression = 0.52,
                updatedAt = System.currentTimeMillis()
            )
        )

        // 3. Insert streak summaries for past 7 days
        val ledgerDao = db.readingLedgerDao()
        for (day in 18..24) {
            val iso = "2026-10-$day"
            ledgerDao.upsertDailySummary(
                DailySummaryEntity(
                    logicalDay = iso,
                    activeSeconds = 1200L,
                    positionsCounted = 12,
                    goalType = "PAGES",
                    goalTarget = 10,
                    qualified = 1,
                    utcOffsetSeconds = 0
                )
            )
        }
    }

    @Test
    fun testWidgetDerivesStateDirectlyFromRoom() = runBlocking(Dispatchers.IO) {
        val today = LogicalDayCalculator().computeLogicalDay(Instant.now(), ZoneId.systemDefault())
        val todayIso = today.toIsoString()

        // Record 8 pages read today
        val ledgerDao = db.readingLedgerDao()
        for (p in 1..8) {
            ledgerDao.recordDailyPosition(
                DailyPositionEntity(
                    logicalDay = todayIso,
                    contentKey = "widget-glance-test-key",
                    positionIndex = p,
                    dwellMs = 6000L
                )
            )
        }

        // Derive state at render time
        val widgetState = WidgetStateDeriver.deriveState(db)

        assertEquals("Principles of Psychology", widgetState.lastBookTitle)
        assertEquals(testBookId, widgetState.lastBookId)
        assertEquals(52, widgetState.lastBookProgression)
        assertEquals(8, widgetState.pagesReadToday)
        assertEquals(10, widgetState.dailyGoal)
        assertEquals(false, widgetState.todayQualified)

        // Verify snapshot in Room
        val snapshot = db.settingsDao().getWidgetSnapshot()
        assertNotNull(snapshot)
        assertEquals(todayIso, snapshot!!.logicalDay)
        assertEquals(0, snapshot.todayQualified)
        assertEquals(testBookId, snapshot.lastBookId)
    }

    @Test
    fun testWidgetDeepLinkIntentLaunchesAppToBook() {
        val deepLinkIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("bookId", testBookId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val scenario = ActivityScenario.launch<MainActivity>(deepLinkIntent)
        assertNotNull(scenario)

        scenario.onActivity { activity ->
            assertNotNull(activity)
            // Activity launched successfully with intent extra
            val receivedBookId = activity.intent.getLongExtra("bookId", -1L)
            assertEquals(testBookId, receivedBookId)
        }

        scenario.close()
    }

    @Test
    fun testWidgetRolloverWorkerExecution() = runBlocking {
        val worker = TestListenableWorkerBuilder<WidgetRolloverWorker>(context).build()
        val result = worker.doWork()

        assertEquals(Result.success(), result)
    }
}
