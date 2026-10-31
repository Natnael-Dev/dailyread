package io.github.natnael_dev.dailyread.data.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File

class PdfReadingCacheTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var cacheDir: File
    private lateinit var readingCache: PdfReadingCache

    @Before
    fun setup() {
        cacheDir = tempFolder.newFolder("cache")
        readingCache = PdfReadingCache(
            cacheDir = cacheDir,
            maxCacheSizeBytes = 1000L, // 1000 bytes for test budget
            maxCacheEntries = 3
        )
    }

    @Test
    fun `testCreateAndRetrieveCachedFile streams into mru directory`() {
        val dummyPdfData = ByteArray(100) { 0x25 } // "%" in ASCII
        val file = readingCache.getOrCreateCachedFile("book_1") {
            ByteArrayInputStream(dummyPdfData)
        }

        assertNotNull(file)
        assertTrue(file.exists())
        assertEquals(100L, file.length())
        assertEquals("book_1.pdf", file.name)
        assertEquals(1, readingCache.getCachedFileCount())
        assertEquals(100L, readingCache.getCurrentCacheSizeBytes())
    }

    @Test
    fun `testExistingFileTouchesLruTimestamp and does not re-stream`() {
        var streamOpenCount = 0
        val dummyPdfData = ByteArray(50) { 0x20 }

        val file1 = readingCache.getOrCreateCachedFile("book_1") {
            streamOpenCount++
            ByteArrayInputStream(dummyPdfData)
        }
        assertEquals(1, streamOpenCount)

        // Set older modified time
        file1.setLastModified(1000L)

        val file2 = readingCache.getOrCreateCachedFile("book_1") {
            streamOpenCount++
            ByteArrayInputStream(dummyPdfData)
        }

        // Stream supplier should not have been called a second time
        assertEquals(1, streamOpenCount)
        assertEquals(file1.absolutePath, file2.absolutePath)
        assertTrue(file2.lastModified() > 1000L)
    }

    @Test
    fun `testLruEvictionWhenSizeCapExceeded evicts oldest file`() {
        // Budget is 1000 bytes. Create 3 files of 400 bytes each -> total 1200 > 1000!
        val file1Data = ByteArray(400) { 1 }
        val file2Data = ByteArray(400) { 2 }
        val file3Data = ByteArray(400) { 3 }

        val f1 = readingCache.getOrCreateCachedFile("book_1") { ByteArrayInputStream(file1Data) }
        f1.setLastModified(1000L)

        val f2 = readingCache.getOrCreateCachedFile("book_2") { ByteArrayInputStream(file2Data) }
        f2.setLastModified(2000L)

        // Creating file 3 pushes total size to 1200 bytes, exceeding 1000 bytes budget
        val f3 = readingCache.getOrCreateCachedFile("book_3") { ByteArrayInputStream(file3Data) }
        f3.setLastModified(3000L)

        // Oldest file (book_1) must be evicted
        assertFalse("Oldest file book_1 should have been evicted", f1.exists())
        assertTrue("book_2 should still exist", f2.exists())
        assertTrue("Newly added book_3 should exist", f3.exists())
        assertTrue(readingCache.getCurrentCacheSizeBytes() <= 1000L)
    }

    @Test
    fun `testLruEvictionWhenEntryCapExceeded evicts oldest file`() {
        // Max entries is 3. Create 4 small files of 100 bytes.
        val dummyData = ByteArray(100) { 0x41 }

        val f1 = readingCache.getOrCreateCachedFile("book_1") { ByteArrayInputStream(dummyData) }
        f1.setLastModified(1000L)

        val f2 = readingCache.getOrCreateCachedFile("book_2") { ByteArrayInputStream(dummyData) }
        f2.setLastModified(2000L)

        val f3 = readingCache.getOrCreateCachedFile("book_3") { ByteArrayInputStream(dummyData) }
        f3.setLastModified(3000L)

        assertEquals(3, readingCache.getCachedFileCount())

        val f4 = readingCache.getOrCreateCachedFile("book_4") { ByteArrayInputStream(dummyData) }
        f4.setLastModified(4000L)

        // Cap is 3, so oldest (book_1) must be evicted
        assertEquals(3, readingCache.getCachedFileCount())
        assertFalse(f1.exists())
        assertTrue(f2.exists())
        assertTrue(f3.exists())
        assertTrue(f4.exists())
    }

    @Test
    fun `testClearCache removes all cached files`() {
        val dummyData = ByteArray(50) { 0x41 }
        readingCache.getOrCreateCachedFile("book_1") { ByteArrayInputStream(dummyData) }
        readingCache.getOrCreateCachedFile("book_2") { ByteArrayInputStream(dummyData) }

        assertEquals(2, readingCache.getCachedFileCount())
        readingCache.clearCache()
        assertEquals(0, readingCache.getCachedFileCount())
        assertEquals(0L, readingCache.getCurrentCacheSizeBytes())
    }

    @Test(expected = InsufficientStorageException::class)
    fun `testLowStorageFailsGracefullyAndThrowsInsufficientStorageException`() {
        val lowStorageCache = PdfReadingCache(
            cacheDir = cacheDir,
            maxCacheSizeBytes = 1000L,
            maxCacheEntries = 3,
            minRequiredFreeBytes = Long.MAX_VALUE // Simulates critically low storage space
        )

        lowStorageCache.getOrCreateCachedFile("book_low_storage") {
            ByteArrayInputStream(ByteArray(50) { 0x41 })
        }
    }
}
