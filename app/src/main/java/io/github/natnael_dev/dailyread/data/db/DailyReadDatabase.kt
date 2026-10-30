package io.github.natnael_dev.dailyread.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import io.github.natnael_dev.dailyread.data.db.dao.BookDao
import io.github.natnael_dev.dailyread.data.db.dao.BookmarkDao
import io.github.natnael_dev.dailyread.data.db.dao.LibrarySourceDao
import io.github.natnael_dev.dailyread.data.db.dao.ReadingLedgerDao
import io.github.natnael_dev.dailyread.data.db.dao.SettingsDao
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

@Database(
    entities = [
        LibrarySourceEntity::class,
        BookEntity::class,
        BookSourceEntity::class,
        ReadingPositionEntity::class,
        ReadingIntervalEntity::class,
        DailyPositionEntity::class,
        DailySummaryEntity::class,
        GoalVersionEntity::class,
        PlanEntity::class,
        BookmarkEntity::class,
        SettingEntity::class,
        WidgetSnapshotEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class DailyReadDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun readingLedgerDao(): ReadingLedgerDao
    abstract fun settingsDao(): SettingsDao
    abstract fun librarySourceDao(): LibrarySourceDao
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        const val DATABASE_NAME = "dailyread.db"

        @Volatile
        private var instance: DailyReadDatabase? = null

        fun getInstance(context: Context): DailyReadDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    DailyReadDatabase::class.java,
                    DATABASE_NAME
                ).build().also { instance = it }
            }
        }
    }
}
