package io.github.natnael_dev.dailyread

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.natnael_dev.dailyread.data.db.DailyReadDatabase
import io.github.natnael_dev.dailyread.data.ingest.AndroidPdfCoverExtractor
import io.github.natnael_dev.dailyread.data.ingest.AndroidTreeDocumentProvider
import io.github.natnael_dev.dailyread.data.ingest.IngestionEngine
import io.github.natnael_dev.dailyread.data.reader.PdfReadingCache
import io.github.natnael_dev.dailyread.ui.library.BottomNavBar
import io.github.natnael_dev.dailyread.ui.library.LibraryScreen
import io.github.natnael_dev.dailyread.ui.library.LibraryViewModel
import io.github.natnael_dev.dailyread.ui.plans.PlansScreen
import io.github.natnael_dev.dailyread.ui.plans.PlansViewModel
import io.github.natnael_dev.dailyread.ui.reader.ReaderScreen
import io.github.natnael_dev.dailyread.ui.reader.ReaderViewModel
import io.github.natnael_dev.dailyread.ui.theme.DailyReadTheme
import io.github.natnael_dev.dailyread.ui.today.TodayScreen
import io.github.natnael_dev.dailyread.ui.today.TodayViewModel
import java.io.File

class MainActivity : FragmentActivity() {

    private lateinit var database: DailyReadDatabase
    private lateinit var pdfReadingCache: PdfReadingCache
    private lateinit var todayViewModel: TodayViewModel
    private lateinit var libraryViewModel: LibraryViewModel
    private lateinit var plansViewModel: PlansViewModel

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

        todayViewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TodayViewModel(
                        bookDao = database.bookDao(),
                        readingLedgerDao = database.readingLedgerDao(),
                        planDao = database.planDao()
                    ) as T
                }
            }
        )[TodayViewModel::class.java]

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

        plansViewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PlansViewModel(
                        planDao = database.planDao(),
                        bookDao = database.bookDao()
                    ) as T
                }
            }
        )[PlansViewModel::class.java]

        setContent {
            DailyReadTheme {
                var currentTab by remember { mutableStateOf("today") }
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
                        onBack = {
                            activeReadingBookId = null
                            todayViewModel.refresh()
                            plansViewModel.refresh()
                        }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (currentTab) {
                            "today" -> TodayScreen(
                                viewModel = todayViewModel,
                                onContinueReading = { bookId -> activeReadingBookId = bookId },
                                onNavigateToPlans = { currentTab = "plans" },
                                onNavigateToLibrary = { currentTab = "library" }
                            )
                            "library" -> LibraryScreen(
                                viewModel = libraryViewModel,
                                onBookClick = { bookId -> activeReadingBookId = bookId },
                                onTabSelect = { currentTab = it }
                            )
                            "plans" -> PlansScreen(
                                viewModel = plansViewModel,
                                onBookClick = { bookId -> activeReadingBookId = bookId }
                            )
                        }

                        // Floating bottom navigation overlay
                        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                            BottomNavBar(
                                activeTab = currentTab,
                                onTabSelect = { currentTab = it }
                            )
                        }
                    }
                }
            }
        }
    }
}
