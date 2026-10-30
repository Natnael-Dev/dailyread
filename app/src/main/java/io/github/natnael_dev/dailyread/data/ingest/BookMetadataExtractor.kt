package io.github.natnael_dev.dailyread.data.ingest

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.zip.ZipInputStream

data class ExtractedMetadata(
    val title: String?,
    val author: String?,
    val totalUnits: Int?,
    val coverBytes: ByteArray? = null
)

object BookMetadataExtractor {

    suspend fun extractMetadata(format: String, inputStream: InputStream): ExtractedMetadata = withContext(Dispatchers.IO) {
        when (format.uppercase()) {
            "EPUB" -> extractEpubMetadata(inputStream)
            "PDF" -> extractPdfMetadata(inputStream)
            else -> ExtractedMetadata(title = null, author = null, totalUnits = null)
        }
    }

    private fun extractEpubMetadata(inputStream: InputStream): ExtractedMetadata {
        var title: String? = null
        var author: String? = null
        var pageCount: Int? = null

        try {
            val zip = ZipInputStream(inputStream)
            var entry = zip.nextEntry
            var opfContent: String? = null

            while (entry != null) {
                if (entry.name.endsWith(".opf", ignoreCase = true)) {
                    opfContent = zip.bufferedReader().readText()
                    break
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }

            if (opfContent != null) {
                // Parse dc:title
                val titleRegex = Regex("<dc:title[^>]*>([^<]+)</dc:title>", RegexOption.IGNORE_CASE)
                val titleMatch = titleRegex.find(opfContent)
                if (titleMatch != null) {
                    title = titleMatch.groupValues[1].trim()
                }

                // Parse dc:creator
                val creatorRegex = Regex("<dc:creator[^>]*>([^<]+)</dc:creator>", RegexOption.IGNORE_CASE)
                val creatorMatch = creatorRegex.find(opfContent)
                if (creatorMatch != null) {
                    author = creatorMatch.groupValues[1].trim()
                }

                // Estimate units from spine itemrefs if available
                val itemrefCount = Regex("<itemref[^>]+>", RegexOption.IGNORE_CASE).findAll(opfContent).count()
                if (itemrefCount > 0) {
                    pageCount = itemrefCount
                }
            }
        } catch (e: Exception) {
            // Graceful fallback
        }

        return ExtractedMetadata(
            title = title,
            author = author,
            totalUnits = pageCount
        )
    }

    private fun extractPdfMetadata(inputStream: InputStream): ExtractedMetadata {
        var pageCount: Int? = null
        var title: String? = null
        var author: String? = null

        try {
            val bytes = inputStream.readBytes()
            val text = String(bytes, Charsets.ISO_8859_1)

            val pageMatches = Regex("/Type\\s*/Page[^s]", RegexOption.IGNORE_CASE).findAll(text).count()
            if (pageMatches > 0) {
                pageCount = pageMatches
            }

            val titleMatch = Regex("/Title\\s*\\(([^)]+)\\)").find(text)
            if (titleMatch != null) {
                title = titleMatch.groupValues[1].trim()
            }

            val authorMatch = Regex("/Author\\s*\\(([^)]+)\\)").find(text)
            if (authorMatch != null) {
                author = authorMatch.groupValues[1].trim()
            }
        } catch (e: Exception) {
            // Graceful fallback
        }

        return ExtractedMetadata(
            title = title,
            author = author,
            totalUnits = pageCount
        )
    }
}
