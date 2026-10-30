package io.github.natnael_dev.dailyread

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.natnael_dev.dailyread.data.db.DailyReadDatabase
import io.github.natnael_dev.dailyread.data.ingest.AndroidPdfCoverExtractor
import io.github.natnael_dev.dailyread.data.ingest.AndroidTreeDocumentProvider
import io.github.natnael_dev.dailyread.data.ingest.IngestionEngine
import io.github.natnael_dev.dailyread.data.reader.PdfReadingCache
import io.github.natnael_dev.dailyread.ui.library.LibraryScreen
import io.github.natnael_dev.dailyread.ui.library.LibraryViewModel
import io.github.natnael_dev.dailyread.ui.reader.ReaderScreen
import io.github.natnael_dev.dailyread.ui.reader.ReaderViewModel
import io.github.natnael_dev.dailyread.ui.theme.DailyReadTheme
import java.io.File

class MainActivity : FragmentActivity() {

    private lateinit var libraryViewModel: LibraryViewModel
    private lateinit var database: DailyReadDatabase
    private lateinit var pdfReadingCache: PdfReadingCache

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = DailyReadDatabase.getInstance(this)
        pdfReadingCache = PdfReadingCache(cacheDir = cacheDir)
        val documentProvider = AndroidTreeDocumentProvider(contentResolver)
        val coversDirectory = File(filesDir, "covers")
        val ingestionEngine = IngestionEngine(
            bookDao = database.bookDao(),
            librarySourceDao = database.librarySourceDao(),
            documentProvider = documentProvider,
            coverDir = coversDirectory,
            pdfCoverExtractor = { uriString ->
                AndroidPdfCoverExtractor.renderFirstPage(contentResolver, uriString)
            }
        )

        libraryViewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return LibraryViewModel(
                        bookDao = database.bookDao(),
                        librarySourceDao = database.librarySourceDao(),
                        ingestionEngine = ingestionEngine
                    ) as T
                }
            }
        )[LibraryViewModel::class.java]

        setContent {
            DailyReadTheme {
                var activeReadingBookId by remember { mutableStateOf<Long?>(null) }

                if (activeReadingBookId != null) {
                    val currentBookId = activeReadingBookId!!
                    val readerViewModel = remember(currentBookId) {
                        ReaderViewModel(
                            bookId = currentBookId,
                            bookDao = database.bookDao(),
                            bookmarkDao = database.bookmarkDao(),
                            readingLedgerDao = database.readingLedgerDao()
                        )
                    }

                    ReaderScreen(
                        bookId = currentBookId,
                        viewModel = readerViewModel,
                        bookDao = database.bookDao(),
                        pdfReadingCache = pdfReadingCache,
                        onBack = { activeReadingBookId = null }
                    )
                } else {
                    LibraryScreen(
                        viewModel = libraryViewModel,
                        onBookClick = { bookId ->
                            activeReadingBookId = bookId
                        }
                    )
                }
            }
        }
    }
}
