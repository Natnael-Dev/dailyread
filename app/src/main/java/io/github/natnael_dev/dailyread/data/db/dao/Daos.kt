package io.github.natnael_dev.dailyread.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.natnael_dev.dailyread.data.db.entity.BookEntity
import io.github.natnael_dev.dailyread.data.db.entity.BookSourceEntity
import io.github.natnael_dev.dailyread.data.db.entity.DailyPositionEntity
import io.github.natnael_dev.dailyread.data.db.entity.DailySummaryEntity
import io.github.natnael_dev.dailyread.data.db.entity.GoalVersionEntity
import io.github.natnael_dev.dailyread.data.db.entity.LibrarySourceEntity
import io.github.natnael_dev.dailyread.data.db.entity.ReadingIntervalEntity
import io.github.natnael_dev.dailyread.data.db.entity.ReadingPositionEntity
import io.github.natnael_dev.dailyread.data.db.entity.SettingEntity
import io.github.natnael_dev.dailyread.data.db.entity.WidgetSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBook(book: BookEntity): Long

    @Update
    suspend fun updateBook(book: BookEntity)

    @Query("SELECT * FROM book WHERE id = :id")
    suspend fun getBookById(id: Long): BookEntity?

    @Query("SELECT * FROM book WHERE content_key = :contentKey")
    suspend fun getBookByContentKey(contentKey: String): BookEntity?

    @Query("SELECT * FROM book ORDER BY added_at DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookSource(source: BookSourceEntity): Long

    @Query("SELECT * FROM book_source WHERE book_id = :bookId")
    suspend fun getSourcesForBook(bookId: Long): List<BookSourceEntity>

    @Query("SELECT * FROM book_source WHERE document_uri = :uri")
    suspend fun getBookSourceByDocumentUri(uri: String): BookSourceEntity?

    @Query("UPDATE book_source SET state = :state, last_seen_at = :seenAt WHERE id = :sourceId")
    suspend fun updateBookSourceState(sourceId: Long, state: String, seenAt: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReadingPosition(position: ReadingPositionEntity)

    @Query("SELECT * FROM reading_position WHERE book_id = :bookId")
    suspend fun getReadingPosition(bookId: Long): ReadingPositionEntity?
}

@Dao
interface ReadingLedgerDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertInterval(interval: ReadingIntervalEntity): Long

    @Update
    suspend fun updateInterval(interval: ReadingIntervalEntity)

    @Query("SELECT * FROM reading_interval WHERE closed = 0 LIMIT 1")
    suspend fun getOpenInterval(): ReadingIntervalEntity?

    @Query("SELECT * FROM reading_interval WHERE logical_day = :logicalDay ORDER BY started_at ASC")
    suspend fun getIntervalsForLogicalDay(logicalDay: String): List<ReadingIntervalEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun recordDailyPosition(dailyPosition: DailyPositionEntity)

    @Query("SELECT COUNT(*) FROM daily_position WHERE logical_day = :logicalDay")
    suspend fun getDistinctPositionCountForDay(logicalDay: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailySummary(summary: DailySummaryEntity)

    @Query("SELECT * FROM daily_summary WHERE logical_day = :logicalDay")
    suspend fun getDailySummary(logicalDay: String): DailySummaryEntity?

    @Query("SELECT * FROM daily_summary ORDER BY logical_day ASC")
    suspend fun getAllDailySummaries(): List<DailySummaryEntity>

    @Query("SELECT * FROM daily_summary WHERE qualified = 1 ORDER BY logical_day ASC")
    suspend fun getQualifyingDays(): List<DailySummaryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoalVersion(goalVersion: GoalVersionEntity): Long

    @Query("SELECT * FROM goal_version WHERE effective_from_day <= :logicalDay ORDER BY effective_from_day DESC LIMIT 1")
    suspend fun getActiveGoalForDay(logicalDay: String): GoalVersionEntity?
}

@Dao
interface SettingsDao {
    @Query("SELECT value FROM setting WHERE `key` = :key")
    suspend fun getSetting(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(setting: SettingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWidgetSnapshot(snapshot: WidgetSnapshotEntity)

    @Query("SELECT * FROM widget_snapshot WHERE id = 1")
    suspend fun getWidgetSnapshot(): WidgetSnapshotEntity?
}

@Dao
interface LibrarySourceDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSource(source: LibrarySourceEntity): Long

    @Query("SELECT * FROM library_source ORDER BY added_at DESC")
    fun getAllSources(): Flow<List<LibrarySourceEntity>>

    @Query("SELECT * FROM library_source WHERE tree_uri = :treeUri")
    suspend fun getSourceByTreeUri(treeUri: String): LibrarySourceEntity?

    @Query("UPDATE library_source SET last_scan_at = :timestamp WHERE id = :id")
    suspend fun updateLastScan(id: Long, timestamp: Long)

    @Query("UPDATE library_source SET state = :state WHERE id = :id")
    suspend fun updateSourceState(id: Long, state: String)
}
