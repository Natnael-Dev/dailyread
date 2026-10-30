package io.github.natnael_dev.dailyread.ui.library

import io.github.natnael_dev.dailyread.data.db.entity.BookEntity
import io.github.natnael_dev.dailyread.data.db.entity.BookSourceEntity
import io.github.natnael_dev.dailyread.data.ingest.FakeBookDao
import io.github.natnael_dev.dailyread.data.ingest.FakeLibrarySourceDao
import io.github.natnael_dev.dailyread.data.ingest.FakeTreeDocumentProvider
import io.github.natnael_dev.dailyread.data.ingest.IngestionEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var bookDao: FakeBookDao
    private lateinit var librarySourceDao: FakeLibrarySourceDao
    private lateinit var documentProvider: FakeTreeDocumentProvider
    private lateinit var ingestionEngine: IngestionEngine
    private lateinit var viewModel: LibraryViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        bookDao = FakeBookDao()
        librarySourceDao = FakeLibrarySourceDao()
        documentProvider = FakeTreeDocumentProvider()
        ingestionEngine = IngestionEngine(bookDao, librarySourceDao, documentProvider)
        viewModel = LibraryViewModel(bookDao, librarySourceDao, ingestionEngine)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialEmptyState() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(0, state.totalBooksCount)
        assertEquals(0, state.filteredBooks.size)
        assertEquals(ReadingStatus.ALL, state.activeFilter)
        assertEquals(SortOption.RECENT, state.sortOption)
        assertEquals(ViewMode.GRID, state.viewMode)
        assertFalse(state.isScanning)
        assertFalse(state.isPermissionDenied)
    }

    @Test
    fun testPopulatedStateWithFilterCounts() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        // Book 1: Reading
        val b1 = bookDao.insertBook(
            BookEntity(
                id = 1,
                contentKey = "k1",
                format = "EPUB",
                title = "The Nature of Thought",
                author = "Brand Blanshard",
                sizeBytes = 1000,
                lastOpenedAt = 5000L,
                addedAt = 1000L,
                fingerprintState = "READY"
            )
        )
        bookDao.insertBookSource(
            BookSourceEntity(bookId = b1, documentUri = "uri1", documentId = "doc1", displayName = "Thought.epub", sizeBytes = 1000, lastModified = 1000, lastSeenAt = 1000, state = "PRESENT")
        )

        // Book 2: Finished
        val b2 = bookDao.insertBook(
            BookEntity(
                id = 2,
                contentKey = "k2",
                format = "PDF",
                title = "Meditations",
                author = "Marcus Aurelius",
                sizeBytes = 2000,
                finishedAt = 6000L,
                addedAt = 2000L,
                fingerprintState = "READY"
            )
        )
        bookDao.insertBookSource(
            BookSourceEntity(bookId = b2, documentUri = "uri2", documentId = "doc2", displayName = "Meditations.pdf", sizeBytes = 2000, lastModified = 2000, lastSeenAt = 2000, state = "PRESENT")
        )

        // Book 3: Unread
        val b3 = bookDao.insertBook(
            BookEntity(
                id = 3,
                contentKey = "k3",
                format = "EPUB",
                title = "Silent Spring",
                author = "Rachel Carson",
                sizeBytes = 3000,
                addedAt = 3000L,
                fingerprintState = "READY"
            )
        )
        bookDao.insertBookSource(
            BookSourceEntity(bookId = b3, documentUri = "uri3", documentId = "doc3", displayName = "Spring.epub", sizeBytes = 3000, lastModified = 3000, lastSeenAt = 3000, state = "PRESENT")
        )

        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(3, state.totalBooksCount)
        assertEquals(1, state.readingCount)
        assertEquals(1, state.finishedCount)
        assertEquals(1, state.unreadCount)
        assertEquals(6000L, state.totalSizeBytes)
    }

    @Test
    fun testFilteringByStatus() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val b1 = bookDao.insertBook(BookEntity(contentKey = "k1", format = "EPUB", title = "B1", sizeBytes = 1000L, lastOpenedAt = 100L, addedAt = 100L))
        bookDao.insertBookSource(BookSourceEntity(bookId = b1, documentUri = "u1", documentId = "d1", displayName = "b1", sizeBytes = 10, lastModified = 10, lastSeenAt = 10))

        val b2 = bookDao.insertBook(BookEntity(contentKey = "k2", format = "PDF", title = "B2", sizeBytes = 2000L, finishedAt = 200L, addedAt = 200L))
        bookDao.insertBookSource(BookSourceEntity(bookId = b2, documentUri = "u2", documentId = "d2", displayName = "b2", sizeBytes = 20, lastModified = 20, lastSeenAt = 20))

        advanceUntilIdle()
        viewModel.onFilterSelected(ReadingStatus.FINISHED)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ReadingStatus.FINISHED, state.activeFilter)
        assertEquals(1, state.filteredBooks.size)
        assertEquals("B2", state.filteredBooks[0].title)
    }

    @Test
    fun testSearchFiltering() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val b1 = bookDao.insertBook(BookEntity(contentKey = "k1", format = "EPUB", title = "Walden", author = "Thoreau", sizeBytes = 1000L, addedAt = 100L))
        bookDao.insertBookSource(BookSourceEntity(bookId = b1, documentUri = "u1", documentId = "d1", displayName = "b1", sizeBytes = 10, lastModified = 10, lastSeenAt = 10))

        val b2 = bookDao.insertBook(BookEntity(contentKey = "k2", format = "PDF", title = "Botanical Drawing", author = "Brown", sizeBytes = 2000L, addedAt = 200L))
        bookDao.insertBookSource(BookSourceEntity(bookId = b2, documentUri = "u2", documentId = "d2", displayName = "b2", sizeBytes = 20, lastModified = 20, lastSeenAt = 20))

        advanceUntilIdle()
        viewModel.onSearchQueryChanged("Walden")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.filteredBooks.size)
        assertEquals("Walden", state.filteredBooks[0].title)
    }

    @Test
    fun testSortingOptions() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val b1 = bookDao.insertBook(BookEntity(contentKey = "k1", format = "EPUB", title = "Zebra", author = "Aaron", sizeBytes = 1000L, addedAt = 100L))
        bookDao.insertBookSource(BookSourceEntity(bookId = b1, documentUri = "u1", documentId = "d1", displayName = "b1", sizeBytes = 10, lastModified = 10, lastSeenAt = 10))

        val b2 = bookDao.insertBook(BookEntity(contentKey = "k2", format = "PDF", title = "Apple", author = "Zach", sizeBytes = 2000L, addedAt = 200L))
        bookDao.insertBookSource(BookSourceEntity(bookId = b2, documentUri = "u2", documentId = "d2", displayName = "b2", sizeBytes = 20, lastModified = 20, lastSeenAt = 20))

        advanceUntilIdle()

        // Sort by Title
        viewModel.onSortOptionSelected(SortOption.TITLE)
        advanceUntilIdle()
        assertEquals("Apple", viewModel.uiState.value.filteredBooks[0].title)
        assertEquals("Zebra", viewModel.uiState.value.filteredBooks[1].title)

        // Sort by Author
        viewModel.onSortOptionSelected(SortOption.AUTHOR)
        advanceUntilIdle()
        assertEquals("Zebra", viewModel.uiState.value.filteredBooks[0].title) // Aaron is first author
        assertEquals("Apple", viewModel.uiState.value.filteredBooks[1].title) // Zach is second author
    }

    @Test
    fun testMissingFileDetectedInUiState() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val b1 = bookDao.insertBook(BookEntity(contentKey = "k1", format = "EPUB", title = "Lost Book", sizeBytes = 1000L, addedAt = 100L))
        bookDao.insertBookSource(
            BookSourceEntity(bookId = b1, documentUri = "u1", documentId = "d1", displayName = "lost.epub", sizeBytes = 10, lastModified = 10, lastSeenAt = 10, state = "MISSING")
        )

        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(1, state.filteredBooks.size)
        assertTrue(state.filteredBooks[0].isMissing)
    }

    @Test
    fun testViewModeToggle() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        assertEquals(ViewMode.GRID, viewModel.uiState.value.viewMode)
        viewModel.onToggleViewMode()
        advanceUntilIdle()
        assertEquals(ViewMode.LIST, viewModel.uiState.value.viewMode)
        viewModel.onToggleViewMode()
        advanceUntilIdle()
        assertEquals(ViewMode.GRID, viewModel.uiState.value.viewMode)
    }

    @Test
    fun testPermissionDeniedState() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isPermissionDenied)
        viewModel.onPermissionDenied()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isPermissionDenied)
    }
}
