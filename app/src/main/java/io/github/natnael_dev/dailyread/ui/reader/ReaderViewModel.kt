package io.github.natnael_dev.dailyread.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.natnael_dev.dailyread.data.db.dao.BookDao
import io.github.natnael_dev.dailyread.data.db.dao.BookmarkDao
import io.github.natnael_dev.dailyread.data.db.dao.ReadingLedgerDao
import io.github.natnael_dev.dailyread.data.db.entity.BookEntity
import io.github.natnael_dev.dailyread.data.db.entity.BookmarkEntity
import io.github.natnael_dev.dailyread.data.db.entity.DailyPositionEntity
import io.github.natnael_dev.dailyread.data.db.entity.ReadingIntervalEntity
import io.github.natnael_dev.dailyread.data.db.entity.ReadingPositionEntity
import io.github.natnael_dev.dailyread.domain.time.Clock
import io.github.natnael_dev.dailyread.domain.time.SystemClock
import io.github.natnael_dev.dailyread.domain.tracker.ReadingTracker
import io.github.natnael_dev.dailyread.domain.tracker.ReadingTrackerConstants
import io.github.natnael_dev.dailyread.domain.tracker.TrackedInterval
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ReaderNavigationAction {
    data class JumpToLocator(val locatorJson: String) : ReaderNavigationAction
    object GoForward : ReaderNavigationAction
    object GoBackward : ReaderNavigationAction
}

class ReaderViewModel(
    val bookId: Long,
    private val bookDao: BookDao,
    private val bookmarkDao: BookmarkDao,
    private val readingLedgerDao: ReadingLedgerDao,
    private val clock: Clock = SystemClock(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private val _navigationActions = MutableSharedFlow<ReaderNavigationAction>()
    val navigationActions: SharedFlow<ReaderNavigationAction> = _navigationActions.asSharedFlow()

    private var tracker: ReadingTracker? = null
    private var lastFlushTimestampMs: Long = 0L
    private var lastSavedLocatorJson: String? = null

    init {
        loadBook()
        observeBookmarks()
    }

    private fun loadBook() {
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val book = bookDao.getBookById(bookId)
            if (book == null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Book not found in library.") }
                return@launch
            }

            // Restore exact reading position (DEC-021 / Exact Resume)
            val savedPosition = bookDao.getReadingPosition(bookId)
            val format = if (book.format.equals("PDF", ignoreCase = true)) {
                ReaderFormat.PDF
            } else {
                ReaderFormat.EPUB
            }

            // Initialize domain ReadingTracker with provisional constants
            val readingTracker = ReadingTracker(
                contentKey = book.contentKey,
                titleSnapshot = book.title,
                clock = clock
            )
            tracker = readingTracker

            // Initial position index for tracker
            val initialPosition = savedPosition?.let {
                // Approximate page / position index or start from 1
                (it.totalProgression * (book.totalUnits ?: 100)).toInt().coerceAtLeast(1)
            } ?: 1

            readingTracker.startSession(
                initialPosition = initialPosition,
                elapsedRealtimeMs = System.currentTimeMillis()
            )
            lastFlushTimestampMs = System.currentTimeMillis()
            lastSavedLocatorJson = savedPosition?.locatorJson

            _uiState.update {
                it.copy(
                    book = book,
                    format = format,
                    title = book.title,
                    progression = savedPosition?.totalProgression ?: 0.0,
                    currentLocatorJson = savedPosition?.locatorJson,
                    isLoading = false
                )
            }
        }
    }

    private fun observeBookmarks() {
        viewModelScope.launch(ioDispatcher) {
            bookmarkDao.getBookmarksForBook(bookId).collectLatest { bookmarks ->
                _uiState.update { state ->
                    val isCurrentBookmarked = bookmarks.any { b ->
                        b.locatorJson == state.currentLocatorJson
                    }
                    state.copy(
                        bookmarks = bookmarks,
                        isBookmarked = isCurrentBookmarked
                    )
                }
            }
        }
    }

    /**
     * Called by reader engine when locator changes as user reads.
     */
    fun onLocatorChanged(
        locatorJson: String,
        progression: Double,
        pageNumber: Int?,
        chapterTitle: String? = null
    ) {
        val nowMs = System.currentTimeMillis()
        val currentPosition = pageNumber ?: (progression * 100).toInt().coerceAtLeast(1)

        // Notify ReadingTracker
        tracker?.onPositionChanged(currentPosition, nowMs)

        _uiState.update { state ->
            val isBookmarked = state.bookmarks.any { it.locatorJson == locatorJson }
            state.copy(
                currentLocatorJson = locatorJson,
                progression = progression,
                currentPage = pageNumber ?: state.currentPage,
                chapterTitle = chapterTitle ?: state.chapterTitle,
                isBookmarked = isBookmarked
            )
        }

        // Periodic flush (<=30s flush requirement)
        if (nowMs - lastFlushTimestampMs >= 30_000L || (tracker?.shouldFlush(nowMs) == true)) {
            flushReadingSession(nowMs)
            viewModelScope.launch(ioDispatcher) {
                persistCurrentLocator(locatorJson, progression)
            }
        }
    }

    /**
     * Called on user touch, scroll, swipe or tap.
     */
    fun onUserInteraction() {
        val nowMs = System.currentTimeMillis()
        tracker?.onUserInteraction(nowMs)
    }

    /**
     * Toggles bookmark for current reading position.
     */
    fun toggleBookmark(customTitle: String? = null) {
        val state = _uiState.value
        val locatorJson = state.currentLocatorJson ?: return

        viewModelScope.launch(ioDispatcher) {
            val existing = bookmarkDao.getBookmarkByLocator(bookId, locatorJson)
            if (existing != null) {
                bookmarkDao.deleteBookmark(existing.id)
                _uiState.update { it.copy(statusMessage = "Bookmark removed", isBookmarked = false) }
            } else {
                val title = customTitle
                    ?: if (state.chapterTitle.isNotEmpty()) {
                        "${state.chapterTitle} • Page ${state.currentPage}"
                    } else {
                        "Page ${state.currentPage}"
                    }
                bookmarkDao.insertBookmark(
                    BookmarkEntity(
                        bookId = bookId,
                        locatorJson = locatorJson,
                        title = title,
                        createdAt = System.currentTimeMillis()
                    )
                )
                _uiState.update { it.copy(statusMessage = "Bookmark saved", isBookmarked = true) }
            }
        }
    }

    /**
     * Delete a specific bookmark by ID.
     */
    fun deleteBookmark(bookmarkId: Long) {
        viewModelScope.launch(ioDispatcher) {
            bookmarkDao.deleteBookmark(bookmarkId)
            _uiState.update { it.copy(statusMessage = "Bookmark deleted") }
        }
    }

    /**
     * Jumps reader to the saved bookmark's locator.
     */
    fun jumpToBookmark(bookmark: BookmarkEntity) {
        viewModelScope.launch {
            _navigationActions.emit(ReaderNavigationAction.JumpToLocator(bookmark.locatorJson))
            _uiState.update { it.copy(activeSheet = ReaderSheet.NONE) }
        }
    }

    fun goForward() {
        viewModelScope.launch {
            _navigationActions.emit(ReaderNavigationAction.GoForward)
        }
    }

    fun goBackward() {
        viewModelScope.launch {
            _navigationActions.emit(ReaderNavigationAction.GoBackward)
        }
    }

    fun setTheme(theme: ReaderTheme) {
        _uiState.update { it.copy(theme = theme) }
    }

    fun setFontSize(multiplier: Double) {
        val clamped = multiplier.coerceIn(0.8, 2.2)
        _uiState.update { it.copy(fontSizeMultiplier = clamped) }
    }

    fun setFontFamily(fontFamily: ReaderFontFamily) {
        _uiState.update { it.copy(fontFamily = fontFamily) }
    }

    fun toggleControls() {
        _uiState.update { it.copy(isControlsVisible = !it.isControlsVisible) }
    }

    fun openSheet(sheet: ReaderSheet) {
        _uiState.update { it.copy(activeSheet = sheet) }
    }

    fun closeSheet() {
        _uiState.update { it.copy(activeSheet = ReaderSheet.NONE) }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    /**
     * Exact Resume & ReadingTracker persistence on lifecycle pause/stop.
     * Guaranteed to execute off main thread.
     */
    fun onPause() {
        val nowMs = System.currentTimeMillis()
        val locatorJson = _uiState.value.currentLocatorJson
        val progression = _uiState.value.progression

        viewModelScope.launch(ioDispatcher) {
            if (locatorJson != null) {
                persistCurrentLocator(locatorJson, progression)
            }
            // Flush and close reading interval on pause
            val intervals = tracker?.pauseSession(nowMs) ?: emptyList()
            recordIntervalsToLedger(intervals)
        }
    }

    private suspend fun persistCurrentLocator(locatorJson: String, progression: Double) {
        withContext(ioDispatcher) {
            bookDao.upsertReadingPosition(
                ReadingPositionEntity(
                    bookId = bookId,
                    locatorJson = locatorJson,
                    totalProgression = progression,
                    updatedAt = System.currentTimeMillis()
                )
            )
            lastSavedLocatorJson = locatorJson
        }
    }

    private fun flushReadingSession(nowMs: Long) {
        viewModelScope.launch(ioDispatcher) {
            val intervals = tracker?.flush(nowMs) ?: emptyList()
            recordIntervalsToLedger(intervals)
            lastFlushTimestampMs = nowMs
        }
    }

    private suspend fun recordIntervalsToLedger(intervals: List<TrackedInterval>) {
        withContext(ioDispatcher) {
            for (interval in intervals) {
                if (interval.activeSeconds > 0 || interval.positionsCounted > 0) {
                    readingLedgerDao.insertInterval(
                        ReadingIntervalEntity(
                            contentKey = interval.contentKey,
                            titleSnapshot = interval.titleSnapshot,
                            startedAt = interval.startedAt.toEpochMilli(),
                            endedAt = interval.endedAt.toEpochMilli(),
                            zoneId = interval.zoneId.id,
                            utcOffsetSeconds = interval.utcOffsetSeconds,
                            logicalDay = interval.logicalDay.toIsoString(),
                            activeSeconds = interval.activeSeconds,
                            positionsCounted = interval.positionsCounted,
                            elapsedRealtimeStart = interval.elapsedRealtimeStart,
                            clockSuspect = if (interval.clockSuspect) 1 else 0,
                            closed = 1
                        )
                    )

                    // Enforce distinct-position-once-per-day via DAO conflict strategy (OnConflictStrategy.IGNORE)
                    for (pos in interval.distinctPositions) {
                        readingLedgerDao.recordDailyPosition(
                            DailyPositionEntity(
                                logicalDay = interval.logicalDay.toIsoString(),
                                contentKey = interval.contentKey,
                                positionIndex = pos,
                                dwellMs = ReadingTrackerConstants.MIN_DWELL_SECONDS * 1000L
                            )
                        )
                    }
                }
            }
        }
    }
}
