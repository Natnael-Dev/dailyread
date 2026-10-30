package io.github.natnael_dev.dailyread.ui.reader

import android.view.LayoutInflater
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import io.github.natnael_dev.dailyread.R
import io.github.natnael_dev.dailyread.data.db.dao.BookDao
import io.github.natnael_dev.dailyread.data.db.entity.BookmarkEntity
import io.github.natnael_dev.dailyread.data.reader.PdfReadingCache
import io.github.natnael_dev.dailyread.ui.theme.BotanicalPrimary
import io.github.natnael_dev.dailyread.ui.theme.BotanicalSurfaceContainerHigh
import io.github.natnael_dev.dailyread.ui.theme.BotanicalTertiary
import io.github.natnael_dev.dailyread.ui.theme.LiterataFontFamily
import io.github.natnael_dev.dailyread.ui.theme.ManropeFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    bookId: Long,
    viewModel: ReaderViewModel,
    bookDao: BookDao,
    pdfReadingCache: PdfReadingCache,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as FragmentActivity
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    // Host helper managing Readium fragment
    val readerHost = androidx.compose.runtime.remember {
        ReaderHost(
            activity = activity,
            bookDao = bookDao,
            pdfReadingCache = pdfReadingCache,
            viewModel = viewModel
        )
    }

    // Intercept hardware / system back to persist and navigate
    BackHandler {
        viewModel.onPause()
        onBack()
    }

    // Lifecycle observer for exact resume persistence on backgrounding
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                viewModel.onPause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.onPause()
            readerHost.onDestroy()
        }
    }

    LaunchedEffect(bookId) {
        readerHost.initialize()
        readerHost.loadPublication(bookId)
    }

    // Auto-clear toast after 3 seconds
    LaunchedEffect(uiState.statusMessage) {
        if (uiState.statusMessage != null) {
            delay(3000)
            viewModel.clearStatusMessage()
        }
    }

    val canvasBg = Color(uiState.theme.bgHex)
    val canvasText = Color(uiState.theme.textHex)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(canvasBg)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        viewModel.onUserInteraction()
                        viewModel.toggleControls()
                    }
                )
            }
    ) {
        // Readium Fragment Host
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                LayoutInflater.from(ctx).inflate(R.layout.reader_container, null, false)
            }
        )

        // Loading Indicator
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(canvasBg),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = BotanicalPrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Opening publication…",
                        color = canvasText,
                        fontFamily = ManropeFontFamily,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Tactile Bookmark Ribbon Attached to Page Top-Right (per Stitch design)
        val ribbonColor = if (uiState.isBookmarked) BotanicalTertiary else Color(0xFFD0C5B2)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 24.dp)
                .clickable {
                    viewModel.onUserInteraction()
                    viewModel.toggleBookmark()
                }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(width = 28.dp, height = 44.dp)
                        .background(ribbonColor, shape = RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp))
                        .shadow(4.dp, shape = RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Text(
                        text = if (uiState.isBookmarked) "★" else "☆",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }
        }

        // Top Floating Header Bar (Overlay)
        AnimatedVisibility(
            visible = uiState.isControlsVisible && !uiState.isLoading,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = BotanicalSurfaceContainerHigh.copy(alpha = 0.96f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Back button + titles
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            viewModel.onPause()
                            onBack()
                        }) {
                            Text(
                                text = "←",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1C1C16)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column(modifier = Modifier.padding(end = 8.dp)) {
                            Text(
                                text = uiState.title.ifEmpty { "DailyRead" },
                                style = MaterialTheme.typography.titleSmall,
                                fontFamily = LiterataFontFamily,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = Color(0xFF1C1C16)
                            )
                            if (uiState.chapterTitle.isNotEmpty()) {
                                Text(
                                    text = uiState.chapterTitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = ManropeFontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = Color(0xFF444841)
                                )
                            }
                        }
                    }

                    // Reader Tool Actions
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Bookmark Collection
                        IconButton(onClick = { viewModel.openSheet(ReaderSheet.BOOKMARKS) }) {
                            Text(
                                text = "🔖",
                                fontSize = 16.sp
                            )
                        }
                        // Search
                        IconButton(onClick = {
                            viewModel.onUserInteraction()
                            viewModel.toggleBookmark("Indexed Search Note")
                        }) {
                            Text(
                                text = "🔍",
                                fontSize = 15.sp
                            )
                        }
                        // Typography
                        Surface(
                            shape = CircleShape,
                            color = BotanicalPrimary.copy(alpha = 0.15f),
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { viewModel.openSheet(ReaderSheet.TYPOGRAPHY) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Aa",
                                    color = BotanicalPrimary,
                                    fontFamily = ManropeFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Floating Reader Dock (Overlay)
        AnimatedVisibility(
            visible = uiState.isControlsVisible && !uiState.isLoading,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = BotanicalSurfaceContainerHigh.copy(alpha = 0.96f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Page Progress Info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Page ${uiState.currentPage} • ${(uiState.progression * 100).toInt()}% read",
                            style = MaterialTheme.typography.titleSmall,
                            fontFamily = ManropeFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1C1C16)
                        )
                        Surface(
                            shape = CircleShape,
                            color = BotanicalPrimary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "${(uiState.progression * 100).toInt()}%",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                color = BotanicalPrimary,
                                fontFamily = ManropeFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Scrubber Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                viewModel.onUserInteraction()
                                viewModel.goBackward()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text(
                                text = "‹",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1C1C16)
                            )
                        }

                        Slider(
                            value = uiState.progression.toFloat(),
                            onValueChange = {},
                            enabled = false,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp),
                            colors = SliderDefaults.colors(
                                activeTrackColor = BotanicalPrimary,
                                inactiveTrackColor = Color(0xFFD0C5B2)
                            )
                        )

                        IconButton(
                            onClick = {
                                viewModel.onUserInteraction()
                                viewModel.goForward()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text(
                                text = "›",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1C1C16)
                            )
                        }
                    }

                    // Bottom Row: Chapter Skips & 4-Theme Switcher
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Chapter Skips
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFF1EEE4),
                                modifier = Modifier.clickable {
                                    viewModel.onUserInteraction()
                                    viewModel.goBackward()
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "«",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF444841)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Prev",
                                        fontSize = 11.sp,
                                        fontFamily = ManropeFontFamily,
                                        color = Color(0xFF444841)
                                    )
                                }
                            }
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFF1EEE4),
                                modifier = Modifier.clickable {
                                    viewModel.onUserInteraction()
                                    viewModel.goForward()
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Next",
                                        fontSize = 11.sp,
                                        fontFamily = ManropeFontFamily,
                                        color = Color(0xFF444841)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "»",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF444841)
                                    )
                                }
                            }
                        }

                        // 4-Theme Quick Switcher (DEC-008, Stitch Palette)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ReaderThemeButton(
                                theme = ReaderTheme.LIGHT,
                                isActive = uiState.theme == ReaderTheme.LIGHT,
                                onClick = { viewModel.setTheme(ReaderTheme.LIGHT) }
                            )
                            ReaderThemeButton(
                                theme = ReaderTheme.SEPIA,
                                isActive = uiState.theme == ReaderTheme.SEPIA,
                                onClick = { viewModel.setTheme(ReaderTheme.SEPIA) }
                            )
                            ReaderThemeButton(
                                theme = ReaderTheme.DARK,
                                isActive = uiState.theme == ReaderTheme.DARK,
                                onClick = { viewModel.setTheme(ReaderTheme.DARK) }
                            )
                            ReaderThemeButton(
                                theme = ReaderTheme.AMOLED,
                                isActive = uiState.theme == ReaderTheme.AMOLED,
                                onClick = { viewModel.setTheme(ReaderTheme.AMOLED) }
                            )
                        }
                    }
                }
            }
        }

        // Status Delight Pill
        if (uiState.statusMessage != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 90.dp),
                shape = CircleShape,
                color = Color(0xFF31312A),
                shadowElevation = 6.dp
            ) {
                Text(
                    text = uiState.statusMessage ?: "",
                    color = Color(0xFFF4F1E7),
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = ManropeFontFamily,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }

    // Bookmarks Sheet
    if (uiState.activeSheet == ReaderSheet.BOOKMARKS) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewModel.closeSheet() },
            sheetState = sheetState,
            containerColor = BotanicalSurfaceContainerHigh
        ) {
            BookmarksSheetContent(
                bookmarks = uiState.bookmarks,
                onAddBookmark = { viewModel.toggleBookmark() },
                onSelectBookmark = { b -> viewModel.jumpToBookmark(b) },
                onDeleteBookmark = { id -> viewModel.deleteBookmark(id) },
                onClose = { viewModel.closeSheet() }
            )
        }
    }

    // Typography Sheet
    if (uiState.activeSheet == ReaderSheet.TYPOGRAPHY) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewModel.closeSheet() },
            sheetState = sheetState,
            containerColor = BotanicalSurfaceContainerHigh
        ) {
            TypographySheetContent(
                fontSizeMultiplier = uiState.fontSizeMultiplier,
                fontFamily = uiState.fontFamily,
                onFontSizeChange = { delta -> viewModel.setFontSize(uiState.fontSizeMultiplier + delta) },
                onFontFamilyChange = { family -> viewModel.setFontFamily(family) },
                onClose = { viewModel.closeSheet() }
            )
        }
    }
}

@Composable
fun ReaderThemeButton(
    theme: ReaderTheme,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isActive) BotanicalPrimary else Color(0xFFC4C8BE)
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(Color(theme.bgHex))
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = borderColor,
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isActive) {
            Text(
                text = "✓",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(theme.textHex)
            )
        }
    }
}

@Composable
fun BookmarksSheetContent(
    bookmarks: List<BookmarkEntity>,
    onAddBookmark: () -> Unit,
    onSelectBookmark: (BookmarkEntity) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    onClose: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Bookmarks & Notes",
                style = MaterialTheme.typography.titleMedium,
                fontFamily = LiterataFontFamily,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1C1C16)
            )
            TextButton(onClick = onAddBookmark) {
                Text("Bookmark Page", color = BotanicalPrimary, fontFamily = ManropeFontFamily, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (bookmarks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No bookmarks yet in this book.\nTap the ribbon or 'Bookmark Page' to save your place.",
                    textAlign = TextAlign.Center,
                    fontFamily = ManropeFontFamily,
                    color = Color(0xFF747870),
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(bookmarks, key = { it.id }) { bookmark ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectBookmark(bookmark) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1EEE4))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = bookmark.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontFamily = LiterataFontFamily,
                                    color = Color(0xFF1C1C16)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = dateFormat.format(Date(bookmark.createdAt)),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = ManropeFontFamily,
                                    color = Color(0xFF747870)
                                )
                            }
                            IconButton(onClick = { onDeleteBookmark(bookmark.id) }) {
                                Text(
                                    text = "✕",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF94452C)
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun TypographySheetContent(
    fontSizeMultiplier: Double,
    fontFamily: ReaderFontFamily,
    onFontSizeChange: (Double) -> Unit,
    onFontFamilyChange: (ReaderFontFamily) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Typography & Display",
            style = MaterialTheme.typography.titleMedium,
            fontFamily = LiterataFontFamily,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1C1C16)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Font Size Controls
        Text(
            text = "Font Size",
            style = MaterialTheme.typography.labelLarge,
            fontFamily = ManropeFontFamily,
            color = Color(0xFF444841)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFF1EEE4),
                modifier = Modifier.clickable { onFontSizeChange(-0.1) }
            ) {
                Text(
                    text = "A-",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    fontFamily = ManropeFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1C16)
                )
            }

            Text(
                text = "${(fontSizeMultiplier * 100).toInt()}%",
                style = MaterialTheme.typography.titleMedium,
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.Bold,
                color = BotanicalPrimary
            )

            Surface(
                shape = CircleShape,
                color = Color(0xFFF1EEE4),
                modifier = Modifier.clickable { onFontSizeChange(0.1) }
            ) {
                Text(
                    text = "A+",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    fontFamily = ManropeFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1C16)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Font Family Selector
        Text(
            text = "Font Family",
            style = MaterialTheme.typography.labelLarge,
            fontFamily = ManropeFontFamily,
            color = Color(0xFF444841)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReaderFontFamily.values().forEach { family ->
                val isSelected = family == fontFamily
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) BotanicalPrimary else Color(0xFFF1EEE4),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onFontFamilyChange(family) }
                ) {
                    Text(
                        text = family.displayName,
                        modifier = Modifier.padding(vertical = 10.dp),
                        textAlign = TextAlign.Center,
                        fontFamily = if (family == ReaderFontFamily.LITERATA) LiterataFontFamily else ManropeFontFamily,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else Color(0xFF1C1C16)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
