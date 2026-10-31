package io.github.natnael_dev.dailyread

import android.content.Intent
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
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.natnael_dev.dailyread.data.db.DailyReadDatabase
import io.github.natnael_dev.dailyread.data.ingest.AndroidPdfCoverExtractor
import io.github.natnael_dev.dailyread.data.ingest.AndroidTreeDocumentProvider
import io.github.natnael_dev.dailyread.data.ingest.IngestionEngine
import io.github.natnael_dev.dailyread.data.reader.PdfReadingCache
import io.github.natnael_dev.dailyread.notifications.ReminderScheduler
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
import io.github.natnael_dev.dailyread.widget.DailyReadGlanceWidget
import io.github.natnael_dev.dailyread.widget.WidgetRolloverScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : FragmentActivity() {

    private lateinit var database: DailyReadDatabase
    private lateinit var pdfReadingCache: PdfReadingCache
    private lateinit var todayViewModel: TodayViewModel
    private lateinit var libraryViewModel: LibraryViewModel
    private lateinit var plansViewModel: PlansViewModel

    private var activeBookIdState = mutableStateOf<Long?>(null)

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

        // Handle initial deep link
        handleDeepLinkIntent(intent)

        // Schedule background reminders and widget rollover (ADR-007)
        ReminderScheduler.scheduleReminders(this)
        WidgetRolloverScheduler.scheduleNextRollover(this)

        setContent {
            DailyReadTheme {
                var currentTab by remember { mutableStateOf("today") }
                val activeReadingBookId = activeBookIdState.value

                if (activeReadingBookId != null) {
                    val currentBookId = activeReadingBookId
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
                            activeBookIdState.value = null
                            todayViewModel.refresh()
                            plansViewModel.refresh()
                            refreshWidget()
                        }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (currentTab) {
                            "today" -> TodayScreen(
                                viewModel = todayViewModel,
                                onContinueReading = { bookId -> activeBookIdState.value = bookId },
                                onNavigateToPlans = { currentTab = "plans" },
                                onNavigateToLibrary = { currentTab = "library" }
                            )
                            "library" -> LibraryScreen(
                                viewModel = libraryViewModel,
                                onBookClick = { bookId -> activeBookIdState.value = bookId },
                                onTabSelect = { currentTab = it }
                            )
                            "plans" -> PlansScreen(
                                viewModel = plansViewModel,
                                onBookClick = { bookId -> activeBookIdState.value = bookId }
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLinkIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        refreshWidget()
    }

    private fun handleDeepLinkIntent(intent: Intent?) {
        if (intent == null) return
        val bookId = extractBookId(intent)
        if (bookId != null && bookId > 0) {
            activeBookIdState.value = bookId
        }
    }

    private fun extractBookId(intent: Intent): Long? {
        val extraId = intent.getLongExtra("bookId", -1L)
        if (extraId > 0) return extraId
        val extraStr = intent.getStringExtra("bookId")?.toLongOrNull()
        if (extraStr != null && extraStr > 0) return extraStr
        val dataUri = intent.data
        if (dataUri != null && dataUri.scheme == "dailyread" && dataUri.host == "book") {
            return dataUri.lastPathSegment?.toLongOrNull()
        }
        return null
    }

    private fun refreshWidget() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                DailyReadGlanceWidget().updateAll(this@MainActivity)
            } catch (e: Exception) {
                // Widget might not be placed on home screen yet
            }
        }
    }
}
