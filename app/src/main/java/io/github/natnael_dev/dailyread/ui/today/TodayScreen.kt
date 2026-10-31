package io.github.natnael_dev.dailyread.ui.today

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.natnael_dev.dailyread.domain.streak.DayStatus
import io.github.natnael_dev.dailyread.ui.theme.BotanicalError
import io.github.natnael_dev.dailyread.ui.theme.BotanicalOutlineVariant
import io.github.natnael_dev.dailyread.ui.theme.BotanicalPrimary
import io.github.natnael_dev.dailyread.ui.theme.BotanicalPrimaryFixed
import io.github.natnael_dev.dailyread.ui.theme.BotanicalSecondary
import io.github.natnael_dev.dailyread.ui.theme.BotanicalSecondaryContainer
import io.github.natnael_dev.dailyread.ui.theme.BotanicalSecondaryFixed
import io.github.natnael_dev.dailyread.ui.theme.BotanicalSurface
import io.github.natnael_dev.dailyread.ui.theme.BotanicalSurfaceContainer
import io.github.natnael_dev.dailyread.ui.theme.BotanicalSurfaceContainerHigh
import io.github.natnael_dev.dailyread.ui.theme.BotanicalSurfaceContainerHighest
import io.github.natnael_dev.dailyread.ui.theme.BotanicalSurfaceContainerLow
import io.github.natnael_dev.dailyread.ui.theme.BotanicalSurfaceContainerLowest
import io.github.natnael_dev.dailyread.ui.theme.BotanicalSurfaceVariant
import io.github.natnael_dev.dailyread.ui.theme.BotanicalTertiary
import io.github.natnael_dev.dailyread.ui.theme.BotanicalTertiaryFixed
import io.github.natnael_dev.dailyread.ui.theme.BotanicalTypography
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun TodayScreen(
    viewModel: TodayViewModel,
    onContinueReading: (Long) -> Unit,
    onNavigateToPlans: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    LaunchedEffect(state.toastMessage) {
        if (state.toastMessage != null) {
            delay(2500)
            viewModel.clearToast()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BotanicalSurface)
            .testTag("today_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .padding(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top App Bar
            TopEditorialHeader()

            // Greeting Section
            WarmEditorialGreeting(
                greeting = state.greeting,
                userName = state.userName,
                formattedDate = state.formattedDate
            )

            // Prominent Celebrated Streak Card (Hero)
            StreakHeroCard(state = state)

            // Daily Progress Ring Card
            DailyProgressCard(
                state = state,
                onQuickAdd = { viewModel.quickAddPages(5) }
            )

            // Active Reading Plan Banner
            ActivePlanBanner(
                state = state,
                onPlanClick = onNavigateToPlans
            )

            // Currently Reading Card (Continue Reading)
            CurrentlyReadingCard(
                state = state,
                onContinueReading = { bookId -> onContinueReading(bookId) },
                onExploreLibrary = onNavigateToLibrary
            )

            // Modern 30-Day Contribution Grid / Activity Matrix
            ActivityMatrixCard(
                state = state,
                onSelectDay = { day -> viewModel.selectDay(day) }
            )

            // Daily Reflection Note Component
            DailyReflectionCard()
        }
    }
}

@Composable
fun TopEditorialHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("📖", fontSize = 24.sp)
            Text(
                text = "DailyRead",
                style = BotanicalTypography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = BotanicalPrimary
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = BotanicalSurfaceContainer,
                border = BorderStroke(1.dp, BotanicalOutlineVariant.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("🌿", fontSize = 12.sp)
                    Text(
                        text = "Synced",
                        style = BotanicalTypography.labelSmall,
                        color = BotanicalPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

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

@Composable
fun WarmEditorialGreeting(
    greeting: String,
    userName: String,
    formattedDate: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = formattedDate.uppercase(),
                style = BotanicalTypography.labelMedium,
                color = BotanicalSecondary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$greeting, $userName",
                style = BotanicalTypography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = BotanicalTypography.headlineMedium.color
            )
        }

        Surface(
            shape = CircleShape,
            color = BotanicalSurfaceContainer,
            border = BorderStroke(1.dp, BotanicalOutlineVariant.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("🌸", fontSize = 13.sp)
                Text(
                    text = "Mindful pace",
                    style = BotanicalTypography.labelSmall,
                    color = BotanicalPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun StreakHeroCard(state: TodayUiState) {
    val a11yStreak = "Current streak: ${state.currentStreak} ${if (state.currentStreak == 1) "day" else "days"}. Longest streak: ${state.bestStreak} days. Freezes available: ${state.freezesHeld} of 2."

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("streak_card")
            .semantics(mergeDescendants = true) {
                contentDescription = a11yStreak
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, BotanicalOutlineVariant.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            BotanicalSurfaceContainerHigh,
                            BotanicalSurfaceContainer,
                            BotanicalSecondaryContainer.copy(alpha = 0.4f)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Fire icon bubble
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(BotanicalTertiaryFixed, CircleShape)
                                .border(2.dp, BotanicalTertiary.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🔥", fontSize = 24.sp)
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${state.currentStreak}-Day Streak",
                                    style = BotanicalTypography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )

                                if (state.isPersonalBest) {
                                    Surface(
                                        shape = CircleShape,
                                        color = BotanicalPrimary.copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, BotanicalPrimary.copy(alpha = 0.25f))
                                    ) {
                                        Text(
                                            text = "Personal Best",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            style = BotanicalTypography.labelSmall,
                                            color = BotanicalPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            Text(
                                text = state.streakSubtitle,
                                style = BotanicalTypography.bodySmall,
                                color = BotanicalSecondary
                            )
                        }
                    }

                    // Freeze badge
                    Surface(
                        shape = CircleShape,
                        color = BotanicalSurfaceContainerLowest,
                        border = BorderStroke(1.dp, BotanicalOutlineVariant.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("❄️", fontSize = 12.sp)
                            Text(
                                text = state.freezeBadgeText,
                                style = BotanicalTypography.labelSmall,
                                color = BotanicalSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DailyProgressCard(
    state: TodayUiState,
    onQuickAdd: () -> Unit
) {
    val a11yProgress = "Today's reading progress: ${state.pagesReadToday} of ${state.goalPages} pages read. ${state.progressPercent} percent of daily goal completed."

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = a11yProgress
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BotanicalSurfaceContainer),
        border = BorderStroke(1.dp, BotanicalOutlineVariant.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Today's Practice",
                        style = BotanicalTypography.labelMedium,
                        color = BotanicalSecondary
                    )
                    Text(
                        text = "Daily Page Ritual",
                        style = BotanicalTypography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = BotanicalPrimaryFixed
                ) {
                    Text(
                        text = "${state.goalPages} pages goal",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = BotanicalTypography.labelSmall,
                        color = BotanicalPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = BotanicalSurfaceContainerLow,
                border = BorderStroke(1.dp, BotanicalOutlineVariant.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Custom SVG / Canvas Progress Ring
                    val animatedSweep by animateFloatAsState(
                        targetValue = (state.progressPercent / 100f).coerceIn(0f, 1f),
                        animationSpec = tween(durationMillis = 700),
                        label = "progress_sweep"
                    )

                    Box(
                        modifier = Modifier.size(76.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 7.dp.toPx()
                            // Background ring
                            drawCircle(
                                color = BotanicalSurfaceVariant,
                                radius = size.minDimension / 2 - strokeWidth / 2,
                                style = Stroke(width = strokeWidth)
                            )
                            // Progress arc
                            drawArc(
                                color = BotanicalPrimary,
                                startAngle = -90f,
                                sweepAngle = animatedSweep * 360f,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${state.progressPercent}%",
                                style = BotanicalTypography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "goal",
                                style = BotanicalTypography.labelSmall,
                                color = BotanicalSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Progress Details
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${state.pagesReadToday} ",
                                style = BotanicalTypography.headlineSmall,
                                color = BotanicalPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "of ${state.goalPages} pages read today",
                                style = BotanicalTypography.bodyMedium,
                                color = BotanicalSecondary
                            )
                        }

                        Text(
                            text = if (state.remainingPages > 0) "${state.remainingPages} quiet pages remaining" else "Ritual goal achieved! 🌿",
                            style = BotanicalTypography.bodySmall,
                            color = if (state.remainingPages == 0) BotanicalPrimary else BotanicalSecondary
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onQuickAdd,
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BotanicalPrimary,
                                    contentColor = Color.White
                                ),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("quick_add_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text("+5 pages", style = BotanicalTypography.labelMedium)
                                }
                            }

                            AnimatedVisibility(
                                visible = state.toastMessage != null,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Text(
                                    text = state.toastMessage ?: "",
                                    style = BotanicalTypography.labelSmall,
                                    color = BotanicalPrimary,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActivePlanBanner(
    state: TodayUiState,
    onPlanClick: () -> Unit
) {
    val activePlan = state.activePlanStatus

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = BotanicalSecondaryContainer,
        border = BorderStroke(1.dp, BotanicalOutlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlanClick)
            .testTag("active_plan_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(BotanicalSurfaceContainerLowest, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🧭", fontSize = 18.sp)
                }

                Column {
                    Text(
                        text = if (activePlan != null) "Curated Reading Plan" else "Reading Plans",
                        style = BotanicalTypography.labelSmall,
                        color = BotanicalSecondary,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )

                    if (activePlan != null) {
                        Text(
                            text = "Finish by ${activePlan.plan.targetDay} (${activePlan.dailyQuota} pages/day)",
                            style = BotanicalTypography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = "Tap to create a structured reading goal",
                            style = BotanicalTypography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (activePlan != null) {
                Surface(
                    shape = CircleShape,
                    color = if (activePlan.isOnTrack) BotanicalPrimaryFixed else BotanicalTertiaryFixed
                ) {
                    Text(
                        text = if (activePlan.isOnTrack) "On Track" else "Behind",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = BotanicalTypography.labelSmall,
                        color = if (activePlan.isOnTrack) BotanicalPrimary else BotanicalTertiary,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Text("→", fontSize = 18.sp, color = BotanicalSecondary)
            }
        }
    }
}

@Composable
fun CurrentlyReadingCard(
    state: TodayUiState,
    onContinueReading: (Long) -> Unit,
    onExploreLibrary: () -> Unit
) {
    val book = state.lastReadBook

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Currently Reading",
                style = BotanicalTypography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (book != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("📖", fontSize = 12.sp)
                    Text(
                        text = "Local ${book.format}",
                        style = BotanicalTypography.labelSmall,
                        color = BotanicalPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BotanicalSurfaceContainer),
            border = BorderStroke(1.dp, BotanicalOutlineVariant.copy(alpha = 0.25f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            if (book != null) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Cover thumbnail
                        val coverFile = book.coverPath?.let { File(it) }
                        val bitmap = remember(book.coverPath) {
                            if (coverFile != null && coverFile.exists()) {
                                BitmapFactory.decodeFile(coverFile.absolutePath)?.asImageBitmap()
                            } else null
                        }

                        Box(
                            modifier = Modifier
                                .size(width = 80.dp, height = 110.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BotanicalSurfaceContainerHighest),
                            contentAlignment = Alignment.Center
                        ) {
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = book.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text("📖", fontSize = 32.sp)
                            }
                        }

                        // Book Details
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .height(110.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = book.title,
                                    style = BotanicalTypography.headlineSmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = book.author ?: "Unknown Author",
                                    style = BotanicalTypography.bodySmall,
                                    color = BotanicalSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Page ${state.lastReadCurrentPage} of ${state.lastReadTotalPages}",
                                        style = BotanicalTypography.labelSmall
                                    )
                                    Text(
                                        text = "${state.lastReadProgressPercent}%",
                                        style = BotanicalTypography.labelSmall,
                                        color = BotanicalPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                LinearProgressIndicator(
                                    progress = { (state.lastReadProgressPercent / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape),
                                    color = BotanicalPrimary,
                                    trackColor = BotanicalSurfaceContainerHigh,
                                )
                            }
                        }
                    }

                    // Continue reading button
                    Button(
                        onClick = { onContinueReading(book.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("continue_reading_button"),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BotanicalPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("🔖", fontSize = 16.sp)
                            Text(
                                text = "Continue Reading",
                                style = BotanicalTypography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            } else {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("📚", fontSize = 36.sp)
                    Text(
                        text = "No books in reading queue",
                        style = BotanicalTypography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Open a book from your library to start your daily reading journey.",
                        style = BotanicalTypography.bodySmall,
                        color = BotanicalSecondary,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = onExploreLibrary,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = BotanicalPrimary)
                    ) {
                        Text("Explore Library", style = BotanicalTypography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityMatrixCard(
    state: TodayUiState,
    onSelectDay: (ActivityDayUiModel) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BotanicalSurfaceContainer),
        border = BorderStroke(1.dp, BotanicalOutlineVariant.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("▦", fontSize = 18.sp, color = BotanicalPrimary)
                    Text(
                        text = "30-Day Activity Matrix",
                        style = BotanicalTypography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = state.completionRateText,
                    style = BotanicalTypography.labelSmall,
                    color = BotanicalSecondary
                )
            }

            // Selected Day Summary Banner
            val selected = state.selectedDaySummary
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = BotanicalSurfaceContainerLow,
                border = BorderStroke(1.dp, BotanicalOutlineVariant.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(BotanicalPrimary, CircleShape)
                        )
                        Text(
                            text = if (selected != null) {
                                "${selected.logicalDay} ${if (selected.isToday) "(Today)" else ""}"
                            } else "Today",
                            style = BotanicalTypography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = if (selected != null) {
                            "${selected.pagesCounted} pages read • ${if (selected.status == DayStatus.FROZEN) "Frozen" else if (selected.pagesCounted >= state.goalPages) "Goal met" else "In progress"}"
                        } else "Select a day to view details",
                        style = BotanicalTypography.bodySmall,
                        color = BotanicalPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 7 Weekday Headers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach { initial ->
                    Text(
                        text = initial,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = BotanicalTypography.labelSmall,
                        color = BotanicalSecondary.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            }

            // 30 Days Grid in chunks of 7
            val matrix = state.activityMatrix
            val rows = matrix.chunked(7)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                rows.forEach { rowDays ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rowDays.forEach { dayModel ->
                            val cellColor = when (dayModel.level) {
                                4 -> BotanicalSecondaryFixed
                                3 -> BotanicalPrimary
                                2 -> BotanicalPrimary.copy(alpha = 0.6f)
                                1 -> BotanicalPrimary.copy(alpha = 0.3f)
                                else -> BotanicalSurfaceContainerLow
                            }

                            val textColor = when (dayModel.level) {
                                3 -> Color.White
                                2 -> Color.White
                                else -> BotanicalTypography.bodySmall.color
                            }

                            val a11yDayDescription = buildString {
                                append(dayModel.logicalDay.date.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d")))
                                if (dayModel.isToday) append(", Today")
                                when (dayModel.status) {
                                    DayStatus.QUALIFIED, DayStatus.TODAY_QUALIFIED -> append(": Goal met, ${dayModel.pagesCounted} pages read")
                                    DayStatus.FROZEN -> append(": Streak frozen")
                                    DayStatus.TODAY_IN_PROGRESS -> append(": In progress, ${dayModel.pagesCounted} pages read")
                                    DayStatus.MISSED -> append(": Goal missed, ${dayModel.pagesCounted} pages read")
                                    DayStatus.FUTURE_BLANK -> append(": Upcoming day")
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(28.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(cellColor)
                                    .then(
                                        if (dayModel.isToday) {
                                            Modifier.border(2.dp, BotanicalPrimary, RoundedCornerShape(4.dp))
                                        } else {
                                            Modifier.border(1.dp, BotanicalOutlineVariant.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        }
                                    )
                                    .semantics {
                                        contentDescription = a11yDayDescription
                                        role = Role.Button
                                    }
                                    .clickable { onSelectDay(dayModel) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (dayModel.status == DayStatus.FROZEN) {
                                    Text("❄️", fontSize = 10.sp)
                                } else {
                                    Text(
                                        text = "${dayModel.dayOfMonth}",
                                        fontSize = 10.sp,
                                        fontWeight = if (dayModel.isToday || dayModel.level >= 2) FontWeight.Bold else FontWeight.Medium,
                                        color = textColor
                                    )
                                }
                            }
                        }

                        // Fill trailing spaces if row has fewer than 7 days
                        if (rowDays.size < 7) {
                            repeat(7 - rowDays.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Botanical Legend
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(color = BotanicalSurfaceContainerLow, label = "None")
                LegendItem(color = BotanicalPrimary.copy(alpha = 0.3f), label = "1–5p")
                LegendItem(color = BotanicalPrimary.copy(alpha = 0.6f), label = "6–15p")
                LegendItem(color = BotanicalPrimary, label = "Goal")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(BotanicalSecondaryFixed, RoundedCornerShape(2.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("❄", fontSize = 8.sp)
                    }
                    Text(text = "Freeze", style = BotanicalTypography.labelSmall, fontSize = 11.sp, color = BotanicalSecondary)
                }
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(2.dp))
                .border(1.dp, BotanicalOutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
        )
        Text(text = label, style = BotanicalTypography.labelSmall, fontSize = 11.sp, color = BotanicalSecondary)
    }
}

@Composable
fun DailyReflectionCard() {
    var noteText by remember { mutableStateOf("\"To read deeply is to find an inner forest where each sentence allows room for moss to grow.\"") }
    var savedNotice by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BotanicalSurfaceContainerLow),
        border = BorderStroke(1.dp, BotanicalOutlineVariant.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("✍️", fontSize = 18.sp)
                    Column {
                        Text(
                            text = "Today's reflection",
                            style = BotanicalTypography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "What stood out in today's reading?",
                            style = BotanicalTypography.labelSmall,
                            color = BotanicalSecondary
                        )
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = BotanicalSurfaceContainer,
                    border = BorderStroke(1.dp, BotanicalOutlineVariant.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "Reader note",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = BotanicalTypography.labelSmall,
                        color = BotanicalSecondary
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = BotanicalSurfaceContainerLowest,
                border = BorderStroke(1.dp, BotanicalOutlineVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        placeholder = { Text("A thought, memorable line, or question from your reading...") }
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (savedNotice) "Saved to local journal ✓" else "Marginal note • ${noteText.length} chars",
                            style = BotanicalTypography.labelSmall,
                            color = if (savedNotice) BotanicalPrimary else BotanicalSecondary.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )

                        Button(
                            onClick = { savedNotice = true },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = BotanicalPrimary),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Save Note", style = BotanicalTypography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
