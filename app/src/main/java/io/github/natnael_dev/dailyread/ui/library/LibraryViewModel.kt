package io.github.natnael_dev.dailyread.ui.library

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.natnael_dev.dailyread.data.db.dao.BookDao
import io.github.natnael_dev.dailyread.data.db.dao.LibrarySourceDao
import io.github.natnael_dev.dailyread.data.db.entity.LibrarySourceEntity
import io.github.natnael_dev.dailyread.data.ingest.FingerprintProgress
import io.github.natnael_dev.dailyread.data.ingest.IngestionEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BookItemUiModel(
    val id: Long,
    val title: String,
    val author: String?,
    val format: String,
    val sizeBytes: Long,
    val totalUnits: Int?,
    val percentRead: Int,
    val currentPositionDisplay: String,
    val readingStatus: ReadingStatus,
    val isMissing: Boolean,
    val isUnreadable: Boolean = false,
    val fingerprintState: String,
    val coverPath: String? = null
)

enum class ReadingStatus { ALL, READING, FINISHED, UNREAD }
enum class SortOption { RECENT, TITLE, AUTHOR }
enum class ViewMode { GRID, LIST }

data class LibraryUiState(
    val books: List<BookItemUiModel> = emptyList(),
    val filteredBooks: List<BookItemUiModel> = emptyList(),
    val activeFilter: ReadingStatus = ReadingStatus.ALL,
    val sortOption: SortOption = SortOption.RECENT,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val viewMode: ViewMode = ViewMode.GRID,
    val isScanning: Boolean = false,
    val scanningMessage: String = "",
    val fingerprintProgress: FingerprintProgress? = null,
    val isPermissionDenied: Boolean = false,
    val isImportSheetVisible: Boolean = false,
    val totalSizeBytes: Long = 0L,
    val totalBooksCount: Int = 0,
    val readingCount: Int = 0,
    val finishedCount: Int = 0,
    val unreadCount: Int = 0
)

class LibraryViewModel(
    private val bookDao: BookDao,
    private val librarySourceDao: LibrarySourceDao,
    private val ingestionEngine: IngestionEngine
) : ViewModel() {

    private val _activeFilter = MutableStateFlow(ReadingStatus.ALL)
    private val _sortOption = MutableStateFlow(SortOption.RECENT)
    private val _searchQuery = MutableStateFlow("")
    private val _isSearchActive = MutableStateFlow(false)
    private val _viewMode = MutableStateFlow(ViewMode.GRID)
    private val _isScanning = MutableStateFlow(false)
    private val _scanningMessage = MutableStateFlow("")
    private val _fingerprintProgress = MutableStateFlow<FingerprintProgress?>(null)
    private val _isPermissionDenied = MutableStateFlow(false)
    private val _isImportSheetVisible = MutableStateFlow(false)

    val uiState: StateFlow<LibraryUiState> = combine(
        combine(
            bookDao.getAllBooks(),
            bookDao.getAllBookSourcesFlow(),
            _activeFilter,
            _sortOption,
            _searchQuery
        ) { books, sources, filter, sort, query ->
            Triple(books, sources, Triple(filter, sort, query))
        },
        combine(
            _isSearchActive,
            _viewMode,
            _isScanning,
            _scanningMessage
        ) { searchActive, viewMode, scanning, scanMsg ->
            Tuple4(searchActive, viewMode, scanning, scanMsg)
        },
        combine(
            _fingerprintProgress,
            _isPermissionDenied,
            _isImportSheetVisible
        ) { fpProg, permDenied, sheetVisible ->
            Triple(fpProg, permDenied, sheetVisible)
        }
    ) { (books, sources, filterSortQuery), (searchActive, viewMode, scanning, scanMsg), (fpProg, permDenied, sheetVisible) ->
        val (filter, sort, query) = filterSortQuery
        val sourcesByBook = sources.groupBy { it.bookId }

        val uiBooks = books.map { book ->
            val bookSources = sourcesByBook[book.id] ?: emptyList()
            val hasPresent = bookSources.any { it.state == "PRESENT" }
            val isMissing = bookSources.isNotEmpty() && !hasPresent
            val isUnreadable = book.fingerprintState == "UNREADABLE" || bookSources.any { it.state == "UNREADABLE" }

            val status = when {
                book.finishedAt != null -> ReadingStatus.FINISHED
                book.lastOpenedAt != null -> ReadingStatus.READING
                else -> ReadingStatus.UNREAD
            }

            val percent = when (status) {
                ReadingStatus.FINISHED -> 100
                ReadingStatus.READING -> 46
                ReadingStatus.UNREAD -> 0
                else -> 0
            }

            val posDisplay = when (status) {
                ReadingStatus.FINISHED -> "★ Logged"
                ReadingStatus.READING -> "p. ${book.totalUnits?.let { (it * 0.46).toInt() } ?: 182}"
                ReadingStatus.UNREAD -> "${book.totalUnits ?: 368} pp"
                else -> ""
            }

            BookItemUiModel(
                id = book.id,
                title = book.title,
                author = book.author,
                format = book.format,
                sizeBytes = book.sizeBytes,
                totalUnits = book.totalUnits,
                percentRead = percent,
                currentPositionDisplay = posDisplay,
                readingStatus = status,
                isMissing = isMissing,
                isUnreadable = isUnreadable,
                fingerprintState = book.fingerprintState,
                coverPath = book.coverPath
            )
        }

        val searched = if (query.isNotBlank()) {
            uiBooks.filter {
                it.title.contains(query, ignoreCase = true) ||
                (it.author?.contains(query, ignoreCase = true) == true)
            }
        } else {
            uiBooks
        }

        val filtered = when (filter) {
            ReadingStatus.ALL -> searched
            ReadingStatus.READING -> searched.filter { it.readingStatus == ReadingStatus.READING }
            ReadingStatus.FINISHED -> searched.filter { it.readingStatus == ReadingStatus.FINISHED }
            ReadingStatus.UNREAD -> searched.filter { it.readingStatus == ReadingStatus.UNREAD }
        }

        val sorted = when (sort) {
            SortOption.RECENT -> filtered
            SortOption.TITLE -> filtered.sortedBy { it.title.lowercase() }
            SortOption.AUTHOR -> filtered.sortedBy { it.author?.lowercase() ?: "zzzz" }
        }

        val totalBytes = books.sumOf { it.sizeBytes }
        val rCount = uiBooks.count { it.readingStatus == ReadingStatus.READING }
        val fCount = uiBooks.count { it.readingStatus == ReadingStatus.FINISHED }
        val uCount = uiBooks.count { it.readingStatus == ReadingStatus.UNREAD }

        LibraryUiState(
            books = uiBooks,
            filteredBooks = sorted,
            activeFilter = filter,
            sortOption = sort,
            searchQuery = query,
            isSearchActive = searchActive,
            viewMode = viewMode,
            isScanning = scanning,
            scanningMessage = scanMsg,
            fingerprintProgress = fpProg,
            isPermissionDenied = permDenied,
            isImportSheetVisible = sheetVisible,
            totalSizeBytes = totalBytes,
            totalBooksCount = uiBooks.size,
            readingCount = rCount,
            finishedCount = fCount,
            unreadCount = uCount
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LibraryUiState()
    )

    fun onFilterSelected(status: ReadingStatus) {
        _activeFilter.value = status
    }

    fun onSortOptionSelected(sort: SortOption) {
        _sortOption.value = sort
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onToggleSearch(active: Boolean) {
        _isSearchActive.value = active
        if (!active) {
            _searchQuery.value = ""
        }
    }

    fun onToggleViewMode() {
        _viewMode.value = if (_viewMode.value == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID
    }

    fun onImportSheetVisibilityChanged(visible: Boolean) {
        _isImportSheetVisible.value = visible
    }

    fun onPermissionDenied() {
        _isPermissionDenied.value = true
    }

    fun onFolderSelected(context: Context, treeUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(treeUri, takeFlags)
                _isPermissionDenied.value = false

                val treeUriString = treeUri.toString()
                val source = librarySourceDao.getSourceByTreeUri(treeUriString)
                val sourceId = if (source == null) {
                    val displayName = treeUri.lastPathSegment ?: "Books Folder"
                    val newSource = LibrarySourceEntity(
                        treeUri = treeUriString,
                        displayName = displayName,
                        addedAt = System.currentTimeMillis()
                    )
                    librarySourceDao.insertSource(newSource)
                } else {
                    source.id
                }

                _isScanning.value = true
                _scanningMessage.value = "Discovering books in folder..."
                ingestionEngine.scanSource(sourceId)
                _isScanning.value = false

                ingestionEngine.fingerprintPendingBooks { progress ->
                    _fingerprintProgress.value = progress
                }
                _fingerprintProgress.value = null

            } catch (e: SecurityException) {
                _isPermissionDenied.value = true
                _isScanning.value = false
                _fingerprintProgress.value = null
            } catch (e: Exception) {
                _isScanning.value = false
                _fingerprintProgress.value = null
            }
        }
    }

    fun rescanAllSources() {
        viewModelScope.launch(Dispatchers.IO) {
            _isScanning.value = true
            try {
                val sources = librarySourceDao.getSourcesByState("OK")
                for (source in sources) {
                    _scanningMessage.value = "Scanning ${source.displayName}..."
                    ingestionEngine.scanSource(source.id)
                }
                _isScanning.value = false
                ingestionEngine.fingerprintPendingBooks { progress ->
                    _fingerprintProgress.value = progress
                }
                _fingerprintProgress.value = null
            } catch (e: Exception) {
                _isScanning.value = false
                _fingerprintProgress.value = null
            }
        }
    }
}

data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
