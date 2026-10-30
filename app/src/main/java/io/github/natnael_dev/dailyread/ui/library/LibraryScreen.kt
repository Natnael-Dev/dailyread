package io.github.natnael_dev.dailyread.ui.library

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import io.github.natnael_dev.dailyread.ui.theme.BotanicalBackground
import io.github.natnael_dev.dailyread.ui.theme.BotanicalError
import io.github.natnael_dev.dailyread.ui.theme.BotanicalErrorContainer
import io.github.natnael_dev.dailyread.ui.theme.BotanicalOnErrorContainer
import io.github.natnael_dev.dailyread.ui.theme.BotanicalOnPrimary
import io.github.natnael_dev.dailyread.ui.theme.BotanicalOnPrimaryFixed
import io.github.natnael_dev.dailyread.ui.theme.BotanicalOnSecondaryContainer
import io.github.natnael_dev.dailyread.ui.theme.BotanicalOnSecondaryFixed
import io.github.natnael_dev.dailyread.ui.theme.BotanicalOnSurface
import io.github.natnael_dev.dailyread.ui.theme.BotanicalOnSurfaceVariant
import io.github.natnael_dev.dailyread.ui.theme.BotanicalOutlineVariant
import io.github.natnael_dev.dailyread.ui.theme.BotanicalPrimary
import io.github.natnael_dev.dailyread.ui.theme.BotanicalPrimaryContainer
import io.github.natnael_dev.dailyread.ui.theme.BotanicalPrimaryFixed
import io.github.natnael_dev.dailyread.ui.theme.BotanicalSecondaryContainer
import io.github.natnael_dev.dailyread.ui.theme.BotanicalSecondaryFixed
import io.github.natnael_dev.dailyread.ui.theme.BotanicalSurface
import io.github.natnael_dev.dailyread.ui.theme.BotanicalSurfaceContainer
import io.github.natnael_dev.dailyread.ui.theme.BotanicalSurfaceContainerHigh
import io.github.natnael_dev.dailyread.ui.theme.BotanicalSurfaceContainerHighest
import io.github.natnael_dev.dailyread.ui.theme.BotanicalSurfaceContainerLow
import io.github.natnael_dev.dailyread.ui.theme.BotanicalSurfaceContainerLowest
import io.github.natnael_dev.dailyread.ui.theme.BotanicalTertiary
import io.github.natnael_dev.dailyread.ui.theme.BotanicalTypography

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    modifier: Modifier = Modifier,
    onBookClick: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // SAF Directory picker launcher
    val openTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onFolderSelected(context, uri)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BotanicalBackground)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Top App Bar
            LibraryTopBar()

            // Main Content Area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Header with Title and Search/View toggles
                LibraryHeader(
                    booksCount = uiState.totalBooksCount,
                    isSearchActive = uiState.isSearchActive,
                    viewMode = uiState.viewMode,
                    onToggleSearch = { viewModel.onToggleSearch(!uiState.isSearchActive) },
                    onToggleViewMode = { viewModel.onToggleViewMode() }
                )

                // Search Bar (when active)
                AnimatedVisibility(visible = uiState.isSearchActive) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search by title or author...", style = BotanicalTypography.bodyMedium) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            trailingIcon = {
                                if (uiState.searchQuery.isNotBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .clickable { viewModel.onSearchQueryChanged("") }
                                            .padding(8.dp)
                                    ) {
                                        Text("✕", fontSize = 14.sp, color = BotanicalOnSurfaceVariant)
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = BotanicalSurfaceContainerLowest,
                                unfocusedContainerColor = BotanicalSurfaceContainerLow,
                                focusedBorderColor = BotanicalPrimary,
                                unfocusedBorderColor = BotanicalOutlineVariant
                            )
                        )
                    }
                }

                // Quick Import Pill
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    QuickImportPill(
                        onImportClick = {
                            viewModel.onImportSheetVisibilityChanged(true)
                        }
                    )
                }

                // Status banners (Scanning, Fingerprinting progress, Permission Denied)
                if (uiState.isScanning) {
                    ScanningBanner(message = uiState.scanningMessage)
                }

                uiState.fingerprintProgress?.let { progress ->
                    FingerprintingProgressBanner(progress = progress)
                }

                if (uiState.isPermissionDenied) {
                    PermissionDeniedBanner(
                        onRetryClick = {
                            openTreeLauncher.launch(null)
                        }
                    )
                }

                // Filter Ribbon & Sort selector
                FilterChipsRibbon(
                    activeFilter = uiState.activeFilter,
                    totalCount = uiState.totalBooksCount,
                    readingCount = uiState.readingCount,
                    finishedCount = uiState.finishedCount,
                    unreadCount = uiState.unreadCount,
                    currentSort = uiState.sortOption,
                    onFilterSelect = { viewModel.onFilterSelected(it) },
                    onSortSelect = { viewModel.onSortOptionSelected(it) }
                )

                // Main Books Content: Empty state or Populated Grid/List
                if (uiState.books.isEmpty() && !uiState.isScanning) {
                    EmptyLibraryState(
                        onImportClick = {
                            openTreeLauncher.launch(null)
                        }
                    )
                } else {
                    if (uiState.viewMode == ViewMode.GRID) {
                        BooksGrid(
                            books = uiState.filteredBooks,
                            modifier = Modifier.weight(1f),
                            onBookClick = onBookClick
                        )
                    } else {
                        BooksList(
                            books = uiState.filteredBooks,
                            modifier = Modifier.weight(1f),
                            onBookClick = onBookClick
                        )
                    }

                    // Storage Footprint Card
                    StorageFootprintCard(
                        totalSizeBytes = uiState.totalSizeBytes,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // Bottom Navigation Bar
            BottomNavBar(
                activeTab = "library"
            )
        }

        // Bottom Action Drawer / Import Sheet
        AnimatedVisibility(
            visible = uiState.isImportSheetVisible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            ImportBottomSheet(
                onDismiss = { viewModel.onImportSheetVisibilityChanged(false) },
                onSelectFolder = {
                    viewModel.onImportSheetVisibilityChanged(false)
                    openTreeLauncher.launch(null)
                }
            )
        }
    }
}

// -------------------------------------------------------------
// Sub-components matching dailyread_local_library/code.html
// -------------------------------------------------------------

@Composable
fun LibraryTopBar() {
    Surface(
        color = BotanicalSurface.copy(alpha = 0.95f),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(BotanicalPrimaryContainer.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📖", fontSize = 18.sp)
                }
                Text(
                    text = "DailyRead",
                    style = BotanicalTypography.headlineSmall,
                    color = BotanicalOnSurface,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Synced indicator pill
                Row(
                    modifier = Modifier
                        .background(BotanicalSurfaceContainer, RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(modifier = Modifier.size(6.dp).background(BotanicalPrimary, CircleShape))
                    Text(
                        text = "Synced",
                        style = BotanicalTypography.labelSmall,
                        color = BotanicalPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Profile avatar circle
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(BotanicalPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("👤", fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun LibraryHeader(
    booksCount: Int,
    isSearchActive: Boolean,
    viewMode: ViewMode,
    onToggleSearch: () -> Unit,
    onToggleViewMode: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "My Library",
                style = BotanicalTypography.headlineMedium,
                color = BotanicalOnSurface,
                fontWeight = FontWeight.Bold
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Box(modifier = Modifier.size(8.dp).background(BotanicalPrimary, CircleShape))
                Text(
                    text = "$booksCount Books • Offline Storage",
                    style = BotanicalTypography.labelMedium,
                    color = BotanicalOnSurfaceVariant
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Search toggle pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isSearchActive) BotanicalPrimary else BotanicalSurfaceContainerLow)
                    .border(1.dp, BotanicalOutlineVariant.copy(alpha = 0.4f), RoundedCornerShape(999.dp))
                    .clickable { onToggleSearch() }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "🔍",
                    fontSize = 12.sp,
                    color = if (isSearchActive) BotanicalOnPrimary else BotanicalPrimary
                )
                Text(
                    text = "Search",
                    style = BotanicalTypography.labelSmall,
                    color = if (isSearchActive) BotanicalOnPrimary else BotanicalOnSurfaceVariant
                )
            }

            // View mode toggle
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(BotanicalSurfaceContainerLow)
                    .border(1.dp, BotanicalOutlineVariant.copy(alpha = 0.4f), RoundedCornerShape(999.dp))
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (viewMode == ViewMode.GRID) BotanicalSurfaceContainerLowest else Color.Transparent)
                        .clickable { if (viewMode != ViewMode.GRID) onToggleViewMode() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("⊞", fontSize = 14.sp, color = if (viewMode == ViewMode.GRID) BotanicalPrimary else BotanicalOnSurfaceVariant)
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (viewMode == ViewMode.LIST) BotanicalSurfaceContainerLowest else Color.Transparent)
                        .clickable { if (viewMode != ViewMode.LIST) onToggleViewMode() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("☰", fontSize = 14.sp, color = if (viewMode == ViewMode.LIST) BotanicalPrimary else BotanicalOnSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun QuickImportPill(
    onImportClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onImportClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BotanicalSurfaceContainerLow),
        border = androidx.compose.foundation.BorderStroke(1.dp, BotanicalOutlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(BotanicalPrimaryFixed, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📂", fontSize = 20.sp)
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Import to Local Shelf",
                            style = BotanicalTypography.titleSmall,
                            color = BotanicalOnSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .background(BotanicalSecondaryContainer, RoundedCornerShape(999.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "EPUB / PDF",
                                style = BotanicalTypography.labelSmall,
                                color = BotanicalOnSecondaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "Direct via Android Document Picker",
                        style = BotanicalTypography.bodySmall,
                        color = BotanicalOnSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(BotanicalPrimary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("＋", fontSize = 16.sp, color = BotanicalPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun FilterChipsRibbon(
    activeFilter: ReadingStatus,
    totalCount: Int,
    readingCount: Int,
    finishedCount: Int,
    unreadCount: Int,
    currentSort: SortOption,
    onFilterSelect: (ReadingStatus) -> Unit,
    onSortSelect: (SortOption) -> Unit
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            label = "All ($totalCount)",
            selected = activeFilter == ReadingStatus.ALL,
            onClick = { onFilterSelect(ReadingStatus.ALL) }
        )
        FilterChip(
            label = "Reading ($readingCount)",
            selected = activeFilter == ReadingStatus.READING,
            onClick = { onFilterSelect(ReadingStatus.READING) }
        )
        FilterChip(
            label = "Finished ($finishedCount)",
            selected = activeFilter == ReadingStatus.FINISHED,
            onClick = { onFilterSelect(ReadingStatus.FINISHED) }
        )
        FilterChip(
            label = "Unread ($unreadCount)",
            selected = activeFilter == ReadingStatus.UNREAD,
            onClick = { onFilterSelect(ReadingStatus.UNREAD) }
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Sort toggle chip
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(BotanicalSurfaceContainer)
                .clickable {
                    val nextSort = when (currentSort) {
                        SortOption.RECENT -> SortOption.TITLE
                        SortOption.TITLE -> SortOption.AUTHOR
                        SortOption.AUTHOR -> SortOption.RECENT
                    }
                    onSortSelect(nextSort)
                }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Sort: ${currentSort.name.lowercase().replaceFirstChar { it.uppercase() }}",
                style = BotanicalTypography.labelSmall,
                color = BotanicalOnSurfaceVariant
            )
        }
    }
}

@Composable
fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) BotanicalPrimary else BotanicalSurfaceContainerHigh)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = BotanicalTypography.labelMedium,
            color = if (selected) BotanicalOnPrimary else BotanicalOnSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun BooksGrid(
    books: List<BookItemUiModel>,
    modifier: Modifier = Modifier,
    onBookClick: (Long) -> Unit = {}
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(books, key = { it.id }) { book ->
            BookGridCard(book = book, onClick = { onBookClick(book.id) })
        }
    }
}

@Composable
fun BooksList(
    books: List<BookItemUiModel>,
    modifier: Modifier = Modifier,
    onBookClick: (Long) -> Unit = {}
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(books, key = { it.id }) { book ->
            BookListCard(book = book, onClick = { onBookClick(book.id) })
        }
    }
}

@Composable
fun BookGridCard(
    book: BookItemUiModel,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BotanicalSurfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Book cover container (2:3 aspect ratio)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (book.format == "PDF") BotanicalSecondaryFixed.copy(alpha = 0.5f)
                        else BotanicalPrimaryFixed.copy(alpha = 0.5f)
                    )
            ) {
                val coverBitmap = remember(book.coverPath) {
                    if (book.coverPath != null) {
                        try {
                            val f = File(book.coverPath)
                            if (f.exists() && f.length() > 0) {
                                BitmapFactory.decodeFile(book.coverPath)?.asImageBitmap()
                            } else null
                        } catch (e: Throwable) {
                            null
                        }
                    } else null
                }

                if (coverBitmap != null) {
                    Image(
                        bitmap = coverBitmap,
                        contentDescription = "Cover of ${book.title}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Designed placeholder tile using theme tokens
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = book.title,
                            style = BotanicalTypography.titleSmall,
                            color = BotanicalOnSurface,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        book.author?.let {
                            Text(
                                text = it,
                                style = BotanicalTypography.bodySmall,
                                color = BotanicalOnSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                // Completed badge (top-left)
                if (book.readingStatus == ReadingStatus.FINISHED) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .background(BotanicalPrimary, RoundedCornerShape(999.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text("✓ Done", style = BotanicalTypography.labelSmall, color = BotanicalOnPrimary, fontSize = 10.sp)
                    }
                }

                // Missing file badge
                if (book.isMissing) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .background(BotanicalError, RoundedCornerShape(999.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text("⚠️ Missing", style = BotanicalTypography.labelSmall, color = Color.White, fontSize = 10.sp)
                    }
                }

                // Format badge (top-right)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(BotanicalSurfaceContainerLowest.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = book.format,
                        style = BotanicalTypography.labelSmall,
                        color = if (book.format == "PDF") BotanicalTertiary else BotanicalPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Book Details
            Text(
                text = book.title,
                style = BotanicalTypography.titleSmall,
                color = BotanicalOnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = book.author ?: "Unknown Author",
                style = BotanicalTypography.bodySmall,
                color = BotanicalOnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Reading Progress Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (book.readingStatus) {
                        ReadingStatus.FINISHED -> "Completed 100%"
                        ReadingStatus.READING -> "${book.percentRead}% read"
                        ReadingStatus.UNREAD -> "Unread"
                        else -> ""
                    },
                    style = BotanicalTypography.labelSmall,
                    color = if (book.readingStatus == ReadingStatus.FINISHED) BotanicalPrimary else BotanicalOnSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = book.currentPositionDisplay,
                    style = BotanicalTypography.labelSmall,
                    color = if (book.format == "PDF") BotanicalTertiary else BotanicalPrimary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = { book.percentRead / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = if (book.format == "PDF") BotanicalTertiary else BotanicalPrimary,
                trackColor = BotanicalSurfaceContainerHigh,
            )
        }
    }
}

@Composable
fun BookListCard(
    book: BookItemUiModel,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BotanicalSurfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 44.dp, height = 66.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (book.format == "PDF") BotanicalSecondaryFixed else BotanicalPrimaryFixed),
                contentAlignment = Alignment.Center
            ) {
                val listCover = remember(book.coverPath) {
                    if (book.coverPath != null) {
                        try {
                            val f = File(book.coverPath)
                            if (f.exists() && f.length() > 0) {
                                BitmapFactory.decodeFile(book.coverPath)?.asImageBitmap()
                            } else null
                        } catch (e: Throwable) {
                            null
                        }
                    } else null
                }

                if (listCover != null) {
                    Image(
                        bitmap = listCover,
                        contentDescription = "Cover of ${book.title}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(book.format, style = BotanicalTypography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = book.title,
                        style = BotanicalTypography.titleSmall,
                        color = BotanicalOnSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (book.isMissing) {
                        Text("Missing", style = BotanicalTypography.labelSmall, color = BotanicalError)
                    }
                }
                Text(
                    text = book.author ?: "Unknown Author",
                    style = BotanicalTypography.bodySmall,
                    color = BotanicalOnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { book.percentRead / 100f },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(999.dp)),
                    color = BotanicalPrimary,
                    trackColor = BotanicalSurfaceContainerHigh
                )
            }
        }
    }
}

@Composable
fun StorageFootprintCard(
    totalSizeBytes: Long,
    modifier: Modifier = Modifier
) {
    val mb = totalSizeBytes / (1024.0 * 1024.0)
    val displaySize = if (mb > 0) String.format("%.1f MB", mb) else "0.0 MB"

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BotanicalSurfaceContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(BotanicalSecondaryFixed, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📦", fontSize = 18.sp)
                }
                Column {
                    Text(
                        text = "Storage Footprint",
                        style = BotanicalTypography.titleSmall,
                        color = BotanicalOnSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$displaySize • Zero Cloud Dependency",
                        style = BotanicalTypography.bodySmall,
                        color = BotanicalOnSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .background(BotanicalSurfaceContainerLowest, RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Local-Only",
                    style = BotanicalTypography.labelSmall,
                    color = BotanicalPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun EmptyLibraryState(
    onImportClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(BotanicalPrimaryFixed, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("📚", fontSize = 36.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Your Library is Empty",
            style = BotanicalTypography.headlineSmall,
            color = BotanicalOnSurface,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Add EPUB or PDF books from your device or SD card. DailyRead reads your files in place with zero cloud dependency.",
            style = BotanicalTypography.bodyMedium,
            color = BotanicalOnSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(BotanicalPrimary)
                .clickable { onImportClick() }
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Select Folder to Import",
                style = BotanicalTypography.labelLarge,
                color = BotanicalOnPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ScanningBanner(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = BotanicalPrimaryFixed)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = BotanicalPrimary,
                strokeWidth = 2.dp
            )
            Text(
                text = message,
                style = BotanicalTypography.bodySmall,
                color = BotanicalOnPrimaryFixed
            )
        }
    }
}

@Composable
fun FingerprintingProgressBanner(progress: io.github.natnael_dev.dailyread.data.ingest.FingerprintProgress) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = BotanicalSecondaryFixed)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Indexing & fingerprinting: ${progress.completed} of ${progress.total}",
                    style = BotanicalTypography.labelSmall,
                    color = BotanicalOnSecondaryFixed,
                    fontWeight = FontWeight.Bold
                )
                progress.currentBookTitle?.let {
                    Text(
                        text = it,
                        style = BotanicalTypography.labelSmall,
                        color = BotanicalOnSecondaryFixed,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { if (progress.total > 0) progress.completed.toFloat() / progress.total else 0f },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(999.dp)),
                color = BotanicalPrimary,
                trackColor = BotanicalSurfaceContainerHighest
            )
        }
    }
}

@Composable
fun PermissionDeniedBanner(onRetryClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = BotanicalErrorContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("⚠️", fontSize = 18.sp)
                Column {
                    Text(
                        text = "Folder Permission Needed",
                        style = BotanicalTypography.titleSmall,
                        color = BotanicalOnErrorContainer,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Grant SAF permission to read books in-place.",
                        style = BotanicalTypography.bodySmall,
                        color = BotanicalOnErrorContainer
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(BotanicalError)
                    .clickable { onRetryClick() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Grant", style = BotanicalTypography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ImportBottomSheet(
    onDismiss: () -> Unit,
    onSelectFolder: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        color = BotanicalSurfaceContainerLowest,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(BotanicalPrimaryFixed, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📥", fontSize = 20.sp)
                    }
                    Column {
                        Text(
                            text = "Import Local Books",
                            style = BotanicalTypography.titleMedium,
                            color = BotanicalOnSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Access files from storage or SD card",
                            style = BotanicalTypography.bodySmall,
                            color = BotanicalOnSurfaceVariant
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onDismiss() }
                        .padding(8.dp)
                ) {
                    Text("✕", fontSize = 16.sp, color = BotanicalOnSurfaceVariant)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Folder option (tree)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectFolder() },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = BotanicalSurfaceContainerLow)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📖", fontSize = 28.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Select Folder",
                            style = BotanicalTypography.labelLarge,
                            color = BotanicalOnSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Batch scan all books",
                            style = BotanicalTypography.labelSmall,
                            color = BotanicalOnSurfaceVariant
                        )
                    }
                }

                // Single file option
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectFolder() },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = BotanicalSurfaceContainerLow)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📄", fontSize = 28.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Single Document",
                            style = BotanicalTypography.labelLarge,
                            color = BotanicalOnSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "EPUB or PDF file",
                            style = BotanicalTypography.labelSmall,
                            color = BotanicalOnSurfaceVariant
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text("🔒", fontSize = 14.sp)
                Text(
                    text = "SAF (Storage Access Framework) retains full local privacy. Books are read in place without copying.",
                    style = BotanicalTypography.labelSmall,
                    color = BotanicalOnSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun BottomNavBar(activeTab: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = BotanicalSurfaceContainerLowest.copy(alpha = 0.95f),
            shadowElevation = 6.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, BotanicalOutlineVariant.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavTabItem(title = "Today", icon = "☀️", selected = activeTab == "today")
                NavTabItem(title = "Library", icon = "📚", selected = activeTab == "library")
                NavTabItem(title = "Plans", icon = "🧭", selected = activeTab == "plans")
                NavTabItem(title = "Settings", icon = "⚙️", selected = activeTab == "settings")
            }
        }
    }
}

@Composable
fun NavTabItem(title: String, icon: String, selected: Boolean) {
    if (selected) {
        Row(
            modifier = Modifier
                .background(BotanicalPrimary.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(icon, fontSize = 16.sp)
            Text(
                text = title,
                style = BotanicalTypography.labelSmall,
                color = BotanicalPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    } else {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, fontSize = 16.sp)
            Text(
                text = title,
                style = BotanicalTypography.labelSmall,
                color = BotanicalOnSurfaceVariant
            )
        }
    }
}
