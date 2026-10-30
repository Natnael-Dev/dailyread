package io.github.natnael_dev.dailyread.data.ingest

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

data class ExtractedMetadata(
    val title: String?,
    val author: String?,
    val totalUnits: Int?,
    val coverBytes: ByteArray? = null
)

object BookMetadataExtractor {

    suspend fun extractMetadata(
        format: String,
        inputStream: InputStream,
        pdfCoverProvider: (suspend () -> ByteArray?)? = null
    ): ExtractedMetadata = withContext(Dispatchers.IO) {
        when (format.uppercase()) {
            "EPUB" -> extractEpubMetadata(inputStream)
            "PDF" -> extractPdfMetadata(inputStream, pdfCoverProvider)
            else -> ExtractedMetadata(title = null, author = null, totalUnits = null, coverBytes = null)
        }
    }

    private fun extractEpubMetadata(inputStream: InputStream): ExtractedMetadata {
        var title: String? = null
        var author: String? = null
        var pageCount: Int? = null
        var coverBytes: ByteArray? = null

        try {
            val allBytes = inputStream.readBytes()
            var opfContent: String? = null
            var opfDir = ""

            // Pass 1: Find container.xml or .opf
            ZipInputStream(ByteArrayInputStream(allBytes)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name.endsWith(".opf", ignoreCase = true)) {
                        val lastSlash = entry.name.lastIndexOf('/')
                        opfDir = if (lastSlash >= 0) entry.name.substring(0, lastSlash + 1) else ""
                        opfContent = zip.bufferedReader().readText()
                        break
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            var coverHref: String? = null
            if (opfContent != null) {
                val nonNullOpf = opfContent
                // Parse dc:title
                val titleRegex = Regex("<dc:title[^>]*>([^<]+)</dc:title>", RegexOption.IGNORE_CASE)
                val titleMatch = titleRegex.find(nonNullOpf)
                if (titleMatch != null) {
                    title = titleMatch.groupValues[1].trim()
                }

                // Parse dc:creator
                val creatorRegex = Regex("<dc:creator[^>]*>([^<]+)</dc:creator>", RegexOption.IGNORE_CASE)
                val creatorMatch = creatorRegex.find(nonNullOpf)
                if (creatorMatch != null) {
                    author = creatorMatch.groupValues[1].trim()
                }

                // Estimate units from spine itemrefs if available
                val itemrefCount = Regex("<itemref[^>]+>", RegexOption.IGNORE_CASE).findAll(nonNullOpf).count()
                if (itemrefCount > 0) {
                    pageCount = itemrefCount
                }

                // 1. EPUB 3 cover-image property check
                val epub3CoverRegex = Regex("<item[^>]+properties=\"[^\"]*cover-image[^\"]*\"[^>]+href=\"([^\"]+)\"", RegexOption.IGNORE_CASE)
                val epub3CoverRegex2 = Regex("<item[^>]+href=\"([^\"]+)\"[^>]+properties=\"[^\"]*cover-image[^\"]*\"", RegexOption.IGNORE_CASE)
                val matchE3 = epub3CoverRegex.find(nonNullOpf) ?: epub3CoverRegex2.find(nonNullOpf)
                if (matchE3 != null) {
                    coverHref = matchE3.groupValues[1].trim()
                }

                // 2. EPUB 2 <meta name="cover" content="coverId"/> check
                if (coverHref == null) {
                    val epub2MetaRegex = Regex("<meta[^>]+name=\"cover\"[^>]+content=\"([^\"]+)\"", RegexOption.IGNORE_CASE)
                    val matchE2 = epub2MetaRegex.find(nonNullOpf)
                    if (matchE2 != null) {
                        val coverId = matchE2.groupValues[1].trim()
                        val itemRegex = Regex("<item[^>]+id=\"${Regex.escape(coverId)}\"[^>]+href=\"([^\"]+)\"", RegexOption.IGNORE_CASE)
                        val itemMatch = itemRegex.find(nonNullOpf)
                        if (itemMatch != null) {
                            coverHref = itemMatch.groupValues[1].trim()
                        }
                    }
                }

                // 3. Fallback: item id="cover"
                if (coverHref == null) {
                    val fallbackRegex = Regex("<item[^>]+id=\"cover\"[^>]+href=\"([^\"]+)\"", RegexOption.IGNORE_CASE)
                    val matchFallback = fallbackRegex.find(nonNullOpf)
                    if (matchFallback != null) {
                        coverHref = matchFallback.groupValues[1].trim()
                    }
                }

                // 4. Fallback: item with image and cover in href
                if (coverHref == null) {
                    val imgCoverRegex = Regex("<item[^>]+href=\"([^\"]*cover[^\"]*\\.(?:jpg|jpeg|png|webp))\"", RegexOption.IGNORE_CASE)
                    val matchImg = imgCoverRegex.find(nonNullOpf)
                    if (matchImg != null) {
                        coverHref = matchImg.groupValues[1].trim()
                    }
                }
            }

            // Pass 2: Extract cover image bytes if coverHref found
            if (coverHref != null) {
                val resolvedCoverPath = normalizeZipPath(opfDir + coverHref)
                ZipInputStream(ByteArrayInputStream(allBytes)).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        val normalizedEntryName = normalizeZipPath(entry.name)
                        if (normalizedEntryName.equals(resolvedCoverPath, ignoreCase = true) ||
                            (normalizedEntryName.endsWith(coverHref.substringAfterLast('/'), ignoreCase = true) &&
                             (normalizedEntryName.endsWith(".png", true) || normalizedEntryName.endsWith(".jpg", true) ||
                              normalizedEntryName.endsWith(".jpeg", true) || normalizedEntryName.endsWith(".webp", true)))
                        ) {
                            coverBytes = zip.readBytes()
                            break
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            }
        } catch (e: Exception) {
            // Graceful fallback
        }

        return ExtractedMetadata(
            title = title,
            author = author,
            totalUnits = pageCount,
            coverBytes = coverBytes
        )
    }

    private suspend fun extractPdfMetadata(
        inputStream: InputStream,
        pdfCoverProvider: (suspend () -> ByteArray?)? = null
    ): ExtractedMetadata {
        var pageCount: Int? = null
        var title: String? = null
        var author: String? = null
        var coverBytes: ByteArray? = null

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

            if (pdfCoverProvider != null) {
                coverBytes = pdfCoverProvider.invoke()
            }
        } catch (e: Exception) {
            // Graceful fallback
        }

        return ExtractedMetadata(
            title = title,
            author = author,
            totalUnits = pageCount,
            coverBytes = coverBytes
        )
    }

    private fun normalizeZipPath(path: String): String {
        val parts = path.replace('\\', '/').split('/')
        val stack = mutableListOf<String>()
        for (part in parts) {
            if (part == "." || part.isEmpty()) continue
            if (part == "..") {
                if (stack.isNotEmpty()) stack.removeAt(stack.size - 1)
            } else {
                stack.add(part)
            }
        }
        return stack.joinToString("/")
    }
}
