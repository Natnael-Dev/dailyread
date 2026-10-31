package io.github.natnael_dev.dailyread.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import io.github.natnael_dev.dailyread.MainActivity
import io.github.natnael_dev.dailyread.data.db.DailyReadDatabase

class DailyReadGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = DailyReadDatabase.getInstance(context)
        // State Derivation: derive from Room at render time
        val state = WidgetStateDeriver.deriveState(database)

        provideContent {
            GlanceTheme {
                DailyReadWidgetContent(context = context, state = state)
            }
        }
    }
}

@androidx.compose.runtime.Composable
fun DailyReadWidgetContent(
    context: Context,
    state: DailyReadWidgetState
) {
    // Warm botanical theme tokens (Color.kt)
    val colorBackground = Color(0xFFFCF9EF)      // BotanicalBackground (Warm Ivory Canvas)
    val colorCardSurface = Color(0xFFF1EEE4)     // BotanicalSurfaceContainer
    val colorPrimary = Color(0xFF4D6148)         // BotanicalPrimary (Sage Green)
    val colorOnPrimary = Color(0xFFFFFFFF)       // BotanicalOnPrimary
    val colorOnSurface = Color(0xFF1C1C16)       // BotanicalOnSurface (Deep Charcoal)
    val colorMuted = Color(0xFF655E4E)           // BotanicalSecondary (Muted Warm Wood)
    val colorTerracotta = Color(0xFF94452C)      // BotanicalTertiary (Terracotta Accent)

    // Deep link intent: opens MainActivity and immediately opens active book if available
    val deepLinkIntent = Intent(context, MainActivity::class.java).apply {
        action = Intent.ACTION_VIEW
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        if (state.lastBookId != null) {
            putExtra("bookId", state.lastBookId)
        }
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(colorBackground))
            .cornerRadius(16.dp)
            .padding(12.dp)
            .clickable(actionStartActivity(deepLinkIntent))
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            // Header Row: Streak + Today Progress Badge
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Text(
                    text = "🔥 ${state.streak}-day streak",
                    style = TextStyle(
                        color = ColorProvider(colorTerracotta),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )

                val progressText = if (state.todayQualified) {
                    "✓ Today done (${state.pagesReadToday} pgs)"
                } else {
                    "${state.pagesReadToday} / ${state.dailyGoal} pgs"
                }

                Text(
                    text = progressText,
                    style = TextStyle(
                        color = ColorProvider(if (state.todayQualified) colorPrimary else colorMuted),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Book info card
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(ColorProvider(colorCardSurface))
                    .cornerRadius(10.dp)
                    .padding(8.dp)
            ) {
                Text(
                    text = state.lastBookTitle,
                    style = TextStyle(
                        color = ColorProvider(colorOnSurface),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1
                )
                Spacer(modifier = GlanceModifier.height(2.dp))
                Text(
                    text = "${state.lastBookProgression}% completed",
                    style = TextStyle(
                        color = ColorProvider(colorMuted),
                        fontSize = 12.sp
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Action Button: Continue Reading
            Button(
                text = "Continue Reading",
                onClick = actionStartActivity(deepLinkIntent),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = ColorProvider(colorPrimary),
                    contentColor = ColorProvider(colorOnPrimary)
                ),
                modifier = GlanceModifier.fillMaxWidth().height(36.dp)
            )
        }
    }
}
