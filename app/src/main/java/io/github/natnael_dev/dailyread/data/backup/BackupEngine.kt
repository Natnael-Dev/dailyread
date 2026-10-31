package io.github.natnael_dev.dailyread.data.backup

import androidx.room.withTransaction
import io.github.natnael_dev.dailyread.data.db.DailyReadDatabase
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.time.Clock

open class BackupEngine(
    protected val database: DailyReadDatabase? = null,
    private val clock: Clock = Clock.systemUTC()
) {

    open suspend fun createBackupData(): BackupData = withContext(Dispatchers.IO) {
        val db = checkNotNull(database) { "Database required for createBackupData" }
        val books = db.bookDao().getAllBooksSync()
        val bookSources = db.bookDao().getAllBookSources()
        val readingPositions = db.bookDao().getAllReadingPositions()
        val bookmarks = db.bookmarkDao().getAllBookmarks()
        val intervals = db.readingLedgerDao().getAllIntervals()
        val dailyPositions = db.readingLedgerDao().getAllDailyPositions()
        val dailySummaries = db.readingLedgerDao().getAllDailySummaries()
        val goalVersions = db.readingLedgerDao().getAllGoalVersions()
        val plans = db.planDao().getAllPlansSync()
        val settings = db.settingsDao().getAllSettings()
        val librarySources = db.librarySourceDao().getAllSourcesSync()

        BackupData(
            version = 1,
            exportedAt = clock.millis(),
            books = books,
            bookSources = bookSources,
            readingPositions = readingPositions,
            bookmarks = bookmarks,
            readingIntervals = intervals,
            dailyPositions = dailyPositions,
            dailySummaries = dailySummaries,
            goalVersions = goalVersions,
            plans = plans,
            settings = settings,
            librarySources = librarySources
        )
    }

    open suspend fun createBackupJson(): String = withContext(Dispatchers.IO) {
        val data = createBackupData()
        serializeBackupData(data)
    }

    open suspend fun exportToStream(outputStream: OutputStream): Unit = withContext(Dispatchers.IO) {
        val json = createBackupJson()
        outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write(json)
            writer.flush()
        }
    }

    open suspend fun importFromStream(inputStream: InputStream): BackupSummary = withContext(Dispatchers.IO) {
        val json = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val backupData = parseBackupJson(json)
        restoreBackup(backupData)
    }

    open suspend fun restoreFromJson(jsonString: String): BackupSummary = withContext(Dispatchers.IO) {
        val backupData = parseBackupJson(jsonString)
        restoreBackup(backupData)
    }

    open suspend fun restoreBackup(data: BackupData): BackupSummary = withContext(Dispatchers.IO) {
        val db = checkNotNull(database) { "Database required for restoreBackup" }
        db.withTransaction {
            // Restore library sources first (foreign key target for book_source)
            if (data.librarySources.isNotEmpty()) {
                db.librarySourceDao().insertSources(data.librarySources)
            }

            // 1. Books
            if (data.books.isNotEmpty()) {
                db.bookDao().insertBooks(data.books)
            }

            // 2. Book Sources
            if (data.bookSources.isNotEmpty()) {
                db.bookDao().insertBookSources(data.bookSources)
            }

            // 3. Reading Positions
            if (data.readingPositions.isNotEmpty()) {
                db.bookDao().insertReadingPositions(data.readingPositions)
            }

            // 4. Bookmarks
            if (data.bookmarks.isNotEmpty()) {
                db.bookmarkDao().insertBookmarks(data.bookmarks)
            }

            // 5. Reading Intervals
            if (data.readingIntervals.isNotEmpty()) {
                db.readingLedgerDao().insertIntervals(data.readingIntervals)
            }

            // 6. Daily Positions
            if (data.dailyPositions.isNotEmpty()) {
                db.readingLedgerDao().insertDailyPositions(data.dailyPositions)
            }

            // 7. Daily Summaries
            if (data.dailySummaries.isNotEmpty()) {
                db.readingLedgerDao().insertDailySummaries(data.dailySummaries)
            }

            // 8. Goal Versions
            if (data.goalVersions.isNotEmpty()) {
                db.readingLedgerDao().insertGoalVersions(data.goalVersions)
            }

            // 9. Plans
            if (data.plans.isNotEmpty()) {
                db.planDao().insertPlans(data.plans)
            }

            // 10. Settings
            if (data.settings.isNotEmpty()) {
                db.settingsDao().insertSettings(data.settings)
            }
        }

        BackupSummary(
            booksCount = data.books.size,
            bookSourcesCount = data.bookSources.size,
            readingPositionsCount = data.readingPositions.size,
            bookmarksCount = data.bookmarks.size,
            intervalsCount = data.readingIntervals.size,
            dailyPositionsCount = data.dailyPositions.size,
            dailySummariesCount = data.dailySummaries.size,
            goalVersionsCount = data.goalVersions.size,
            plansCount = data.plans.size,
            settingsCount = data.settings.size,
            librarySourcesCount = data.librarySources.size
        )
    }

    companion object {

        fun serializeBackupData(data: BackupData): String {
            val root = mutableMapOf<String, JsonElement>()
            root["version"] = JsonPrimitive(data.version.toString())
            root["exported_at"] = JsonPrimitive(data.exportedAt.toString())

            // Books
            root["books"] = JsonArray(data.books.map { serializeBook(it) })
            // Book Sources
            root["book_sources"] = JsonArray(data.bookSources.map { serializeBookSource(it) })
            // Reading Positions
            root["reading_positions"] = JsonArray(data.readingPositions.map { serializeReadingPosition(it) })
            // Bookmarks
            root["bookmarks"] = JsonArray(data.bookmarks.map { serializeBookmark(it) })
            // Reading Intervals
            root["reading_intervals"] = JsonArray(data.readingIntervals.map { serializeInterval(it) })
            // Daily Positions
            root["daily_positions"] = JsonArray(data.dailyPositions.map { serializeDailyPosition(it) })
            // Daily Summaries
            root["daily_summaries"] = JsonArray(data.dailySummaries.map { serializeDailySummary(it) })
            // Goal Versions
            root["goal_versions"] = JsonArray(data.goalVersions.map { serializeGoalVersion(it) })
            // Plans
            root["plans"] = JsonArray(data.plans.map { serializePlan(it) })
            // Settings
            root["settings"] = JsonArray(data.settings.map { serializeSetting(it) })
            // Library Sources
            root["library_sources"] = JsonArray(data.librarySources.map { serializeLibrarySource(it) })

            return JsonObject(root).toJsonString(indent = true)
        }

        fun parseBackupJson(json: String): BackupData {
            val root = SimpleJson.parseObject(json)
            val version = root.getInt("version").takeIf { it > 0 } ?: 1
            val exportedAt = root.getLong("exported_at")

            val books = root.getArray("books").items.mapNotNull {
                (it as? JsonObject)?.let { obj -> deserializeBook(obj) }
            }
            val bookSources = root.getArray("book_sources").items.mapNotNull {
                (it as? JsonObject)?.let { obj -> deserializeBookSource(obj) }
            }
            val readingPositions = root.getArray("reading_positions").items.mapNotNull {
                (it as? JsonObject)?.let { obj -> deserializeReadingPosition(obj) }
            }
            val bookmarks = root.getArray("bookmarks").items.mapNotNull {
                (it as? JsonObject)?.let { obj -> deserializeBookmark(obj) }
            }
            val readingIntervals = root.getArray("reading_intervals").items.mapNotNull {
                (it as? JsonObject)?.let { obj -> deserializeInterval(obj) }
            }
            val dailyPositions = root.getArray("daily_positions").items.mapNotNull {
                (it as? JsonObject)?.let { obj -> deserializeDailyPosition(obj) }
            }
            val dailySummaries = root.getArray("daily_summaries").items.mapNotNull {
                (it as? JsonObject)?.let { obj -> deserializeDailySummary(obj) }
            }
            val goalVersions = root.getArray("goal_versions").items.mapNotNull {
                (it as? JsonObject)?.let { obj -> deserializeGoalVersion(obj) }
            }
            val plans = root.getArray("plans").items.mapNotNull {
                (it as? JsonObject)?.let { obj -> deserializePlan(obj) }
            }
            val settings = root.getArray("settings").items.mapNotNull {
                (it as? JsonObject)?.let { obj -> deserializeSetting(obj) }
            }
            val librarySources = root.getArray("library_sources").items.mapNotNull {
                (it as? JsonObject)?.let { obj -> deserializeLibrarySource(obj) }
            }

            return BackupData(
                version = version,
                exportedAt = exportedAt,
                books = books,
                bookSources = bookSources,
                readingPositions = readingPositions,
                bookmarks = bookmarks,
                readingIntervals = readingIntervals,
                dailyPositions = dailyPositions,
                dailySummaries = dailySummaries,
                goalVersions = goalVersions,
                plans = plans,
                settings = settings,
                librarySources = librarySources
            )
        }

        private fun serializeBook(b: BookEntity): JsonObject {
            val map = mutableMapOf<String, JsonElement>()
            map["id"] = JsonPrimitive(b.id.toString())
            map["content_key"] = JsonPrimitive(b.contentKey, isString = true)
            b.contentHash?.let { map["content_hash"] = JsonPrimitive(it, isString = true) } ?: run { map["content_hash"] = JsonNull }
            map["format"] = JsonPrimitive(b.format, isString = true)
            map["title"] = JsonPrimitive(b.title, isString = true)
            b.author?.let { map["author"] = JsonPrimitive(it, isString = true) } ?: run { map["author"] = JsonNull }
            map["size_bytes"] = JsonPrimitive(b.sizeBytes.toString())
            b.totalUnits?.let { map["total_units"] = JsonPrimitive(it.toString()) } ?: run { map["total_units"] = JsonNull }
            b.coverPath?.let { map["cover_path"] = JsonPrimitive(it, isString = true) } ?: run { map["cover_path"] = JsonNull }
            map["added_at"] = JsonPrimitive(b.addedAt.toString())
            b.lastOpenedAt?.let { map["last_opened_at"] = JsonPrimitive(it.toString()) } ?: run { map["last_opened_at"] = JsonNull }
            b.finishedAt?.let { map["finished_at"] = JsonPrimitive(it.toString()) } ?: run { map["finished_at"] = JsonNull }
            b.previousBookId?.let { map["previous_book_id"] = JsonPrimitive(it.toString()) } ?: run { map["previous_book_id"] = JsonNull }
            map["fingerprint_state"] = JsonPrimitive(b.fingerprintState, isString = true)
            return JsonObject(map)
        }

        private fun deserializeBook(obj: JsonObject): BookEntity = BookEntity(
            id = obj.getLong("id"),
            contentKey = obj.getString("content_key"),
            contentHash = obj.getStringOrNull("content_hash"),
            format = obj.getString("format"),
            title = obj.getString("title"),
            author = obj.getStringOrNull("author"),
            sizeBytes = obj.getLong("size_bytes"),
            totalUnits = obj.getIntOrNull("total_units"),
            coverPath = obj.getStringOrNull("cover_path"),
            addedAt = obj.getLong("added_at"),
            lastOpenedAt = obj.getLongOrNull("last_opened_at"),
            finishedAt = obj.getLongOrNull("finished_at"),
            previousBookId = obj.getLongOrNull("previous_book_id"),
            fingerprintState = obj.getStringOrNull("fingerprint_state") ?: "TIER1"
        )

        private fun serializeBookSource(s: BookSourceEntity): JsonObject {
            val map = mutableMapOf<String, JsonElement>()
            map["id"] = JsonPrimitive(s.id.toString())
            map["book_id"] = JsonPrimitive(s.bookId.toString())
            s.sourceId?.let { map["source_id"] = JsonPrimitive(it.toString()) } ?: run { map["source_id"] = JsonNull }
            map["document_uri"] = JsonPrimitive(s.documentUri, isString = true)
            map["document_id"] = JsonPrimitive(s.documentId, isString = true)
            s.relativePath?.let { map["relative_path"] = JsonPrimitive(it, isString = true) } ?: run { map["relative_path"] = JsonNull }
            map["display_name"] = JsonPrimitive(s.displayName, isString = true)
            map["size_bytes"] = JsonPrimitive(s.sizeBytes.toString())
            map["last_modified"] = JsonPrimitive(s.lastModified.toString())
            map["last_seen_at"] = JsonPrimitive(s.lastSeenAt.toString())
            map["state"] = JsonPrimitive(s.state, isString = true)
            return JsonObject(map)
        }

        private fun deserializeBookSource(obj: JsonObject): BookSourceEntity = BookSourceEntity(
            id = obj.getLong("id"),
            bookId = obj.getLong("book_id"),
            sourceId = obj.getLongOrNull("source_id"),
            documentUri = obj.getString("document_uri"),
            documentId = obj.getString("document_id"),
            relativePath = obj.getStringOrNull("relative_path"),
            displayName = obj.getString("display_name"),
            sizeBytes = obj.getLong("size_bytes"),
            lastModified = obj.getLong("last_modified"),
            lastSeenAt = obj.getLong("last_seen_at"),
            state = obj.getStringOrNull("state") ?: "PRESENT"
        )

        private fun serializeReadingPosition(p: ReadingPositionEntity): JsonObject {
            val map = mutableMapOf<String, JsonElement>()
            map["book_id"] = JsonPrimitive(p.bookId.toString())
            map["locator_json"] = JsonPrimitive(p.locatorJson, isString = true)
            map["total_progression"] = JsonPrimitive(p.totalProgression.toString())
            map["updated_at"] = JsonPrimitive(p.updatedAt.toString())
            return JsonObject(map)
        }

        private fun deserializeReadingPosition(obj: JsonObject): ReadingPositionEntity = ReadingPositionEntity(
            bookId = obj.getLong("book_id"),
            locatorJson = obj.getString("locator_json"),
            totalProgression = obj.getDouble("total_progression"),
            updatedAt = obj.getLong("updated_at")
        )

        private fun serializeBookmark(b: BookmarkEntity): JsonObject {
            val map = mutableMapOf<String, JsonElement>()
            map["id"] = JsonPrimitive(b.id.toString())
            map["book_id"] = JsonPrimitive(b.bookId.toString())
            map["locator_json"] = JsonPrimitive(b.locatorJson, isString = true)
            map["title"] = JsonPrimitive(b.title, isString = true)
            map["created_at"] = JsonPrimitive(b.createdAt.toString())
            return JsonObject(map)
        }

        private fun deserializeBookmark(obj: JsonObject): BookmarkEntity = BookmarkEntity(
            id = obj.getLong("id"),
            bookId = obj.getLong("book_id"),
            locatorJson = obj.getString("locator_json"),
            title = obj.getString("title"),
            createdAt = obj.getLong("created_at")
        )

        private fun serializeInterval(i: ReadingIntervalEntity): JsonObject {
            val map = mutableMapOf<String, JsonElement>()
            map["id"] = JsonPrimitive(i.id.toString())
            map["content_key"] = JsonPrimitive(i.contentKey, isString = true)
            map["title_snapshot"] = JsonPrimitive(i.titleSnapshot, isString = true)
            map["started_at"] = JsonPrimitive(i.startedAt.toString())
            map["ended_at"] = JsonPrimitive(i.endedAt.toString())
            map["zone_id"] = JsonPrimitive(i.zoneId, isString = true)
            map["utc_offset_seconds"] = JsonPrimitive(i.utcOffsetSeconds.toString())
            map["logical_day"] = JsonPrimitive(i.logicalDay, isString = true)
            map["active_seconds"] = JsonPrimitive(i.activeSeconds.toString())
            map["positions_counted"] = JsonPrimitive(i.positionsCounted.toString())
            map["elapsed_realtime_start"] = JsonPrimitive(i.elapsedRealtimeStart.toString())
            map["clock_suspect"] = JsonPrimitive(i.clockSuspect.toString())
            map["closed"] = JsonPrimitive(i.closed.toString())
            return JsonObject(map)
        }

        private fun deserializeInterval(obj: JsonObject): ReadingIntervalEntity = ReadingIntervalEntity(
            id = obj.getLong("id"),
            contentKey = obj.getString("content_key"),
            titleSnapshot = obj.getString("title_snapshot"),
            startedAt = obj.getLong("started_at"),
            endedAt = obj.getLong("ended_at"),
            zoneId = obj.getString("zone_id"),
            utcOffsetSeconds = obj.getInt("utc_offset_seconds"),
            logicalDay = obj.getString("logical_day"),
            activeSeconds = obj.getLong("active_seconds"),
            positionsCounted = obj.getInt("positions_counted"),
            elapsedRealtimeStart = obj.getLong("elapsed_realtime_start"),
            clockSuspect = obj.getInt("clock_suspect"),
            closed = obj.getInt("closed")
        )

        private fun serializeDailyPosition(d: DailyPositionEntity): JsonObject {
            val map = mutableMapOf<String, JsonElement>()
            map["logical_day"] = JsonPrimitive(d.logicalDay, isString = true)
            map["content_key"] = JsonPrimitive(d.contentKey, isString = true)
            map["position_index"] = JsonPrimitive(d.positionIndex.toString())
            map["dwell_ms"] = JsonPrimitive(d.dwellMs.toString())
            return JsonObject(map)
        }

        private fun deserializeDailyPosition(obj: JsonObject): DailyPositionEntity = DailyPositionEntity(
            logicalDay = obj.getString("logical_day"),
            contentKey = obj.getString("content_key"),
            positionIndex = obj.getInt("position_index"),
            dwellMs = obj.getLong("dwell_ms")
        )

        private fun serializeDailySummary(s: DailySummaryEntity): JsonObject {
            val map = mutableMapOf<String, JsonElement>()
            map["logical_day"] = JsonPrimitive(s.logicalDay, isString = true)
            map["active_seconds"] = JsonPrimitive(s.activeSeconds.toString())
            map["positions_counted"] = JsonPrimitive(s.positionsCounted.toString())
            map["goal_type"] = JsonPrimitive(s.goalType, isString = true)
            map["goal_target"] = JsonPrimitive(s.goalTarget.toString())
            map["qualified"] = JsonPrimitive(s.qualified.toString())
            map["utc_offset_seconds"] = JsonPrimitive(s.utcOffsetSeconds.toString())
            return JsonObject(map)
        }

        private fun deserializeDailySummary(obj: JsonObject): DailySummaryEntity = DailySummaryEntity(
            logicalDay = obj.getString("logical_day"),
            activeSeconds = obj.getLong("active_seconds"),
            positionsCounted = obj.getInt("positions_counted"),
            goalType = obj.getString("goal_type"),
            goalTarget = obj.getInt("goal_target"),
            qualified = obj.getInt("qualified"),
            utcOffsetSeconds = obj.getInt("utc_offset_seconds")
        )

        private fun serializeGoalVersion(g: GoalVersionEntity): JsonObject {
            val map = mutableMapOf<String, JsonElement>()
            map["id"] = JsonPrimitive(g.id.toString())
            map["effective_from_day"] = JsonPrimitive(g.effectiveFromDay, isString = true)
            map["goal_type"] = JsonPrimitive(g.goalType, isString = true)
            map["target"] = JsonPrimitive(g.target.toString())
            return JsonObject(map)
        }

        private fun deserializeGoalVersion(obj: JsonObject): GoalVersionEntity = GoalVersionEntity(
            id = obj.getLong("id"),
            effectiveFromDay = obj.getString("effective_from_day"),
            goalType = obj.getString("goal_type"),
            target = obj.getInt("target")
        )

        private fun serializePlan(p: PlanEntity): JsonObject {
            val map = mutableMapOf<String, JsonElement>()
            map["id"] = JsonPrimitive(p.id.toString())
            map["book_id"] = JsonPrimitive(p.bookId.toString())
            map["start_day"] = JsonPrimitive(p.startDay, isString = true)
            map["target_day"] = JsonPrimitive(p.targetDay, isString = true)
            map["start_units"] = JsonPrimitive(p.startUnits.toString())
            p.totalUnits?.let { map["total_units"] = JsonPrimitive(it.toString()) } ?: run { map["total_units"] = JsonNull }
            map["state"] = JsonPrimitive(p.state, isString = true)
            return JsonObject(map)
        }

        private fun deserializePlan(obj: JsonObject): PlanEntity = PlanEntity(
            id = obj.getLong("id"),
            bookId = obj.getLong("book_id"),
            startDay = obj.getString("start_day"),
            targetDay = obj.getString("target_day"),
            startUnits = obj.getInt("start_units"),
            totalUnits = obj.getIntOrNull("total_units"),
            state = obj.getStringOrNull("state") ?: "ACTIVE"
        )

        private fun serializeSetting(s: SettingEntity): JsonObject {
            val map = mutableMapOf<String, JsonElement>()
            map["key"] = JsonPrimitive(s.key, isString = true)
            map["value"] = JsonPrimitive(s.value, isString = true)
            return JsonObject(map)
        }

        private fun deserializeSetting(obj: JsonObject): SettingEntity = SettingEntity(
            key = obj.getString("key"),
            value = obj.getString("value")
        )

        private fun serializeLibrarySource(s: LibrarySourceEntity): JsonObject {
            val map = mutableMapOf<String, JsonElement>()
            map["id"] = JsonPrimitive(s.id.toString())
            map["tree_uri"] = JsonPrimitive(s.treeUri, isString = true)
            map["display_name"] = JsonPrimitive(s.displayName, isString = true)
            map["added_at"] = JsonPrimitive(s.addedAt.toString())
            s.lastScanAt?.let { map["last_scan_at"] = JsonPrimitive(it.toString()) } ?: run { map["last_scan_at"] = JsonNull }
            map["state"] = JsonPrimitive(s.state, isString = true)
            return JsonObject(map)
        }

        private fun deserializeLibrarySource(obj: JsonObject): LibrarySourceEntity = LibrarySourceEntity(
            id = obj.getLong("id"),
            treeUri = obj.getString("tree_uri"),
            displayName = obj.getString("display_name"),
            addedAt = obj.getLong("added_at"),
            lastScanAt = obj.getLongOrNull("last_scan_at"),
            state = obj.getStringOrNull("state") ?: "OK"
        )
    }
}
