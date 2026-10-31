package io.github.natnael_dev.dailyread.data.reader

import android.content.ContentResolver
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Thrown when internal cache storage has insufficient space to cache a PDF for reading.
 */
class InsufficientStorageException(
    message: String = "Insufficient storage space to cache PDF for reading"
) : java.io.IOException(message)

/**
 * Approved DEC-021 Fallback: Bounded MRU Reading Cache for PDF Documents.
 *
 * Background & Rationale (DEC-021 / Risk R-01):
 * In Readium 3.1.1, [org.readium.adapter.pdfium.document.PdfiumDocumentFactory] attempts to open
 * publications via `openAsFile(file)`. For Android SAF `content://` URIs, `sourceUrl.toFile()`
 * returns null because SAF URIs do not map to direct filesystem paths.
 * Consequently, Readium falls back to `openBytes()`, which buffers the ENTIRE PDF file into a Java
 * heap byte array, presenting a critical out-of-memory (OOM) risk on memory-constrained devices
 * (such as the target Samsung Galaxy M12 with 3GB RAM).
 *
 * Solution:
 * This cache maintains a temporary, bounded, LRU-evicted reading copy in internal storage
 * (`context.cacheDir/pdf_mru/`).
 *
 * Invariants & Constraints:
 * 1. Bounded: Capped at [maxCacheSizeBytes] (default 50 MB) and [maxCacheEntries] (default 5).
 * 2. LRU Eviction: Least-recently-accessed files are evicted when the cache budget is exceeded.
 * 3. Non-Destructive: Original user files on SAF storage are NEVER moved, modified, or written to.
 * 4. Backup Excluded: Stored strictly within `context.cacheDir`, which Android excludes from OS backups,
 *    and app-wide `android:allowBackup="false"` is enforced.
 * 5. Privacy & Security: Raw file paths and personal document titles are never logged in release builds.
 */
class PdfReadingCache(
    private val cacheDir: File,
    val maxCacheSizeBytes: Long = DEFAULT_MAX_CACHE_SIZE_BYTES,
    val maxCacheEntries: Int = DEFAULT_MAX_ENTRIES,
    val minRequiredFreeBytes: Long = DEFAULT_MIN_FREE_BYTES
) {
    companion object {
        const val CACHE_SUBDIR = "pdf_mru"
        const val DEFAULT_MAX_CACHE_SIZE_BYTES = 50 * 1024 * 1024L // 50 MB budget (DEC-021)
        const val DEFAULT_MAX_ENTRIES = 5
        const val DEFAULT_MIN_FREE_BYTES = 5 * 1024 * 1024L // 5 MB minimum disk headroom
    }

    private val mruDirectory: File = File(cacheDir, CACHE_SUBDIR).apply {
        if (!exists()) {
            mkdirs()
        }
    }

    /**
     * Retrieves or creates a temporary cached file for the specified book content key.
     * Original SAF stream is copied safely in 8KB chunks.
     *
     * @param contentKey Unique identifier or hash for the book
     * @param openStream Supplier providing an open InputStream to the original SAF document
     * @return Cache-backed File suitable for Readium's `openAsFile(file)`
     */
    @Synchronized
    fun getOrCreateCachedFile(
        contentKey: String,
        openStream: () -> InputStream?
    ): File {
        // Sanitize contentKey for safe filename
        val safeKey = contentKey.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val targetFile = File(mruDirectory, "$safeKey.pdf")

        if (targetFile.exists() && targetFile.length() > 0) {
            // Touch lastModified to update LRU access timestamp
            targetFile.setLastModified(System.currentTimeMillis())
            return targetFile
        }

        // Verify storage space availability before allocating temporary copy
        if (minRequiredFreeBytes > 0 && mruDirectory.usableSpace < minRequiredFreeBytes) {
            evictOldEntriesIfNeeded()
            if (mruDirectory.usableSpace < minRequiredFreeBytes) {
                throw InsufficientStorageException(
                    "Device storage critically low: ${mruDirectory.usableSpace} bytes available, requires at least $minRequiredFreeBytes bytes."
                )
            }
        }

        // Temporary download/copy file to ensure atomicity
        val tempFile = File(mruDirectory, "${safeKey}_temp_${System.currentTimeMillis()}.tmp")
        try {
            val stream = openStream() ?: error("Failed to open input stream for PDF content")
            stream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output, bufferSize = 8192)
                }
            }

            if (targetFile.exists()) {
                targetFile.delete()
            }
            if (!tempFile.renameTo(targetFile)) {
                // Fallback copy if renameTo fails across file systems
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }
            targetFile.setLastModified(System.currentTimeMillis())
        } catch (e: Exception) {
            if (tempFile.exists()) {
                tempFile.delete()
            }
            if (e is InsufficientStorageException) {
                throw e
            }
            if (e is java.io.IOException && (e.message?.contains("ENOSPC", ignoreCase = true) == true ||
                e.message?.contains("space", ignoreCase = true) == true)) {
                throw InsufficientStorageException("Failed to cache PDF due to low storage: ${e.message}")
            }
            throw e
        }

        // Enforce cache bounds and LRU eviction (protecting targetFile)
        evictOldEntriesIfNeeded(protectedFile = targetFile)

        return targetFile
    }

    /**
     * Enforces size cap and entry count budget by deleting oldest files.
     */
    @Synchronized
    fun evictOldEntriesIfNeeded(protectedFile: File? = null) {
        val files = mruDirectory.listFiles { file ->
            file.isFile && file.name.endsWith(".pdf") && file != protectedFile
        } ?: return

        // Sort by lastModified ascending (oldest first)
        val sortedFiles = files.sortedBy { it.lastModified() }

        var currentTotalSize = (mruDirectory.listFiles() ?: emptyArray()).sumOf { it.length() }
        var currentFileCount = files.size + (if (protectedFile != null && protectedFile.exists()) 1 else 0)

        for (file in sortedFiles) {
            if (currentTotalSize <= maxCacheSizeBytes && currentFileCount <= maxCacheEntries) {
                break
            }
            val fileSize = file.length()
            if (file.delete()) {
                currentTotalSize -= fileSize
                currentFileCount -= 1
            }
        }
    }

    /**
     * Clears all temporary MRU cached reading files.
     */
    @Synchronized
    fun clearCache() {
        mruDirectory.listFiles()?.forEach { it.delete() }
    }

    /**
     * Returns the current total byte usage of the MRU reading cache.
     */
    fun getCurrentCacheSizeBytes(): Long {
        return (mruDirectory.listFiles() ?: emptyArray()).sumOf { it.length() }
    }

    /**
     * Returns the count of cached PDF files currently held.
     */
    fun getCachedFileCount(): Int {
        return (mruDirectory.listFiles { file -> file.isFile && file.name.endsWith(".pdf") } ?: emptyArray()).size
    }
}
