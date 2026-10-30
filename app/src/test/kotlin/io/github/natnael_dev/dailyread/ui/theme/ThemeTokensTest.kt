package io.github.natnael_dev.dailyread.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeTokensTest {

    @Test
    fun testLightPaletteValuesMatchDesignTokens() {
        assertEquals(Color(0xFF4D6148), BotanicalPrimary)
        assertEquals(Color(0xFFFCF9EF), BotanicalBackground)
        assertEquals(Color(0xFF1C1C16), BotanicalOnBackground)
        assertEquals(Color(0xFF655E4E), BotanicalSecondary)
        assertEquals(Color(0xFF94452C), BotanicalTertiary)
        assertEquals(Color(0xFFF7F4EA), BotanicalSurfaceContainerLow)
        assertEquals(Color(0xFFF1EEE4), BotanicalSurfaceContainer)
    }

    @Test
    fun testDarkPaletteValuesMatchDesignTokens() {
        assertEquals(Color(0xFF9AB093), BotanicalDarkPrimary)
        assertEquals(Color(0xFF1E231F), BotanicalDarkBackground)
        assertEquals(Color(0xFFEDE9DE), BotanicalDarkOnBackground)
        assertEquals(Color(0xFF292F2A), BotanicalDarkSurfaceVariant)
    }

    @Test
    fun testTypographyTokensMatchDesignScale() {
        assertEquals(48.sp, BotanicalTypography.displayLarge.fontSize)
        assertEquals(FontWeight.SemiBold, BotanicalTypography.displayLarge.fontWeight)
        assertEquals(26.sp, BotanicalTypography.headlineMedium.fontSize)
        assertEquals(20.sp, BotanicalTypography.titleLarge.fontSize)
        assertEquals(16.sp, BotanicalTypography.titleMedium.fontSize)
        assertEquals(15.sp, BotanicalTypography.bodyMedium.fontSize)
        assertEquals(14.sp, BotanicalTypography.labelLarge.fontSize)
    }

    @Test
    fun testShapeTokens() {
        assertEquals(4.dp, BotanicalShapes.extraSmall.topStart.toPx(androidx.compose.ui.geometry.Size(100f, 100f), androidx.compose.ui.unit.Density(1f)).toInt().dp)
        assertEquals(8.dp, BotanicalShapes.small.topStart.toPx(androidx.compose.ui.geometry.Size(100f, 100f), androidx.compose.ui.unit.Density(1f)).toInt().dp)
        assertEquals(12.dp, BotanicalShapes.medium.topStart.toPx(androidx.compose.ui.geometry.Size(100f, 100f), androidx.compose.ui.unit.Density(1f)).toInt().dp)
        assertEquals(16.dp, BotanicalShapes.large.topStart.toPx(androidx.compose.ui.geometry.Size(100f, 100f), androidx.compose.ui.unit.Density(1f)).toInt().dp)
        assertEquals(24.dp, BotanicalShapes.extraLarge.topStart.toPx(androidx.compose.ui.geometry.Size(100f, 100f), androidx.compose.ui.unit.Density(1f)).toInt().dp)
    }
}
