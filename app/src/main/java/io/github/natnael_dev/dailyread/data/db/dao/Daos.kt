package io.github.natnael_dev.dailyread.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.natnael_dev.dailyread.data.db.entity.BookEntity
import io.github.natnael_dev.dailyread.data.db.entity.BookSourceEntity
import io.github.natnael_dev.dailyread.data.db.entity.BookmarkEntity
import io.github.natnael_dev.dailyread.data.db.entity.DailyPositionEntity
import io.github.natnael_dev.dailyread.data.db.entity.DailySummaryEntity
import io.github.natnael_dev.dailyread.data.db.entity.GoalVersionEntity
import io.github.natnael_dev.dailyread.data.db.entity.LibrarySourceEntity
import io.github.natnael_dev.dailyread.data.db.entity.PlanEntity
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

    @Query("SELECT * FROM book WHERE content_hash = :contentHash LIMIT 1")
    suspend fun getBookByContentHash(contentHash: String): BookEntity?

    @Query("DELETE FROM book WHERE id = :id")
    suspend fun deleteBookById(id: Long)

    @Query("UPDATE book_source SET book_id = :newBookId WHERE id = :sourceId")
    suspend fun updateBookSourceBookId(sourceId: Long, newBookId: Long)

    @Query("SELECT * FROM book_source WHERE source_id = :sourceId")
    suspend fun getSourcesForLibrarySource(sourceId: Long): List<BookSourceEntity>

    @Query("SELECT * FROM book WHERE fingerprint_state != 'READY'")
    suspend fun getBooksPendingFingerprint(): List<BookEntity>

    @Query("SELECT * FROM book_source")
    suspend fun getAllBookSources(): List<BookSourceEntity>

    @Query("SELECT * FROM book_source")
    fun getAllBookSourcesFlow(): Flow<List<BookSourceEntity>>

    @Query("SELECT * FROM book WHERE title LIKE '%' || :query || '%' OR (author IS NOT NULL AND author LIKE '%' || :query || '%') ORDER BY added_at DESC")
    fun searchBooks(query: String): Flow<List<BookEntity>>

    @Query("SELECT * FROM book ORDER BY id ASC")
    suspend fun getAllBooksSync(): List<BookEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<BookEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookSources(sources: List<BookSourceEntity>)

    @Query("SELECT * FROM reading_position")
    suspend fun getAllReadingPositions(): List<ReadingPositionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadingPositions(positions: List<ReadingPositionEntity>)

    @Query("SELECT * FROM book_source WHERE document_id = :documentId LIMIT 1")
    suspend fun findSourceByDocumentId(documentId: String): BookSourceEntity?

    @Query("SELECT * FROM book_source WHERE display_name = :displayName AND size_bytes = :sizeBytes LIMIT 1")
    suspend fun findSourceByFilenameAndSize(displayName: String, sizeBytes: Long): BookSourceEntity?
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

    @Query("SELECT * FROM reading_interval ORDER BY id ASC")
    suspend fun getAllIntervals(): List<ReadingIntervalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntervals(intervals: List<ReadingIntervalEntity>)

    @Query("SELECT * FROM daily_position")
    suspend fun getAllDailyPositions(): List<DailyPositionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyPositions(positions: List<DailyPositionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailySummaries(summaries: List<DailySummaryEntity>)

    @Query("SELECT * FROM goal_version ORDER BY id ASC")
    suspend fun getAllGoalVersions(): List<GoalVersionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoalVersions(goalVersions: List<GoalVersionEntity>)
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

    @Query("SELECT * FROM setting")
    suspend fun getAllSettings(): List<SettingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: List<SettingEntity>)
}

@Dao
interface LibrarySourceDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSource(source: LibrarySourceEntity): Long

    @Query("SELECT * FROM library_source ORDER BY added_at DESC")
    fun getAllSources(): Flow<List<LibrarySourceEntity>>

    @Query("SELECT * FROM library_source ORDER BY id ASC")
    suspend fun getAllSourcesSync(): List<LibrarySourceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSources(sources: List<LibrarySourceEntity>)

    @Query("SELECT * FROM library_source WHERE tree_uri = :treeUri")
    suspend fun getSourceByTreeUri(treeUri: String): LibrarySourceEntity?

    @Query("UPDATE library_source SET last_scan_at = :timestamp WHERE id = :id")
    suspend fun updateLastScan(id: Long, timestamp: Long)

    @Query("UPDATE library_source SET state = :state WHERE id = :id")
    suspend fun updateSourceState(id: Long, state: String)

    @Query("SELECT * FROM library_source WHERE id = :id")
    suspend fun getSourceById(id: Long): LibrarySourceEntity?

    @Query("SELECT * FROM library_source WHERE state = :state")
    suspend fun getSourcesByState(state: String): List<LibrarySourceEntity>
}

@Dao
interface BookmarkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Query("SELECT * FROM bookmark WHERE book_id = :bookId ORDER BY created_at DESC")
    fun getBookmarksForBook(bookId: Long): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmark WHERE book_id = :bookId ORDER BY created_at DESC")
    suspend fun getBookmarksForBookList(bookId: Long): List<BookmarkEntity>

    @Query("SELECT * FROM bookmark ORDER BY id ASC")
    suspend fun getAllBookmarks(): List<BookmarkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmarks(bookmarks: List<BookmarkEntity>)

    @Query("DELETE FROM bookmark WHERE id = :id")
    suspend fun deleteBookmark(id: Long)

    @Query("SELECT * FROM bookmark WHERE book_id = :bookId AND locator_json = :locatorJson LIMIT 1")
    suspend fun getBookmarkByLocator(bookId: Long, locatorJson: String): BookmarkEntity?
}

@Dao
interface PlanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: PlanEntity): Long

    @Update
    suspend fun updatePlan(plan: PlanEntity)

    @Query("SELECT * FROM plan WHERE state = 'ACTIVE' LIMIT 1")
    suspend fun getActivePlan(): PlanEntity?

    @Query("SELECT * FROM plan WHERE state = 'ACTIVE' LIMIT 1")
    fun getActivePlanFlow(): Flow<PlanEntity?>

    @Query("SELECT * FROM plan WHERE book_id = :bookId AND state = 'ACTIVE' LIMIT 1")
    suspend fun getActivePlanForBook(bookId: Long): PlanEntity?

    @Query("SELECT * FROM plan WHERE book_id = :bookId AND state = 'ACTIVE' LIMIT 1")
    fun getActivePlanForBookFlow(bookId: Long): Flow<PlanEntity?>

    @Query("SELECT * FROM plan ORDER BY id DESC")
    fun getAllPlansFlow(): Flow<List<PlanEntity>>

    @Query("SELECT * FROM plan ORDER BY id ASC")
    suspend fun getAllPlansSync(): List<PlanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlans(plans: List<PlanEntity>)

    @Query("UPDATE plan SET state = 'DELETED' WHERE id = :id")
    suspend fun deletePlan(id: Long)
}
