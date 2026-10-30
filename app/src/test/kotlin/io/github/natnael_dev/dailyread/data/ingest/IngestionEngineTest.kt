package io.github.natnael_dev.dailyread.data.ingest

import io.github.natnael_dev.dailyread.data.db.dao.BookDao
import io.github.natnael_dev.dailyread.data.db.dao.LibrarySourceDao
import io.github.natnael_dev.dailyread.data.db.entity.BookEntity
import io.github.natnael_dev.dailyread.data.db.entity.BookSourceEntity
import io.github.natnael_dev.dailyread.data.db.entity.LibrarySourceEntity
import io.github.natnael_dev.dailyread.data.db.entity.ReadingPositionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class FakeBookDao : BookDao {
    private val books = mutableMapOf<Long, BookEntity>()
    private val sources = mutableMapOf<Long, BookSourceEntity>()
    private val booksFlow = MutableStateFlow<List<BookEntity>>(emptyList())
    private val sourcesFlow = MutableStateFlow<List<BookSourceEntity>>(emptyList())
    private var nextBookId = 1L
    private var nextSourceId = 1L

    private fun updateFlows() {
        booksFlow.value = books.values.toList().sortedByDescending { it.addedAt }
        sourcesFlow.value = sources.values.toList()
    }

    override suspend fun insertBook(book: BookEntity): Long {
        val id = nextBookId++
        val entity = book.copy(id = id)
        books[id] = entity
        updateFlows()
        return id
    }

    override suspend fun updateBook(book: BookEntity) {
        books[book.id] = book
        updateFlows()
    }

    override suspend fun getBookById(id: Long): BookEntity? = books[id]

    override suspend fun getBookByContentKey(contentKey: String): BookEntity? =
        books.values.firstOrNull { it.contentKey == contentKey }

    override fun getAllBooks(): Flow<List<BookEntity>> = booksFlow

    override suspend fun insertBookSource(source: BookSourceEntity): Long {
        val id = if (source.id == 0L) nextSourceId++ else source.id
        val entity = source.copy(id = id)
        sources[id] = entity
        updateFlows()
        return id
    }

    override suspend fun getSourcesForBook(bookId: Long): List<BookSourceEntity> =
        sources.values.filter { it.bookId == bookId }

    override suspend fun getBookSourceByDocumentUri(uri: String): BookSourceEntity? =
        sources.values.firstOrNull { it.documentUri == uri }

    override suspend fun updateBookSourceState(sourceId: Long, state: String, seenAt: Long) {
        sources[sourceId]?.let {
            sources[sourceId] = it.copy(state = state, lastSeenAt = seenAt)
            updateFlows()
        }
    }

    private val readingPositions = mutableMapOf<Long, ReadingPositionEntity>()

    override suspend fun upsertReadingPosition(position: ReadingPositionEntity) {
        readingPositions[position.bookId] = position
    }

    override suspend fun getReadingPosition(bookId: Long): ReadingPositionEntity? = readingPositions[bookId]

    override suspend fun getBookByContentHash(contentHash: String): BookEntity? =
        books.values.firstOrNull { it.contentHash == contentHash }

    override suspend fun deleteBookById(id: Long) {
        books.remove(id)
        updateFlows()
    }

    override suspend fun updateBookSourceBookId(sourceId: Long, newBookId: Long) {
        sources[sourceId]?.let {
            sources[sourceId] = it.copy(bookId = newBookId)
            updateFlows()
        }
    }

    override suspend fun getSourcesForLibrarySource(sourceId: Long): List<BookSourceEntity> =
        sources.values.filter { it.sourceId == sourceId }

    override suspend fun getBooksPendingFingerprint(): List<BookEntity> =
        books.values.filter { it.fingerprintState != "READY" }

    override suspend fun getAllBookSources(): List<BookSourceEntity> = sources.values.toList()

    override fun getAllBookSourcesFlow(): Flow<List<BookSourceEntity>> = sourcesFlow

    override fun searchBooks(query: String): Flow<List<BookEntity>> =
        booksFlow.map { list ->
            list.filter { it.title.contains(query, ignoreCase = true) || (it.author?.contains(query, ignoreCase = true) == true) }
        }
}

class FakeLibrarySourceDao : LibrarySourceDao {
    private val sources = mutableMapOf<Long, LibrarySourceEntity>()
    private var nextId = 1L

    override suspend fun insertSource(source: LibrarySourceEntity): Long {
        val id = nextId++
        sources[id] = source.copy(id = id)
        return id
    }

    override fun getAllSources(): Flow<List<LibrarySourceEntity>> =
        MutableStateFlow(sources.values.toList())

    override suspend fun getSourceByTreeUri(treeUri: String): LibrarySourceEntity? =
        sources.values.firstOrNull { it.treeUri == treeUri }

    override suspend fun updateLastScan(id: Long, timestamp: Long) {
        sources[id]?.let {
            sources[id] = it.copy(lastScanAt = timestamp)
        }
    }

    override suspend fun updateSourceState(id: Long, state: String) {
        sources[id]?.let {
            sources[id] = it.copy(state = state)
        }
    }

    override suspend fun getSourceById(id: Long): LibrarySourceEntity? = sources[id]

    override suspend fun getSourcesByState(state: String): List<LibrarySourceEntity> =
        sources.values.filter { it.state == state }
}

class FakeTreeDocumentProvider : TreeDocumentProvider {
    val documents = mutableListOf<ScannedDocument>()
    val fileContents = mutableMapOf<String, ByteArray>()

    override suspend fun queryTreeDocuments(treeUri: String): List<ScannedDocument> = documents

    override suspend fun openInputStream(documentUri: String): InputStream? {
        val bytes = fileContents[documentUri] ?: return null
        return ByteArrayInputStream(bytes)
    }
}

class IngestionEngineTest {

    private lateinit var bookDao: FakeBookDao
    private lateinit var librarySourceDao: FakeLibrarySourceDao
    private lateinit var documentProvider: FakeTreeDocumentProvider
    private lateinit var ingestionEngine: IngestionEngine
    private lateinit var tempCoverDir: File
    private val fixedClock = Clock.fixed(Instant.parse("2026-10-30T10:00:00Z"), ZoneId.of("UTC"))

    @Before
    fun setup() {
        tempCoverDir = Files.createTempDirectory("covers_test").toFile()
        bookDao = FakeBookDao()
        librarySourceDao = FakeLibrarySourceDao()
        documentProvider = FakeTreeDocumentProvider()
        ingestionEngine = IngestionEngine(
            bookDao = bookDao,
            librarySourceDao = librarySourceDao,
            documentProvider = documentProvider,
            clock = fixedClock,
            coverDir = tempCoverDir
        )
    }

    @Test
    fun testStage1QuickDiscoverCreatesProvisionalBooksAndSources() = runTest {
        val sourceId = librarySourceDao.insertSource(
            LibrarySourceEntity(treeUri = "content://tree/books", displayName = "Books", addedAt = 1000L)
        )

        documentProvider.documents.addAll(
            listOf(
                ScannedDocument(
                    documentId = "doc_1",
                    documentUri = "content://tree/books/doc_1",
                    displayName = "The Nature of Thought.epub",
                    mimeType = "application/epub+zip",
                    sizeBytes = 1024L * 500,
                    lastModified = 1500L
                ),
                ScannedDocument(
                    documentId = "doc_2",
                    documentUri = "content://tree/books/doc_2",
                    displayName = "Meditations.pdf",
                    mimeType = "application/pdf",
                    sizeBytes = 1024L * 800,
                    lastModified = 1600L
                )
            )
        )

        val summary = ingestionEngine.scanSource(sourceId)
        assertEquals(2, summary.totalFound)
        assertEquals(2, summary.newBooks)
        assertEquals(0, summary.missingSources)

        val book1 = bookDao.getBookById(1L)
        assertNotNull(book1)
        assertEquals("The Nature of Thought", book1?.title)
        assertEquals("EPUB", book1?.format)
        assertEquals("SCANNING", book1?.fingerprintState)

        val book2 = bookDao.getBookById(2L)
        assertNotNull(book2)
        assertEquals("Meditations", book2?.title)
        assertEquals("PDF", book2?.format)
        assertEquals("SCANNING", book2?.fingerprintState)

        val sources1 = bookDao.getSourcesForBook(1L)
        assertEquals(1, sources1.size)
        assertEquals("PRESENT", sources1[0].state)
    }

    @Test
    fun testStage2FingerprintingComputesHashAndMarksReady() = runTest {
        val sourceId = librarySourceDao.insertSource(
            LibrarySourceEntity(treeUri = "content://tree/books", displayName = "Books", addedAt = 1000L)
        )
        val uri = "content://tree/books/doc_1"
        documentProvider.documents.add(
            ScannedDocument(
                documentId = "doc_1",
                documentUri = uri,
                displayName = "Meditations.pdf",
                mimeType = "application/pdf",
                sizeBytes = 100L,
                lastModified = 1500L
            )
        )
        // PDF content with Title and Page tags
        val samplePdf = "%PDF-1.4\n/Title (Meditations)\n/Author (Marcus Aurelius)\n/Type /Page\n/Type /Page\n%%EOF"
        documentProvider.fileContents[uri] = samplePdf.toByteArray()

        ingestionEngine.scanSource(sourceId)

        val processed = ingestionEngine.fingerprintPendingBooks()
        assertEquals(1, processed)

        val book = bookDao.getBookById(1L)
        assertNotNull(book)
        assertEquals("READY", book?.fingerprintState)
        assertNotNull(book?.contentHash)
        assertEquals("Meditations", book?.title)
        assertEquals("Marcus Aurelius", book?.author)
        assertEquals(2, book?.totalUnits)
    }

    @Test
    fun testContentCentricDeduplicationLinksSourceToExistingBook() = runTest {
        val source1Id = librarySourceDao.insertSource(
            LibrarySourceEntity(treeUri = "content://tree/folder1", displayName = "Folder 1", addedAt = 1000L)
        )
        val source2Id = librarySourceDao.insertSource(
            LibrarySourceEntity(treeUri = "content://tree/folder2", displayName = "Folder 2", addedAt = 1000L)
        )

        val identicalBytes = "Same exact book content across two folders".toByteArray()

        // 1. Scan folder 1
        documentProvider.documents.clear()
        documentProvider.documents.add(
            ScannedDocument("doc_a", "content://tree/folder1/doc_a", "Book.pdf", "application/pdf", identicalBytes.size.toLong(), 2000L)
        )
        documentProvider.fileContents["content://tree/folder1/doc_a"] = identicalBytes
        ingestionEngine.scanSource(source1Id)
        ingestionEngine.fingerprintPendingBooks()

        val book1 = bookDao.getBookById(1L)
        assertNotNull(book1)
        assertEquals("READY", book1?.fingerprintState)
        val hash = book1?.contentHash
        assertNotNull(hash)

        // 2. Scan folder 2 with identical content
        documentProvider.documents.clear()
        documentProvider.documents.add(
            ScannedDocument("doc_b", "content://tree/folder2/doc_b", "BookCopy.pdf", "application/pdf", identicalBytes.size.toLong(), 2500L)
        )
        documentProvider.fileContents["content://tree/folder2/doc_b"] = identicalBytes
        ingestionEngine.scanSource(source2Id)

        // Two books exist before fingerprinting
        assertEquals(2, bookDao.getBooksPendingFingerprint().size + 1)

        // 3. Stage 2 fingerprinting runs
        ingestionEngine.fingerprintPendingBooks()

        // Provisional book is cleaned up, only 1 BookEntity exists!
        val allBooks = bookDao.getAllBookSources()
        assertEquals(2, allBooks.size) // 2 sources
        assertEquals(1L, allBooks[0].bookId)
        assertEquals(1L, allBooks[1].bookId) // Second source linked to Book 1!

        // Provisional book 2 was deleted
        assertNull(bookDao.getBookById(2L))
    }

    @Test
    fun testMissingFileHandlerMarksMissingAndNeverDeletesBookOrLedger() = runTest {
        val sourceId = librarySourceDao.insertSource(
            LibrarySourceEntity(treeUri = "content://tree/books", displayName = "Books", addedAt = 1000L)
        )
        val docUri = "content://tree/books/doc_1"
        documentProvider.documents.add(
            ScannedDocument("doc_1", docUri, "Nature.epub", "application/epub+zip", 5000L, 1000L)
        )
        ingestionEngine.scanSource(sourceId)

        val sourceBefore = bookDao.getBookSourceByDocumentUri(docUri)
        assertEquals("PRESENT", sourceBefore?.state)

        // Rescan with file removed from storage
        documentProvider.documents.clear()
        val summary2 = ingestionEngine.scanSource(sourceId)
        assertEquals(1, summary2.missingSources)

        val sourceAfter = bookDao.getBookSourceByDocumentUri(docUri)
        assertEquals("MISSING", sourceAfter?.state)

        // BookEntity is NEVER deleted (SC-18)
        val book = bookDao.getBookById(1L)
        assertNotNull(book)
        assertEquals("Nature", book?.title)
    }

    @Test
    fun testStage2EpubCoverExtractionSavesCoverAndSetsCoverPath() = runTest {
        val sourceId = librarySourceDao.insertSource(
            LibrarySourceEntity(treeUri = "content://tree/books", displayName = "Books", addedAt = 1000L)
        )
        val docUri = "content://tree/books/with_cover.epub"
        val expectedCoverBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) // PNG magic
        val epubBytes = createTestEpubBytes(hasCover = true, coverBytes = expectedCoverBytes)

        documentProvider.documents.add(
            ScannedDocument("doc_cov", docUri, "BookWithCover.epub", "application/epub+zip", epubBytes.size.toLong(), 2000L)
        )
        documentProvider.fileContents[docUri] = epubBytes

        ingestionEngine.scanSource(sourceId)
        ingestionEngine.fingerprintPendingBooks()

        val book = bookDao.getBookById(1L)
        assertNotNull(book)
        assertEquals("READY", book?.fingerprintState)
        assertEquals("Epub With Cover", book?.title)
        assertEquals("Test Author", book?.author)
        assertNotNull("coverPath must be populated when cover is extracted", book?.coverPath)

        val coverFile = File(book!!.coverPath!!)
        assertTrue("Cover file must exist on disk", coverFile.exists())
        assertEquals(expectedCoverBytes.size.toLong(), coverFile.length())
    }

    @Test
    fun testStage2FormatWithoutCoverYieldsNullCoverPath() = runTest {
        val sourceId = librarySourceDao.insertSource(
            LibrarySourceEntity(treeUri = "content://tree/books", displayName = "Books", addedAt = 1000L)
        )
        val docUri = "content://tree/books/no_cover.epub"
        val epubBytes = createTestEpubBytes(hasCover = false)

        documentProvider.documents.add(
            ScannedDocument("doc_nocov", docUri, "BookWithoutCover.epub", "application/epub+zip", epubBytes.size.toLong(), 2000L)
        )
        documentProvider.fileContents[docUri] = epubBytes

        ingestionEngine.scanSource(sourceId)
        ingestionEngine.fingerprintPendingBooks()

        val book = bookDao.getBookById(1L)
        assertNotNull(book)
        assertEquals("READY", book?.fingerprintState)
        assertNull("coverPath must be null when format cannot yield a cover", book?.coverPath)
    }

    private fun createTestEpubBytes(hasCover: Boolean, coverBytes: ByteArray = byteArrayOf(1, 2, 3)): ByteArray {
        val baos = ByteArrayOutputStream()
        val zos = ZipOutputStream(baos)

        zos.putNextEntry(ZipEntry("mimetype"))
        zos.write("application/epub+zip".toByteArray())
        zos.closeEntry()

        zos.putNextEntry(ZipEntry("META-INF/container.xml"))
        zos.write("""
            <?xml version="1.0"?>
            <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
                <rootfiles>
                    <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
                </rootfiles>
            </container>
        """.trimIndent().toByteArray())
        zos.closeEntry()

        zos.putNextEntry(ZipEntry("OEBPS/content.opf"))
        val opf = if (hasCover) {
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <package xmlns="http://www.idpf.org/2007/opf" version="3.0">
                <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
                    <dc:title>Epub With Cover</dc:title>
                    <dc:creator>Test Author</dc:creator>
                </metadata>
                <manifest>
                    <item id="cover" href="images/cover.png" media-type="image/png" properties="cover-image"/>
                </manifest>
                <spine>
                    <itemref idref="cover"/>
                </spine>
            </package>
            """.trimIndent()
        } else {
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <package xmlns="http://www.idpf.org/2007/opf" version="3.0">
                <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
                    <dc:title>Epub Without Cover</dc:title>
                    <dc:creator>Test Author</dc:creator>
                </metadata>
                <manifest>
                </manifest>
                <spine>
                </spine>
            </package>
            """.trimIndent()
        }
        zos.write(opf.toByteArray())
        zos.closeEntry()

        if (hasCover) {
            zos.putNextEntry(ZipEntry("OEBPS/images/cover.png"))
            zos.write(coverBytes)
            zos.closeEntry()
        }

        zos.close()
        return baos.toByteArray()
    }
}
