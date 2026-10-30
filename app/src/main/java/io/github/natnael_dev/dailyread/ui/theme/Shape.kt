package io.github.natnael_dev.dailyread.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val BotanicalShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp), // tags, badges
    small = RoundedCornerShape(8.dp),      // chips, inputs
    medium = RoundedCornerShape(12.dp),    // book cards, buttons
    large = RoundedCornerShape(16.dp),     // content containers, dialogs
    extraLarge = RoundedCornerShape(24.dp) // bottom sheets, persistent dock
)
