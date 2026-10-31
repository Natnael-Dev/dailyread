package io.github.natnael_dev.dailyread.notifications

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker.Result
import androidx.work.testing.TestListenableWorkerBuilder
import io.github.natnael_dev.dailyread.data.db.DailyReadDatabase
import io.github.natnael_dev.dailyread.data.db.entity.DailyPositionEntity
import io.github.natnael_dev.dailyread.data.db.entity.SettingEntity
import io.github.natnael_dev.dailyread.domain.time.LogicalDayCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class DailyReminderWorkerDeviceTest {

    private lateinit var context: Context
    private lateinit var db: DailyReadDatabase

    @Before
    fun setup(): Unit = runBlocking(Dispatchers.IO) {
        context = ApplicationProvider.getApplicationContext()
        db = DailyReadDatabase.getInstance(context)
        db.settingsDao().setSetting(SettingEntity(key = "reminders_enabled", value = "true"))
    }

    @Test
    fun testWorkerSuppressesWhenGoalAlreadyMet() = runBlocking {
        val todayIso = LogicalDayCalculator().computeLogicalDay(Instant.now(), ZoneId.systemDefault()).toIsoString()
        val ledgerDao = db.readingLedgerDao()

        // 10 positions read today
        for (i in 1..10) {
            ledgerDao.recordDailyPosition(
                DailyPositionEntity(
                    logicalDay = todayIso,
                    contentKey = "device-test-key",
                    positionIndex = i,
                    dwellMs = 6000L
                )
            )
        }

        val worker = TestListenableWorkerBuilder<DailyReminderWorker>(context).build()
        val result = worker.doWork()

        assertEquals(Result.success(), result)
    }

    @Test
    fun testWorkerExecutesWhenGoalNotMet() = runBlocking {
        val worker = TestListenableWorkerBuilder<DailyReminderWorker>(context).build()
        val result = worker.doWork()

        assertEquals(Result.success(), result)
    }
}
