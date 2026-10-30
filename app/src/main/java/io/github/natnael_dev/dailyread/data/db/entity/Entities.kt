package io.github.natnael_dev.dailyread.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "library_source",
    indices = [Index(value = ["tree_uri"], unique = true)]
)
data class LibrarySourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "tree_uri") val treeUri: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "added_at") val addedAt: Long,
    @ColumnInfo(name = "last_scan_at") val lastScanAt: Long? = null,
    @ColumnInfo(name = "state") val state: String = "OK" // OK, PERMISSION_LOST
)

@Entity(
    tableName = "book",
    indices = [Index(value = ["content_key"], unique = true)]
)
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "content_key") val contentKey: String,
    @ColumnInfo(name = "content_hash") val contentHash: String? = null,
    @ColumnInfo(name = "format") val format: String, // EPUB, PDF
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "author") val author: String? = null,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long,
    @ColumnInfo(name = "total_units") val totalUnits: Int? = null,
    @ColumnInfo(name = "cover_path") val coverPath: String? = null,
    @ColumnInfo(name = "added_at") val addedAt: Long,
    @ColumnInfo(name = "last_opened_at") val lastOpenedAt: Long? = null,
    @ColumnInfo(name = "finished_at") val finishedAt: Long? = null,
    @ColumnInfo(name = "previous_book_id") val previousBookId: Long? = null,
    @ColumnInfo(name = "fingerprint_state") val fingerprintState: String = "TIER1" // PENDING, TIER1, TIER2_DONE, FAILED
)

@Entity(
    tableName = "book_source",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["book_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = LibrarySourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["source_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["document_id", "size_bytes", "last_modified"]),
        Index(value = ["book_id"]),
        Index(value = ["source_id"])
    ]
)
data class BookSourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "book_id") val bookId: Long,
    @ColumnInfo(name = "source_id") val sourceId: Long? = null,
    @ColumnInfo(name = "document_uri") val documentUri: String,
    @ColumnInfo(name = "document_id") val documentId: String,
    @ColumnInfo(name = "relative_path") val relativePath: String? = null,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long,
    @ColumnInfo(name = "last_modified") val lastModified: Long,
    @ColumnInfo(name = "last_seen_at") val lastSeenAt: Long,
    @ColumnInfo(name = "state") val state: String = "PRESENT" // PRESENT, MISSING, UNREADABLE
)

@Entity(
    tableName = "reading_position",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["book_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ReadingPositionEntity(
    @PrimaryKey @ColumnInfo(name = "book_id") val bookId: Long,
    @ColumnInfo(name = "locator_json") val locatorJson: String,
    @ColumnInfo(name = "total_progression") val totalProgression: Double,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)

@Entity(
    tableName = "reading_interval",
    indices = [Index(value = ["logical_day"])]
)
data class ReadingIntervalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "content_key") val contentKey: String,
    @ColumnInfo(name = "title_snapshot") val titleSnapshot: String,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "ended_at") val endedAt: Long,
    @ColumnInfo(name = "zone_id") val zoneId: String,
    @ColumnInfo(name = "utc_offset_seconds") val utcOffsetSeconds: Int,
    @ColumnInfo(name = "logical_day") val logicalDay: String,
    @ColumnInfo(name = "active_seconds") val activeSeconds: Long,
    @ColumnInfo(name = "positions_counted") val positionsCounted: Int,
    @ColumnInfo(name = "elapsed_realtime_start") val elapsedRealtimeStart: Long,
    @ColumnInfo(name = "clock_suspect") val clockSuspect: Int = 0,
    @ColumnInfo(name = "closed") val closed: Int = 0
)

@Entity(
    tableName = "daily_position",
    primaryKeys = ["logical_day", "content_key", "position_index"]
)
data class DailyPositionEntity(
    @ColumnInfo(name = "logical_day") val logicalDay: String,
    @ColumnInfo(name = "content_key") val contentKey: String,
    @ColumnInfo(name = "position_index") val positionIndex: Int,
    @ColumnInfo(name = "dwell_ms") val dwellMs: Long
)

@Entity(tableName = "daily_summary")
data class DailySummaryEntity(
    @PrimaryKey @ColumnInfo(name = "logical_day") val logicalDay: String,
    @ColumnInfo(name = "active_seconds") val activeSeconds: Long,
    @ColumnInfo(name = "positions_counted") val positionsCounted: Int,
    @ColumnInfo(name = "goal_type") val goalType: String,
    @ColumnInfo(name = "goal_target") val goalTarget: Int,
    @ColumnInfo(name = "qualified") val qualified: Int, // 0 or 1
    @ColumnInfo(name = "utc_offset_seconds") val utcOffsetSeconds: Int
)

@Entity(
    tableName = "goal_version",
    indices = [Index(value = ["effective_from_day"], unique = true)]
)
data class GoalVersionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "effective_from_day") val effectiveFromDay: String,
    @ColumnInfo(name = "goal_type") val goalType: String,
    @ColumnInfo(name = "target") val target: Int
)

@Entity(
    tableName = "plan",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["book_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["book_id"])]
)
data class PlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "book_id") val bookId: Long,
    @ColumnInfo(name = "start_day") val startDay: String,
    @ColumnInfo(name = "target_day") val targetDay: String,
    @ColumnInfo(name = "start_units") val startUnits: Int,
    @ColumnInfo(name = "total_units") val totalUnits: Int? = null,
    @ColumnInfo(name = "state") val state: String = "ACTIVE" // ACTIVE, DONE, DELETED
)

@Entity(
    tableName = "bookmark",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["book_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["book_id"])]
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "book_id") val bookId: Long,
    @ColumnInfo(name = "locator_json") val locatorJson: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

@Entity(tableName = "setting")
data class SettingEntity(
    @PrimaryKey @ColumnInfo(name = "key") val key: String,
    @ColumnInfo(name = "value") val value: String
)

@Entity(tableName = "widget_snapshot")
data class WidgetSnapshotEntity(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "logical_day") val logicalDay: String,
    @ColumnInfo(name = "today_qualified") val todayQualified: Int,
    @ColumnInfo(name = "streak") val streak: Int,
    @ColumnInfo(name = "freezes") val freezes: Int,
    @ColumnInfo(name = "last_book_id") val lastBookId: Long? = null,
    @ColumnInfo(name = "computed_at") val computedAt: Long
)
