package io.github.natnael_dev.dailyread

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.natnael_dev.dailyread.data.backup.BackupEngine
import io.github.natnael_dev.dailyread.data.db.DailyReadDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test verifying:
 * 1. Mock JSON backup deserialization and database population.
 * 2. BS-04 SAF re-permission workflow after backup import (re-linking folder trees and confirming books & progress).
 * 3. Round-trip re-export from the restored Room database.
 */
@RunWith(AndroidJUnit4::class)
class BackupRestoreDeviceTest {

    @Test
    fun testMockBackupRestoreAndSafRePermissionFlow() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = DailyReadDatabase.getInstance(context)
        val backupEngine = BackupEngine(db)

        // Mock backup payload representing a restored user state
        val mockBackupJson = """
        {
          "version": 1,
          "exported_at": 1730390400000,
          "library_sources": [
            {
              "id": 10,
              "tree_uri": "content://com.android.externalstorage.documents/tree/primary%3ATestBooksFolder",
              "display_name": "TestBooksFolder",
              "added_at": 1730300000000,
              "last_scan_at": 1730310000000,
              "state": "OK"
            }
          ],
          "books": [
            {
              "id": 101,
              "content_key": "key_city_of_god_123",
              "content_hash": "hash_city_of_god_123",
              "format": "EPUB",
              "title": "The City of God",
              "author": "Augustine of Hippo",
              "total_units": 500,
              "unit_type": "SPINE_ITEM",
              "added_at": 1730300000000,
              "state": "PRESENT",
              "fingerprint_state": "READY"
            }
          ],
          "book_sources": [
            {
              "id": 201,
              "book_id": 101,
              "source_id": 10,
              "document_uri": "content://com.android.externalstorage.documents/tree/primary%3ATestBooksFolder/document/cityofgod.epub",
              "document_id": "primary:TestBooksFolder/cityofgod.epub",
              "relative_path": "cityofgod.epub",
              "display_name": "cityofgod.epub",
              "size_bytes": 2048000,
              "last_modified": 1730200000000,
              "last_seen_at": 1730310000000,
              "state": "OK"
            }
          ],
          "reading_positions": [
            {
              "book_id": 101,
              "locator_json": "{\"href\":\"chap01.xhtml\",\"progression\":0.5}",
              "total_progression": 0.42,
              "updated_at": 1730380000000
            }
          ],
          "bookmarks": [],
          "reading_intervals": [],
          "daily_positions": [],
          "daily_summaries": [],
          "goal_versions": [],
          "plans": [
            {
              "id": 501,
              "book_id": 101,
              "start_day": "2026-10-01",
              "target_day": "2026-11-10",
              "start_units": 0,
              "total_units": 500,
              "state": "ACTIVE"
            }
          ],
          "settings": [
            {
              "key": "app_theme",
              "value": "DARK"
            },
            {
              "key": "daily_goal_minutes",
              "value": "30"
            }
          ]
        }
        """.trimIndent()

        // 1. Import backup into Room database
        val summary = backupEngine.restoreFromJson(mockBackupJson)
        assertEquals(1, summary.librarySourcesCount)
        assertEquals(1, summary.booksCount)
        assertEquals(1, summary.bookSourcesCount)
        assertEquals(1, summary.readingPositionsCount)
        assertEquals(1, summary.plansCount)
        assertEquals(2, summary.settingsCount)

        // 2. Verify restored entities in Room
        val restoredBook = db.bookDao().getBookById(101)
        assertNotNull(restoredBook)
        assertEquals("The City of God", restoredBook?.title)
        assertEquals("Augustine of Hippo", restoredBook?.author)

        val restoredPosition = db.bookDao().getReadingPosition(101)
        assertNotNull(restoredPosition)
        assertEquals(0.42, restoredPosition?.totalProgression ?: 0.0, 0.001)
        assertTrue(restoredPosition?.locatorJson?.contains("chap01.xhtml") == true)

        val activePlan = db.planDao().getActivePlan()
        assertNotNull(activePlan)
        assertEquals(101L, activePlan?.bookId)
        assertEquals("2026-10-01", activePlan?.startDay)
        assertEquals("2026-11-10", activePlan?.targetDay)
        assertEquals(500, activePlan?.totalUnits)

        val themeSetting = db.settingsDao().getSetting("app_theme")
        assertEquals("DARK", themeSetting)

        val goalSetting = db.settingsDao().getSetting("daily_goal_minutes")
        assertEquals("30", goalSetting)

        // 3. BS-04: Simulate SAF re-permission workflow
        // When user re-selects folder via ACTION_OPEN_DOCUMENT_TREE, app matches the tree URI
        val userSelectedTreeUri = "content://com.android.externalstorage.documents/tree/primary%3ATestBooksFolder"
        val source = db.librarySourceDao().getSourceByTreeUri(userSelectedTreeUri)
        assertNotNull("LibrarySource for restored folder tree must be found", source)
        
        // Re-grant / mark active
        db.librarySourceDao().updateSourceState(source!!.id, "OK")
        val updatedSource = db.librarySourceDao().getSourceById(source.id)
        assertEquals("OK", updatedSource?.state)

        // Verify book sources attached to this folder are linked
        val bookSources = db.bookDao().getSourcesForLibrarySource(source.id)
        assertEquals(1, bookSources.size)
        assertEquals(101L, bookSources[0].bookId)
        assertEquals(
            "content://com.android.externalstorage.documents/tree/primary%3ATestBooksFolder/document/cityofgod.epub",
            bookSources[0].documentUri
        )

        // Verify reading progress remains preserved after SAF re-link
        val positionAfterRelink = db.bookDao().getReadingPosition(101)
        assertNotNull(positionAfterRelink)
        assertEquals(0.42, positionAfterRelink?.totalProgression ?: 0.0, 0.001)

        // 4. Test re-exporting the restored state
        val reExportedJson = backupEngine.createBackupJson()
        assertTrue(reExportedJson.contains("The City of God"))
        assertTrue(reExportedJson.contains("primary%3ATestBooksFolder"))
        assertTrue(reExportedJson.contains("\"daily_goal_minutes\""))
        assertTrue(reExportedJson.contains("\"app_theme\""))
    }
}
