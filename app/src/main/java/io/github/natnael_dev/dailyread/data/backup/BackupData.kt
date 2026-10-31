package io.github.natnael_dev.dailyread.data.backup

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

data class BackupData(
    val version: Int = 1,
    val exportedAt: Long,
    val books: List<BookEntity> = emptyList(),
    val bookSources: List<BookSourceEntity> = emptyList(),
    val readingPositions: List<ReadingPositionEntity> = emptyList(),
    val bookmarks: List<BookmarkEntity> = emptyList(),
    val readingIntervals: List<ReadingIntervalEntity> = emptyList(),
    val dailyPositions: List<DailyPositionEntity> = emptyList(),
    val dailySummaries: List<DailySummaryEntity> = emptyList(),
    val goalVersions: List<GoalVersionEntity> = emptyList(),
    val plans: List<PlanEntity> = emptyList(),
    val settings: List<SettingEntity> = emptyList(),
    val librarySources: List<LibrarySourceEntity> = emptyList()
)

data class BackupSummary(
    val booksCount: Int,
    val bookSourcesCount: Int,
    val readingPositionsCount: Int,
    val bookmarksCount: Int,
    val intervalsCount: Int,
    val dailyPositionsCount: Int,
    val dailySummariesCount: Int,
    val goalVersionsCount: Int,
    val plansCount: Int,
    val settingsCount: Int,
    val librarySourcesCount: Int = 0
)
