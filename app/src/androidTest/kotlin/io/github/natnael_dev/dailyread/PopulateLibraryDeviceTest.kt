package io.github.natnael_dev.dailyread

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.natnael_dev.dailyread.data.db.DailyReadDatabase
import io.github.natnael_dev.dailyread.data.db.entity.BookEntity
import io.github.natnael_dev.dailyread.data.db.entity.BookSourceEntity
import io.github.natnael_dev.dailyread.data.db.entity.LibrarySourceEntity
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class PopulateLibraryDeviceTest {

    private fun createSyntheticCover(
        dir: File,
        filename: String,
        bgColor: Int,
        title: String,
        author: String
    ): String {
        dir.mkdirs()
        val file = File(dir, filename)
        val width = 400
        val height = 600
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background fill
        val bgPaint = Paint().apply {
            color = bgColor
            isAntiAlias = true
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Outer border
        val borderPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#FCF9EF")
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        canvas.drawRect(20f, 20f, (width - 20).toFloat(), (height - 20).toFloat(), borderPaint)

        // Inner subtle frame
        val innerBorderPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#FCF9EF")
            alpha = 90
            style = Paint.Style.STROKE
            strokeWidth = 1f
            isAntiAlias = true
        }
        canvas.drawRect(26f, 26f, (width - 26).toFloat(), (height - 26).toFloat(), innerBorderPaint)

        // Title text
        val titlePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#FCF9EF")
            textSize = 26f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(title, (width / 2).toFloat(), 250f, titlePaint)

        // Decorative divider line
        val dividerPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#FCF9EF")
            alpha = 180
            strokeWidth = 2f
            isAntiAlias = true
        }
        canvas.drawLine(140f, 280f, 260f, 280f, dividerPaint)

        // Author text
        val authorPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#EDE9DE")
            textSize = 18f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(author, (width / 2).toFloat(), 320f, authorPaint)

        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
        }
        return file.absolutePath
    }

    @Test
    fun populateLibraryForScreenshot(): Unit = runBlocking {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val db = DailyReadDatabase.getInstance(context)
        val bookDao = db.bookDao()
        val sourceDao = db.librarySourceDao()
        val coversDir = File(context.filesDir, "covers")

        val waldenCover = createSyntheticCover(
            coversDir,
            "walden_cover.png",
            android.graphics.Color.parseColor("#4D6148"), // Sage Green
            "WALDEN",
            "Henry David Thoreau"
        )

        val botanicalCover = createSyntheticCover(
            coversDir,
            "botanical_drawing_cover.png",
            android.graphics.Color.parseColor("#94452C"), // Terracotta
            "BOTANICAL DRAWING",
            "Penny Brown"
        )

        val meditationsCover = createSyntheticCover(
            coversDir,
            "meditations_cover.png",
            android.graphics.Color.parseColor("#655E4E"), // Paper Umber
            "MEDITATIONS",
            "Marcus Aurelius"
        )

        val springCover = createSyntheticCover(
            coversDir,
            "silent_spring_cover.png",
            android.graphics.Color.parseColor("#394C34"), // Deep Forest Sage
            "SILENT SPRING",
            "Rachel Carson"
        )

        val philosophyCover = createSyntheticCover(
            coversDir,
            "ethiopian_philosophy_cover.png",
            android.graphics.Color.parseColor("#667A5F"), // Muted Olive
            "ETHIOPIAN PHILOSOPHY",
            "Claude Sumner"
        )

        var src = sourceDao.getSourceByTreeUri("content://com.android.externalstorage.documents/tree/primary%3ABooks")
        val srcId = src?.id ?: sourceDao.insertSource(
            LibrarySourceEntity(
                treeUri = "content://com.android.externalstorage.documents/tree/primary%3ABooks",
                displayName = "Books",
                addedAt = System.currentTimeMillis()
            )
        )

        val books = listOf(
            BookEntity(
                contentKey = "hash_1",
                contentHash = "hash_1",
                format = "EPUB",
                title = "The Nature of Thought",
                author = "Brand Blanshard",
                sizeBytes = 5_242_880L,
                totalUnits = 395,
                coverPath = null, // Intentionally null to test honest placeholder fallback
                lastOpenedAt = 1000L,
                addedAt = 1000L,
                fingerprintState = "READY"
            ),
            BookEntity(
                contentKey = "hash_2",
                contentHash = "hash_2",
                format = "PDF",
                title = "Meditations",
                author = "Marcus Aurelius",
                sizeBytes = 8_388_608L,
                totalUnits = 260,
                coverPath = meditationsCover,
                lastOpenedAt = 2000L,
                addedAt = 2000L,
                fingerprintState = "READY"
            ),
            BookEntity(
                contentKey = "hash_3",
                contentHash = "hash_3",
                format = "EPUB",
                title = "Ethiopian Philosophy",
                author = "Claude Sumner",
                sizeBytes = 12_582_912L,
                totalUnits = 320,
                coverPath = philosophyCover,
                lastOpenedAt = 3000L,
                addedAt = 3000L,
                fingerprintState = "READY"
            ),
            BookEntity(
                contentKey = "hash_4",
                contentHash = "hash_4",
                format = "EPUB",
                title = "Silent Spring",
                author = "Rachel Carson",
                sizeBytes = 4_194_304L,
                totalUnits = 368,
                coverPath = springCover,
                addedAt = 4000L,
                fingerprintState = "READY"
            ),
            BookEntity(
                contentKey = "hash_5",
                contentHash = "hash_5",
                format = "PDF",
                title = "Botanical Drawing",
                author = "Penny Brown",
                sizeBytes = 15_728_640L,
                totalUnits = 160,
                coverPath = botanicalCover,
                lastOpenedAt = 5000L,
                addedAt = 5000L,
                fingerprintState = "READY"
            ),
            BookEntity(
                contentKey = "hash_6",
                contentHash = "hash_6",
                format = "EPUB",
                title = "Walden",
                author = "Henry David Thoreau",
                sizeBytes = 3_145_728L,
                totalUnits = 280,
                coverPath = waldenCover,
                finishedAt = 6000L,
                addedAt = 6000L,
                fingerprintState = "READY"
            )
        )

        for ((idx, b) in books.withIndex()) {
            val existing = bookDao.getBookByContentKey(b.contentKey)
            val bookId = if (existing != null) {
                bookDao.updateBook(b.copy(id = existing.id))
                existing.id
            } else {
                bookDao.insertBook(b)
            }

            val docUri = "content://com.android.externalstorage.documents/document/primary%3ABooks%2Fdoc_$idx"
            val existingSource = bookDao.getBookSourceByDocumentUri(docUri)
            if (existingSource == null) {
                bookDao.insertBookSource(
                    BookSourceEntity(
                        bookId = bookId,
                        sourceId = srcId,
                        documentUri = docUri,
                        documentId = "doc_$idx",
                        displayName = "${b.title}.${b.format.lowercase()}",
                        sizeBytes = b.sizeBytes,
                        lastModified = System.currentTimeMillis(),
                        lastSeenAt = System.currentTimeMillis(),
                        state = "PRESENT"
                    )
                )
            }
        }
    }
}
