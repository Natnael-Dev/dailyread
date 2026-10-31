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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BackupEngineTest {

    @Test
    fun testEmptyBackupSerializationAndDeserialization() {
        val original = BackupData(
            version = 1,
            exportedAt = 1730390400000L
        )

        val json = BackupEngine.serializeBackupData(original)
        assertTrue(json.contains("\"version\": 1") || json.contains("\"version\": \"1\""))
        assertTrue(json.contains("\"exported_at\": 1730390400000") || json.contains("\"exported_at\": \"1730390400000\""))

        val restored = BackupEngine.parseBackupJson(json)
        assertEquals(1, restored.version)
        assertEquals(1730390400000L, restored.exportedAt)
        assertTrue(restored.books.isEmpty())
        assertTrue(restored.bookSources.isEmpty())
        assertTrue(restored.readingPositions.isEmpty())
        assertTrue(restored.bookmarks.isEmpty())
        assertTrue(restored.readingIntervals.isEmpty())
        assertTrue(restored.dailyPositions.isEmpty())
        assertTrue(restored.dailySummaries.isEmpty())
        assertTrue(restored.goalVersions.isEmpty())
        assertTrue(restored.plans.isEmpty())
        assertTrue(restored.settings.isEmpty())
        assertTrue(restored.librarySources.isEmpty())
    }

    @Test
    fun testComprehensiveBackupRoundTripWithAllEntities() {
        val original = BackupData(
            version = 1,
            exportedAt = 1730391200000L,
            books = listOf(
                BookEntity(
                    id = 1L,
                    contentKey = "key_great_gatsby",
                    contentHash = "hash_sha256_abcdef",
                    format = "EPUB",
                    title = "The Great Gatsby",
                    author = "F. Scott Fitzgerald",
                    sizeBytes = 1048576L,
                    totalUnits = 180,
                    coverPath = "/data/user/0/covers/1.png",
                    addedAt = 1730000000000L,
                    lastOpenedAt = 1730390000000L,
                    finishedAt = null,
                    previousBookId = null,
                    fingerprintState = "TIER2_DONE"
                ),
                BookEntity(
                    id = 2L,
                    contentKey = "key_oromia_history",
                    contentHash = null,
                    format = "PDF",
                    title = "የኢትዮጵያ ታሪክ (Ethiopian History)",
                    author = "ተክለጻድቅ መኩሪያ",
                    sizeBytes = 25000000L,
                    totalUnits = 450,
                    coverPath = null,
                    addedAt = 1730100000000L,
                    lastOpenedAt = null,
                    finishedAt = 1730350000000L,
                    previousBookId = 1L,
                    fingerprintState = "SCANNING"
                )
            ),
            bookSources = listOf(
                BookSourceEntity(
                    id = 10L,
                    bookId = 1L,
                    sourceId = 100L,
                    documentUri = "content://com.android.externalstorage.documents/document/primary%3ABooks%2Fgatsby.epub",
                    documentId = "primary:Books/gatsby.epub",
                    relativePath = "Books/gatsby.epub",
                    displayName = "gatsby.epub",
                    sizeBytes = 1048576L,
                    lastModified = 1729000000000L,
                    lastSeenAt = 1730390000000L,
                    state = "PRESENT"
                )
            ),
            readingPositions = listOf(
                ReadingPositionEntity(
                    bookId = 1L,
                    locatorJson = """{"href":"/ch1.xhtml","type":"application/xhtml+xml","locations":{"progression":0.35}}""",
                    totalProgression = 0.35,
                    updatedAt = 1730390500000L
                )
            ),
            bookmarks = listOf(
                BookmarkEntity(
                    id = 20L,
                    bookId = 1L,
                    locatorJson = """{"href":"/ch2.xhtml","locations":{"progression":0.10}}""",
                    title = "Green Light at End of Dock",
                    createdAt = 1730380000000L
                )
            ),
            readingIntervals = listOf(
                ReadingIntervalEntity(
                    id = 30L,
                    contentKey = "key_great_gatsby",
                    titleSnapshot = "The Great Gatsby",
                    startedAt = 1730388000000L,
                    endedAt = 1730389200000L,
                    zoneId = "Africa/Addis_Ababa",
                    utcOffsetSeconds = 10800,
                    logicalDay = "2026-10-31",
                    activeSeconds = 1200L,
                    positionsCounted = 14,
                    elapsedRealtimeStart = 500000L,
                    clockSuspect = 0,
                    closed = 1
                )
            ),
            dailyPositions = listOf(
                DailyPositionEntity(
                    logicalDay = "2026-10-31",
                    contentKey = "key_great_gatsby",
                    positionIndex = 42,
                    dwellMs = 45000L
                )
            ),
            dailySummaries = listOf(
                DailySummaryEntity(
                    logicalDay = "2026-10-31",
                    activeSeconds = 1200L,
                    positionsCounted = 14,
                    goalType = "PAGES",
                    goalTarget = 10,
                    qualified = 1,
                    utcOffsetSeconds = 10800
                )
            ),
            goalVersions = listOf(
                GoalVersionEntity(
                    id = 1L,
                    effectiveFromDay = "2026-10-01",
                    goalType = "PAGES",
                    target = 10
                )
            ),
            plans = listOf(
                PlanEntity(
                    id = 50L,
                    bookId = 1L,
                    startDay = "2026-10-25",
                    targetDay = "2026-11-10",
                    startUnits = 0,
                    totalUnits = 180,
                    state = "ACTIVE"
                )
            ),
            settings = listOf(
                SettingEntity(key = "app_theme", value = "DARK"),
                SettingEntity(key = "daily_goal_target", value = "15"),
                SettingEntity(key = "onboarding_completed", value = "true")
            ),
            librarySources = listOf(
                LibrarySourceEntity(
                    id = 100L,
                    treeUri = "content://com.android.externalstorage.documents/tree/primary%3ABooks",
                    displayName = "Books",
                    addedAt = 1730000000000L,
                    lastScanAt = 1730390000000L,
                    state = "OK"
                )
            )
        )

        val json = BackupEngine.serializeBackupData(original)
        val restored = BackupEngine.parseBackupJson(json)

        // Verify Books
        assertEquals(2, restored.books.size)
        val b1 = restored.books[0]
        assertEquals(1L, b1.id)
        assertEquals("key_great_gatsby", b1.contentKey)
        assertEquals("hash_sha256_abcdef", b1.contentHash)
        assertEquals("EPUB", b1.format)
        assertEquals("The Great Gatsby", b1.title)
        assertEquals("F. Scott Fitzgerald", b1.author)
        assertEquals(1048576L, b1.sizeBytes)
        assertEquals(180, b1.totalUnits)
        assertEquals("/data/user/0/covers/1.png", b1.coverPath)
        assertEquals("TIER2_DONE", b1.fingerprintState)

        val b2 = restored.books[1]
        assertEquals(2L, b2.id)
        assertEquals("የኢትዮጵያ ታሪክ (Ethiopian History)", b2.title)
        assertEquals("ተክለጻድቅ መኩሪያ", b2.author)
        assertNull(b2.contentHash)
        assertNull(b2.coverPath)
        assertEquals(1L, b2.previousBookId)

        // Verify Sources
        assertEquals(1, restored.bookSources.size)
        val src = restored.bookSources[0]
        assertEquals(10L, src.id)
        assertEquals(1L, src.bookId)
        assertEquals("primary:Books/gatsby.epub", src.documentId)
        assertEquals("PRESENT", src.state)

        // Verify Reading Positions
        assertEquals(1, restored.readingPositions.size)
        val pos = restored.readingPositions[0]
        assertEquals(1L, pos.bookId)
        assertEquals(0.35, pos.totalProgression, 0.001)

        // Verify Bookmarks
        assertEquals(1, restored.bookmarks.size)
        assertEquals("Green Light at End of Dock", restored.bookmarks[0].title)

        // Verify Intervals
        assertEquals(1, restored.readingIntervals.size)
        val iv = restored.readingIntervals[0]
        assertEquals("2026-10-31", iv.logicalDay)
        assertEquals(1200L, iv.activeSeconds)
        assertEquals(14, iv.positionsCounted)

        // Verify Summaries & Goal
        assertEquals(1, restored.dailySummaries.size)
        assertEquals(1, restored.dailySummaries[0].qualified)
        assertEquals(1, restored.goalVersions.size)
        assertEquals(10, restored.goalVersions[0].target)

        // Verify Plans & Settings
        assertEquals(1, restored.plans.size)
        assertEquals("ACTIVE", restored.plans[0].state)
        assertEquals(3, restored.settings.size)
        val themeSetting = restored.settings.firstOrNull { it.key == "app_theme" }
        assertNotNull(themeSetting)
        assertEquals("DARK", themeSetting?.value)

        // Verify Library Sources
        assertEquals(1, restored.librarySources.size)
        assertEquals("Books", restored.librarySources[0].displayName)
    }

    @Test
    fun testEscapingSpecialCharactersAndUnicode() {
        val trickyString = "Quotes: \"hello\", backslash: \\, newline: \n, tab: \t, unicode: መጽሐፍ 🌟"
        val original = BackupData(
            version = 1,
            exportedAt = 1000L,
            books = listOf(
                BookEntity(
                    id = 99L,
                    contentKey = "tricky_key",
                    format = "EPUB",
                    title = trickyString,
                    author = "Special \\\"Author\\\"",
                    sizeBytes = 100L,
                    addedAt = 1000L
                )
            )
        )

        val json = BackupEngine.serializeBackupData(original)
        val restored = BackupEngine.parseBackupJson(json)

        assertEquals(1, restored.books.size)
        assertEquals(trickyString, restored.books[0].title)
        assertEquals("Special \\\"Author\\\"", restored.books[0].author)
    }

    @Test
    fun testMalformedJsonThrowsException() {
        try {
            BackupEngine.parseBackupJson("{ unquoted_key: 123 }")
            fail("Expected exception for malformed JSON")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message != null)
        }
    }
}
