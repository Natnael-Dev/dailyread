package io.github.natnael_dev.dailyread

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.natnael_dev.dailyread.data.db.DailyReadDatabase
import io.github.natnael_dev.dailyread.data.db.entity.BookEntity
import io.github.natnael_dev.dailyread.data.db.entity.BookSourceEntity
import io.github.natnael_dev.dailyread.data.db.entity.LibrarySourceEntity
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PopulateLibraryDeviceTest {

    @Test
    fun populateLibraryForScreenshot() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = DailyReadDatabase.getInstance(context)
        val bookDao = db.bookDao()
        val sourceDao = db.librarySourceDao()

        val srcId = sourceDao.insertSource(
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
                finishedAt = 6000L,
                addedAt = 6000L,
                fingerprintState = "READY"
            )
        )

        for ((idx, b) in books.withIndex()) {
            val bookId = bookDao.insertBook(b)
            bookDao.insertBookSource(
                BookSourceEntity(
                    bookId = bookId,
                    sourceId = srcId,
                    documentUri = "content://com.android.externalstorage.documents/document/primary%3ABooks%2Fdoc_$idx",
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
