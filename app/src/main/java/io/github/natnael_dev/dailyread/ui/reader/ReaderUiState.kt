package io.github.natnael_dev.dailyread.ui.reader

import io.github.natnael_dev.dailyread.data.db.entity.BookEntity
import io.github.natnael_dev.dailyread.data.db.entity.BookmarkEntity

enum class ReaderTheme(val displayName: String, val bgHex: Long, val textHex: Long) {
    LIGHT("Light", 0xFFFCF9EF, 0xFF1C1C16),
    SEPIA("Sepia", 0xFFEADECA, 0xFF1C1C16),
    DARK("Dark", 0xFF1E231F, 0xFFFCF9EF),
    AMOLED("AMOLED", 0xFF121212, 0xFFECE1CD)
}

enum class ReaderFontFamily(val displayName: String, val cssName: String) {
    SYSTEM("System Default", "sans-serif"),
    LITERATA("Literata Serif", "Literata"),
    MANROPE("Manrope Sans", "Manrope")
}

enum class ReaderSheet {
    NONE,
    BOOKMARKS,
    TYPOGRAPHY
}

enum class ReaderFormat {
    EPUB,
    PDF
}

data class ReaderUiState(
    val book: BookEntity? = null,
    val format: ReaderFormat = ReaderFormat.EPUB,
    val title: String = "",
    val chapterTitle: String = "",
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val progression: Double = 0.0,
    val theme: ReaderTheme = ReaderTheme.SEPIA,
    val fontSizeMultiplier: Double = 1.0,
    val fontFamily: ReaderFontFamily = ReaderFontFamily.LITERATA,
    val isBookmarked: Boolean = false,
    val bookmarks: List<BookmarkEntity> = emptyList(),
    val isControlsVisible: Boolean = true,
    val activeSheet: ReaderSheet = ReaderSheet.NONE,
    val statusMessage: String? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val currentLocatorJson: String? = null
)
