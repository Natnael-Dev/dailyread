package io.github.natnael_dev.dailyread

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.natnael_dev.dailyread.data.db.DailyReadDatabase
import io.github.natnael_dev.dailyread.data.ingest.AndroidPdfCoverExtractor
import io.github.natnael_dev.dailyread.data.ingest.AndroidTreeDocumentProvider
import io.github.natnael_dev.dailyread.data.ingest.IngestionEngine
import io.github.natnael_dev.dailyread.ui.library.LibraryScreen
import io.github.natnael_dev.dailyread.ui.library.LibraryViewModel
import io.github.natnael_dev.dailyread.ui.theme.DailyReadTheme
import java.io.File

class MainActivity : ComponentActivity() {

    private lateinit var libraryViewModel: LibraryViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = DailyReadDatabase.getInstance(this)
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
                LibraryScreen(viewModel = libraryViewModel)
            }
        }
    }
}
