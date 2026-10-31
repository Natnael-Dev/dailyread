package io.github.natnael_dev.dailyread

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.natnael_dev.dailyread.data.db.DailyReadDatabase
import io.github.natnael_dev.dailyread.data.db.entity.BookEntity
import io.github.natnael_dev.dailyread.data.db.entity.BookmarkEntity
import io.github.natnael_dev.dailyread.data.db.entity.ReadingPositionEntity
import io.github.natnael_dev.dailyread.data.reader.PdfReadingCache
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.adapter.pdfium.document.PdfiumDocumentFactory
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class ReaderResumeDeviceTest {

    private fun copyAssetToFile(context: Context, assetPath: String, targetFile: File) {
        targetFile.parentFile?.mkdirs()
        context.assets.open(assetPath).use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        }
    }

    @Test
    fun testEpubExactResumePersistence(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = DailyReadDatabase.getInstance(context)
        val bookDao = db.bookDao()

        // 1. Insert an EPUB book
        val epubBook = BookEntity(
            contentKey = "test_epub_key_001",
            contentHash = "hash_epub_001",
            format = "EPUB",
            title = "Test EPUB Exact Resume",
            author = "Test Author",
            sizeBytes = 1024L,
            totalUnits = 100,
            addedAt = System.currentTimeMillis()
        )
        val bookId = bookDao.insertBook(epubBook)
        assertTrue(bookId > 0)

        // 2. Persist exact locator (mid-chapter)
        val sampleLocatorJson = """{"href":"/text/chapter2.xhtml","type":"application/xhtml+xml","locations":{"progression":0.435,"totalProgression":0.18}}"""

        val position = ReadingPositionEntity(
            bookId = bookId,
            locatorJson = sampleLocatorJson,
            totalProgression = 0.18,
            updatedAt = System.currentTimeMillis()
        )
        bookDao.upsertReadingPosition(position)

        // 3. Verify exact restore from database
        val restored = bookDao.getReadingPosition(bookId)
        assertNotNull("Restored reading position must not be null", restored)
        assertEquals(sampleLocatorJson, restored?.locatorJson)
        assertEquals(0.18, restored?.totalProgression ?: 0.0, 0.0001)

        // 4. Verify opening EPUB publication with Readium parser
        val fixtureFile = File(context.cacheDir, "sample.epub")
        copyAssetToFile(context, "fixtures/sample.epub", fixtureFile)

        val httpClient = DefaultHttpClient()
        val assetRetriever = AssetRetriever(context.contentResolver, httpClient)
        val pdfFactory = PdfiumDocumentFactory(context)
        val parser = DefaultPublicationParser(context, httpClient, assetRetriever, pdfFactory)
        val opener = PublicationOpener(parser)

        val assetResult = assetRetriever.retrieve(fixtureFile)
        val asset = assetResult.getOrNull()
        assertNotNull("Asset must be retrieved", asset)

        val pubResult = opener.open(asset!!, allowUserInteraction = false)
        val publication = pubResult.getOrNull()
        assertNotNull("Publication must be opened", publication)
        assertEquals("sample.epub", fixtureFile.name)
        assertTrue(publication!!.readingOrder.isNotEmpty())
        publication.close()
    }

    @Test
    fun testPdfExactResumePersistenceAndReadingCache(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = DailyReadDatabase.getInstance(context)
        val bookDao = db.bookDao()

        // 1. Insert a PDF book
        val pdfBook = BookEntity(
            contentKey = "test_pdf_key_002",
            contentHash = "hash_pdf_002",
            format = "PDF",
            title = "Test PDF Exact Resume",
            author = "Test Author",
            sizeBytes = 2048L,
            totalUnits = 50,
            addedAt = System.currentTimeMillis()
        )
        val bookId = bookDao.insertBook(pdfBook)
        assertTrue(bookId > 0)

        // 2. Persist exact page locator
        val samplePdfLocator = """{"href":"/page/14","type":"application/pdf","locations":{"position":14,"progression":0.28}}"""
        val position = ReadingPositionEntity(
            bookId = bookId,
            locatorJson = samplePdfLocator,
            totalProgression = 0.28,
            updatedAt = System.currentTimeMillis()
        )
        bookDao.upsertReadingPosition(position)

        // 3. Verify exact restore from database
        val restored = bookDao.getReadingPosition(bookId)
        assertNotNull(restored)
        assertEquals(samplePdfLocator, restored?.locatorJson)
        assertEquals(0.28, restored?.totalProgression ?: 0.0, 0.0001)

        // 4. Verify Bounded MRU reading cache with sample PDF fixture
        val cache = PdfReadingCache(cacheDir = context.cacheDir, maxCacheSizeBytes = 50 * 1024 * 1024L)
        val cachedFile = cache.getOrCreateCachedFile(pdfBook.contentKey) {
            context.assets.open("fixtures/sample.pdf")
        }
        assertTrue("Cached file must exist", cachedFile.exists())
        assertTrue("Cached file must have content", cachedFile.length() > 0)

        // Verify Readium opens the cached PDF
        val httpClient = DefaultHttpClient()
        val assetRetriever = AssetRetriever(context.contentResolver, httpClient)
        val pdfFactory = PdfiumDocumentFactory(context)
        val parser = DefaultPublicationParser(context, httpClient, assetRetriever, pdfFactory)
        val opener = PublicationOpener(parser)

        val assetResult = assetRetriever.retrieve(cachedFile)
        val asset = assetResult.getOrNull()
        assertNotNull("PDF asset must be retrieved", asset)

        val pubResult = opener.open(asset!!, allowUserInteraction = false)
        val publication = pubResult.getOrNull()
        assertNotNull("PDF publication must open successfully", publication)
        publication?.close()
    }

    @Test
    fun testAmharicEpubRenderSmoke(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fixtureFile = File(context.cacheDir, "amharic_embedded.epub")
        copyAssetToFile(context, "fixtures/amharic_embedded.epub", fixtureFile)

        val httpClient = DefaultHttpClient()
        val assetRetriever = AssetRetriever(context.contentResolver, httpClient)
        val pdfFactory = PdfiumDocumentFactory(context)
        val parser = DefaultPublicationParser(context, httpClient, assetRetriever, pdfFactory)
        val opener = PublicationOpener(parser)

        val assetResult = assetRetriever.retrieve(fixtureFile)
        val asset = assetResult.getOrNull()
        assertNotNull("Amharic EPUB asset must be retrieved", asset)

        val pubResult = opener.open(asset!!, allowUserInteraction = false)
        val publication = pubResult.getOrNull()
        assertNotNull("Amharic EPUB publication must open", publication)

        // Verify spine reading order contains chapters
        assertTrue("Amharic EPUB reading order must not be empty", publication!!.readingOrder.isNotEmpty())

        // Read first resource and verify Ethiopic Unicode text content is present
        val firstLink = publication.readingOrder.first()
        val resource = publication.get(firstLink)
        assertNotNull("Resource must exist", resource)
        val bytesResult = resource!!.read()
        val bytes = bytesResult.getOrNull()
        assertNotNull("Resource bytes must be readable", bytes)
        val text = String(bytes!!, Charsets.UTF_8)
        assertTrue("Amharic text must contain Ethiopic Unicode block characters", text.any { it in '\u1200'..'\u137F' })

        publication.close()
    }

    @Test
    fun testBookmarkCrudOnDevice(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = DailyReadDatabase.getInstance(context)
        val bookDao = db.bookDao()
        val bookmarkDao = db.bookmarkDao()

        // 1. Create a book
        val book = BookEntity(
            contentKey = "bookmark_test_key",
            contentHash = "hash_bm",
            format = "EPUB",
            title = "Bookmark Test Volume",
            author = "Author",
            sizeBytes = 1000L,
            totalUnits = 50,
            addedAt = System.currentTimeMillis()
        )
        val bookId = bookDao.insertBook(book)

        // 2. Add two bookmarks
        val bm1Id = bookmarkDao.insertBookmark(
            BookmarkEntity(
                bookId = bookId,
                locatorJson = """{"href":"/sec1.xhtml","locations":{"progression":0.1}}""",
                title = "Chapter 1 Opening",
                createdAt = 1000L
            )
        )
        val bm2Id = bookmarkDao.insertBookmark(
            BookmarkEntity(
                bookId = bookId,
                locatorJson = """{"href":"/sec2.xhtml","locations":{"progression":0.5}}""",
                title = "Midpoint Reflection",
                createdAt = 2000L
            )
        )

        // 3. Query bookmarks via Flow
        val bookmarks = bookmarkDao.getBookmarksForBook(bookId).first()
        assertEquals(2, bookmarks.size)
        assertEquals("Midpoint Reflection", bookmarks[0].title) // ordered by createdAt DESC

        // 4. Delete one bookmark
        bookmarkDao.deleteBookmark(bm1Id)
        val afterDelete = bookmarkDao.getBookmarksForBook(bookId).first()
        assertEquals(1, afterDelete.size)
        assertEquals(bm2Id, afterDelete[0].id)
    }
}
