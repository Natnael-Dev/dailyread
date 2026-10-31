package io.github.natnael_dev.dailyread.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    private fun calculateLuminance(color: Color): Double {
        fun channelLuminance(value: Float): Double {
            val d = value.toDouble()
            return if (d <= 0.04045) d / 12.92 else Math.pow((d + 0.055) / 1.055, 2.4)
        }
        val r = channelLuminance(color.red)
        val g = channelLuminance(color.green)
        val b = channelLuminance(color.blue)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun calculateContrastRatio(c1: Color, c2: Color): Double {
        val l1 = calculateLuminance(c1)
        val l2 = calculateLuminance(c2)
        val lighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    @Test
    fun testWarmBotanicalPaletteMeetsWcagAaContrastRatios() {
        // 1. Body Text on Light Background: >= 4.5:1
        val bodyContrast = calculateContrastRatio(BotanicalOnBackground, BotanicalBackground)
        assertTrue("Body text contrast ($bodyContrast) must meet WCAG AA >= 4.5:1", bodyContrast >= 4.5)

        // 2. Primary Sage Green on Background: >= 4.5:1
        val primaryContrast = calculateContrastRatio(BotanicalPrimary, BotanicalBackground)
        assertTrue("Primary contrast ($primaryContrast) must meet WCAG AA >= 4.5:1", primaryContrast >= 4.5)

        // 3. OnPrimary on Primary Sage: >= 4.5:1
        val onPrimaryContrast = calculateContrastRatio(BotanicalOnPrimary, BotanicalPrimary)
        assertTrue("OnPrimary contrast ($onPrimaryContrast) must meet WCAG AA >= 4.5:1", onPrimaryContrast >= 4.5)

        // 4. Secondary Paper Umber on Background: >= 4.5:1
        val secondaryContrast = calculateContrastRatio(BotanicalSecondary, BotanicalBackground)
        assertTrue("Secondary contrast ($secondaryContrast) must meet WCAG AA >= 4.5:1", secondaryContrast >= 4.5)

        // 5. Tertiary Terracotta on Background: >= 4.5:1
        val tertiaryContrast = calculateContrastRatio(BotanicalTertiary, BotanicalBackground)
        assertTrue("Tertiary contrast ($tertiaryContrast) must meet WCAG AA >= 4.5:1", tertiaryContrast >= 4.5)

        // 6. Error Red on Background: >= 4.5:1
        val errorContrast = calculateContrastRatio(BotanicalError, BotanicalBackground)
        assertTrue("Error contrast ($errorContrast) must meet WCAG AA >= 4.5:1", errorContrast >= 4.5)

        // 7. Dark Palette Body Text: >= 4.5:1
        val darkBodyContrast = calculateContrastRatio(BotanicalDarkOnBackground, BotanicalDarkBackground)
        assertTrue("Dark body contrast ($darkBodyContrast) must meet WCAG AA >= 4.5:1", darkBodyContrast >= 4.5)

        // 8. Dark Primary Sage on Dark Background: >= 4.5:1
        val darkPrimaryContrast = calculateContrastRatio(BotanicalDarkPrimary, BotanicalDarkBackground)
        assertTrue("Dark primary contrast ($darkPrimaryContrast) must meet WCAG AA >= 4.5:1", darkPrimaryContrast >= 4.5)
    }
}
