package io.github.natnael_dev.dailyread.ui.plans

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.natnael_dev.dailyread.domain.plan.ReadingPlanStatus
import io.github.natnael_dev.dailyread.ui.theme.BotanicalOutlineVariant
import io.github.natnael_dev.dailyread.ui.theme.BotanicalPrimary
import io.github.natnael_dev.dailyread.ui.theme.BotanicalPrimaryFixed
import io.github.natnael_dev.dailyread.ui.theme.BotanicalSecondary
import io.github.natnael_dev.dailyread.ui.theme.BotanicalSecondaryContainer
import io.github.natnael_dev.dailyread.ui.theme.BotanicalSurface
import io.github.natnael_dev.dailyread.ui.theme.BotanicalSurfaceContainer
import io.github.natnael_dev.dailyread.ui.theme.BotanicalSurfaceContainerHigh
import io.github.natnael_dev.dailyread.ui.theme.BotanicalSurfaceContainerLow
import io.github.natnael_dev.dailyread.ui.theme.BotanicalSurfaceContainerLowest
import io.github.natnael_dev.dailyread.ui.theme.BotanicalTertiary
import io.github.natnael_dev.dailyread.ui.theme.BotanicalTertiaryFixed
import io.github.natnael_dev.dailyread.ui.theme.BotanicalTypography

@Composable
fun PlansScreen(
    viewModel: PlansViewModel,
    onBookClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BotanicalSurface)
            .testTag("plans_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .padding(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Reading Plans",
                        style = BotanicalTypography.headlineMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Curate your reading pace with target dates",
                        style = BotanicalTypography.bodySmall,
                        color = BotanicalSecondary
                    )
                }

                Button(
                    onClick = { viewModel.openCreateDialog() },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = BotanicalPrimary),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("create_plan_button")
                ) {
                    Text("+ New Plan", style = BotanicalTypography.labelMedium)
                }
            }

            // Plans List
            if (state.activePlans.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = BotanicalSurfaceContainer),
                    border = BorderStroke(1.dp, BotanicalOutlineVariant.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("🧭", fontSize = 40.sp)
                        Text(
                            text = "No Active Reading Plans",
                            style = BotanicalTypography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Set a target date for any book in your library. DailyRead will calculate a quiet, mindful daily page quota to help you finish.",
                            style = BotanicalTypography.bodySmall,
                            color = BotanicalSecondary,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { viewModel.openCreateDialog() },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = BotanicalPrimary)
                        ) {
                            Text("Create a Plan", style = BotanicalTypography.labelMedium)
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.activePlans) { planStatus ->
                        val book = state.availableBooks.firstOrNull { it.id == planStatus.plan.bookId }
                        PlanItemCard(
                            planStatus = planStatus,
                            bookTitle = book?.title ?: "Book #${planStatus.plan.bookId}",
                            bookAuthor = book?.author ?: "Unknown Author",
                            onCardClick = { onBookClick(planStatus.plan.bookId) },
                            onDelete = { viewModel.deletePlan(planStatus.plan.id) }
                        )
                    }
                }
            }
        }

        // Create Plan Dialog
        if (state.isCreateDialogOpen) {
            CreatePlanDialog(
                state = state,
                onSelectBook = { bookId -> viewModel.selectBook(bookId) },
                onSelectDays = { days -> viewModel.setTargetDays(days) },
                onConfirm = { bookId, targetDay -> viewModel.createPlan(bookId, targetDay) },
                onDismiss = { viewModel.closeCreateDialog() }
            )
        }
    }
}

@Composable
fun PlanItemCard(
    planStatus: ReadingPlanStatus,
    bookTitle: String,
    bookAuthor: String,
    onCardClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BotanicalSurfaceContainer),
        border = BorderStroke(1.dp, BotanicalOutlineVariant.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bookTitle,
                        style = BotanicalTypography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = bookAuthor,
                        style = BotanicalTypography.bodySmall,
                        color = BotanicalSecondary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (planStatus.isOnTrack) BotanicalPrimaryFixed else BotanicalTertiaryFixed
                    ) {
                        Text(
                            text = if (planStatus.isOnTrack) "On Track" else "Behind",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = BotanicalTypography.labelSmall,
                            color = if (planStatus.isOnTrack) BotanicalPrimary else BotanicalTertiary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "🗑️",
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clickable(onClick = onDelete)
                            .padding(4.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = BotanicalSurfaceContainerLow,
                border = BorderStroke(1.dp, BotanicalOutlineVariant.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Daily Quota",
                            style = BotanicalTypography.labelSmall,
                            color = BotanicalSecondary
                        )
                        Text(
                            text = "${planStatus.dailyQuota} pages/day",
                            style = BotanicalTypography.titleSmall,
                            color = BotanicalPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Finish Target",
                            style = BotanicalTypography.labelSmall,
                            color = BotanicalSecondary
                        )
                        Text(
                            text = "${planStatus.plan.targetDay} (${planStatus.remainingDays}d left)",
                            style = BotanicalTypography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${planStatus.actualUnits} of ${planStatus.plan.totalUnits} pages",
                        style = BotanicalTypography.labelSmall
                    )
                    Text(
                        text = "${planStatus.progressPercent}%",
                        style = BotanicalTypography.labelSmall,
                        color = BotanicalPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                LinearProgressIndicator(
                    progress = { (planStatus.progressPercent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = BotanicalPrimary,
                    trackColor = BotanicalSurfaceContainerHigh
                )
            }
        }
    }
}

@Composable
fun CreatePlanDialog(
    state: PlansUiState,
    onSelectBook: (Long) -> Unit,
    onSelectDays: (Int) -> Unit,
    onConfirm: (Long, io.github.natnael_dev.dailyread.domain.time.LogicalDay) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedBookId by remember { mutableStateOf(state.selectedBookId ?: state.availableBooks.firstOrNull()?.id ?: 0L) }
    var selectedDays by remember { mutableIntStateOf(state.targetDaysOut) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Create Reading Plan", style = BotanicalTypography.titleLarge, fontWeight = FontWeight.SemiBold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Select a book from your library:",
                    style = BotanicalTypography.labelMedium,
                    color = BotanicalSecondary
                )

                if (state.availableBooks.isEmpty()) {
                    Text(
                        text = "No books in library. Please import a book first.",
                        style = BotanicalTypography.bodySmall,
                        color = BotanicalTertiary
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.availableBooks.take(4).forEach { book ->
                            val isSelected = book.id == selectedBookId
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) BotanicalSecondaryContainer else BotanicalSurfaceContainerLow,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) BotanicalPrimary else BotanicalOutlineVariant.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedBookId = book.id
                                        onSelectBook(book.id)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = book.title,
                                            style = BotanicalTypography.bodySmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${book.totalUnits ?: 250} pages",
                                            style = BotanicalTypography.labelSmall,
                                            color = BotanicalSecondary
                                        )
                                    }
                                    if (isSelected) {
                                        Text("✓", color = BotanicalPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "Target finish duration:",
                    style = BotanicalTypography.labelMedium,
                    color = BotanicalSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(7, 14, 30, 60).forEach { days ->
                        val isSelected = selectedDays == days
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) BotanicalPrimary else BotanicalSurfaceContainerLow)
                                .border(1.dp, if (isSelected) BotanicalPrimary else BotanicalOutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedDays = days
                                    onSelectDays(days)
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${days}d",
                                style = BotanicalTypography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else BotanicalTypography.bodySmall.color
                            )
                        }
                    }
                }

                if (state.calculatedDailyQuota != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BotanicalPrimaryFixed.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, BotanicalPrimary.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "Estimated daily quota: ${state.calculatedDailyQuota} pages / day",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            style = BotanicalTypography.labelSmall,
                            color = BotanicalPrimary,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    state.targetFinishDate?.let { target ->
                        if (selectedBookId > 0) {
                            onConfirm(selectedBookId, target)
                        }
                    }
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = BotanicalPrimary),
                enabled = selectedBookId > 0 && state.targetFinishDate != null
            ) {
                Text("Start Plan", style = BotanicalTypography.labelMedium)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = CircleShape
            ) {
                Text("Cancel", style = BotanicalTypography.labelMedium)
            }
        },
        containerColor = BotanicalSurfaceContainerLowest
    )
}
