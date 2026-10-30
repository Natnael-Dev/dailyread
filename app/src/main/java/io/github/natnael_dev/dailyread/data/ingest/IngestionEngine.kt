package io.github.natnael_dev.dailyread.data.ingest

import io.github.natnael_dev.dailyread.data.db.dao.BookDao
import io.github.natnael_dev.dailyread.data.db.dao.LibrarySourceDao
import io.github.natnael_dev.dailyread.data.db.entity.BookEntity
import io.github.natnael_dev.dailyread.data.db.entity.BookSourceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Clock

data class ScanSummary(
    val totalFound: Int,
    val newBooks: Int,
    val updatedSources: Int,
    val missingSources: Int
)

data class FingerprintProgress(
    val completed: Int,
    val total: Int,
    val currentBookTitle: String? = null
)

class IngestionEngine(
    private val bookDao: BookDao,
    private val librarySourceDao: LibrarySourceDao,
    private val documentProvider: TreeDocumentProvider,
    private val clock: Clock = Clock.systemUTC()
) {

    /**
     * Stage 1: Quick Discovery (Foreground, fast, DEC-036).
     * Finds files in the treeUri, inserts provisional BookEntity (status = "SCANNING"),
     * links BookSourceEntity (state = "PRESENT"), and detects MISSING files (SC-18).
     */
    suspend fun scanSource(sourceId: Long): ScanSummary = withContext(Dispatchers.IO) {
        val source = librarySourceDao.getSourceById(sourceId)
            ?: return@withContext ScanSummary(0, 0, 0, 0)

        val scanStartTime = clock.millis()
        val documents = documentProvider.queryTreeDocuments(source.treeUri)

        var newBooksCount = 0
        var updatedSourcesCount = 0

        // Filter supported formats: EPUB and PDF
        val supportedDocs = documents.filter { doc ->
            doc.displayName.endsWith(".epub", ignoreCase = true) ||
            doc.displayName.endsWith(".pdf", ignoreCase = true) ||
            doc.mimeType == "application/epub+zip" ||
            doc.mimeType == "application/pdf"
        }
        val discoveredUris = supportedDocs.map { it.documentUri }.toSet()

        for (doc in supportedDocs) {
            val existingSource = bookDao.getBookSourceByDocumentUri(doc.documentUri)
            if (existingSource != null) {
                // Source exists: update last_seen_at and ensure state is PRESENT
                bookDao.updateBookSourceState(
                    sourceId = existingSource.id,
                    state = "PRESENT",
                    seenAt = scanStartTime
                )
                updatedSourcesCount++
            } else {
                // New document discovered
                val format = if (doc.displayName.endsWith(".pdf", ignoreCase = true) || doc.mimeType == "application/pdf") {
                    "PDF"
                } else {
                    "EPUB"
                }

                val cleanTitle = doc.displayName
                    .removeSuffix(".epub")
                    .removeSuffix(".EPUB")
                    .removeSuffix(".pdf")
                    .removeSuffix(".PDF")

                // Provisional content key for Stage 1
                val provisionalKey = "doc:${doc.documentId}_${doc.sizeBytes}_${doc.lastModified}"

                // Insert BookEntity with status = "SCANNING"
                val book = BookEntity(
                    contentKey = provisionalKey,
                    contentHash = null,
                    format = format,
                    title = cleanTitle,
                    author = null,
                    sizeBytes = doc.sizeBytes,
                    totalUnits = null,
                    addedAt = scanStartTime,
                    fingerprintState = "SCANNING"
                )

                val bookId = bookDao.insertBook(book)

                // Insert BookSourceEntity
                val sourceEntity = BookSourceEntity(
                    bookId = bookId,
                    sourceId = sourceId,
                    documentUri = doc.documentUri,
                    documentId = doc.documentId,
                    displayName = doc.displayName,
                    sizeBytes = doc.sizeBytes,
                    lastModified = doc.lastModified,
                    lastSeenAt = scanStartTime,
                    state = "PRESENT"
                )
                bookDao.insertBookSource(sourceEntity)
                newBooksCount++
            }
        }

        // Missing-file handler (SC-18):
        // Mark any source previously seen in this tree that wasn't in the scan as MISSING.
        // NEVER delete BookEntity or reading ledger entries!
        val allSourcesForTree = bookDao.getSourcesForLibrarySource(sourceId)
        var missingCount = 0
        for (src in allSourcesForTree) {
            if (src.documentUri !in discoveredUris && src.state != "MISSING") {
                bookDao.updateBookSourceState(src.id, "MISSING", src.lastSeenAt)
                missingCount++
            }
        }

        // Update last scan timestamp
        librarySourceDao.updateLastScan(sourceId, scanStartTime)

        ScanSummary(
            totalFound = supportedDocs.size,
            newBooks = newBooksCount,
            updatedSources = updatedSourcesCount,
            missingSources = missingCount
        )
    }

    /**
     * Stage 2: Background Async Fingerprint & Dedup (DEC-030/031, DEC-036).
     * Computes content hashes, extracts metadata, dedupes against existing books.
     */
    suspend fun fingerprintPendingBooks(
        onProgress: (FingerprintProgress) -> Unit = {}
    ): Int = withContext(Dispatchers.IO) {
        val pendingBooks = bookDao.getBooksPendingFingerprint()
        val total = pendingBooks.size
        var completed = 0

        for (book in pendingBooks) {
            onProgress(FingerprintProgress(completed = completed, total = total, currentBookTitle = book.title))

            val sources = bookDao.getSourcesForBook(book.id)
            val presentSource = sources.firstOrNull { it.state == "PRESENT" } ?: sources.firstOrNull()

            if (presentSource == null) {
                completed++
                continue
            }

            val inputStream = documentProvider.openInputStream(presentSource.documentUri)
            if (inputStream == null) {
                bookDao.updateBook(book.copy(fingerprintState = "FAILED"))
                completed++
                continue
            }

            // 1. Compute full content hash (Tier 2 SHA-256)
            val contentHash = inputStream.use { ContentHasher.computeFullHash(it) }

            // 2. Check for Content-Centric Deduplication (DEC-030/031):
            // If another book already has this content hash, link source and delete provisional book.
            val existingBook = bookDao.getBookByContentHash(contentHash)
            if (existingBook != null && existingBook.id != book.id) {
                // Link this source to the existing book
                bookDao.updateBookSourceBookId(presentSource.id, existingBook.id)
                // Remove the provisional book entity (has no ledger/reading position)
                bookDao.deleteBookById(book.id)
            } else {
                // Read metadata using a new stream
                val metadataStream = documentProvider.openInputStream(presentSource.documentUri)
                val metadata = metadataStream?.use {
                    BookMetadataExtractor.extractMetadata(book.format, it)
                } ?: ExtractedMetadata(null, null, null)

                val updatedBook = book.copy(
                    contentKey = "hash:$contentHash",
                    contentHash = contentHash,
                    title = metadata.title?.takeIf { it.isNotBlank() } ?: book.title,
                    author = metadata.author?.takeIf { it.isNotBlank() } ?: book.author,
                    totalUnits = metadata.totalUnits ?: book.totalUnits,
                    fingerprintState = "READY"
                )
                bookDao.updateBook(updatedBook)
            }

            completed++
            onProgress(FingerprintProgress(completed = completed, total = total, currentBookTitle = book.title))
        }

        completed
    }
}
