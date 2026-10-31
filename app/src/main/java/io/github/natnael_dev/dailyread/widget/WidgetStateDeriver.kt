package io.github.natnael_dev.dailyread.widget

import io.github.natnael_dev.dailyread.data.db.DailyReadDatabase
import io.github.natnael_dev.dailyread.data.db.dao.BookDao
import io.github.natnael_dev.dailyread.data.db.dao.ReadingLedgerDao
import io.github.natnael_dev.dailyread.data.db.dao.SettingsDao
import io.github.natnael_dev.dailyread.data.db.entity.WidgetSnapshotEntity
import io.github.natnael_dev.dailyread.domain.streak.QualifyingDay
import io.github.natnael_dev.dailyread.domain.streak.StreakEngine
import io.github.natnael_dev.dailyread.domain.time.LogicalDay
import io.github.natnael_dev.dailyread.domain.time.LogicalDayCalculator
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId

data class DailyReadWidgetState(
    val streak: Int,
    val pagesReadToday: Int,
    val dailyGoal: Int,
    val lastBookId: Long?,
    val lastBookTitle: String,
    val lastBookProgression: Int,
    val todayQualified: Boolean
)

object WidgetStateDeriver {

    suspend fun deriveState(
        database: DailyReadDatabase,
        nowInstant: Instant = Instant.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): DailyReadWidgetState {
        return deriveState(
            bookDao = database.bookDao(),
            readingLedgerDao = database.readingLedgerDao(),
            settingsDao = database.settingsDao(),
            nowInstant = nowInstant,
            zoneId = zoneId
        )
    }

    suspend fun deriveState(
        bookDao: BookDao,
        readingLedgerDao: ReadingLedgerDao,
        settingsDao: SettingsDao,
        nowInstant: Instant = Instant.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): DailyReadWidgetState {
        val calculator = LogicalDayCalculator()
        val today = calculator.computeLogicalDay(nowInstant, zoneId)
        val todayIso = today.toIsoString()

        val pagesReadToday = readingLedgerDao.getDistinctPositionCountForDay(todayIso)
        val goalTarget = readingLedgerDao.getActiveGoalForDay(todayIso)?.target ?: 10

        val qualifying = readingLedgerDao.getQualifyingDays().map {
            QualifyingDay(LogicalDay.parse(it.logicalDay), it.utcOffsetSeconds)
        }
        val streakResult = StreakEngine().calculateStreak(
            qualifyingDaysInput = qualifying,
            today = today,
            currentZoneOffsetSeconds = zoneId.rules.getOffset(nowInstant).totalSeconds
        )

        val books = bookDao.getAllBooks().first()
        val lastBook = books.filter { it.lastOpenedAt != null }
            .maxByOrNull { it.lastOpenedAt ?: 0L }
            ?: books.firstOrNull()

        val position = if (lastBook != null) bookDao.getReadingPosition(lastBook.id) else null
        val progression = ((position?.totalProgression ?: 0.0) * 100).toInt().coerceIn(0, 100)

        val todayQualified = pagesReadToday >= goalTarget

        // Persist snapshot to Room (SettingsDao / WidgetSnapshotEntity)
        settingsDao.upsertWidgetSnapshot(
            WidgetSnapshotEntity(
                id = 1,
                logicalDay = todayIso,
                todayQualified = if (todayQualified) 1 else 0,
                streak = streakResult.currentStreak,
                freezes = streakResult.freezesHeld,
                lastBookId = lastBook?.id,
                computedAt = nowInstant.toEpochMilli()
            )
        )

        return DailyReadWidgetState(
            streak = streakResult.currentStreak,
            pagesReadToday = pagesReadToday,
            dailyGoal = goalTarget,
            lastBookId = lastBook?.id,
            lastBookTitle = lastBook?.title ?: "No book in progress",
            lastBookProgression = progression,
            todayQualified = todayQualified
        )
    }
}
