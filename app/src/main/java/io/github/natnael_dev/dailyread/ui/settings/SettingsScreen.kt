package io.github.natnael_dev.dailyread.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.natnael_dev.dailyread.ui.theme.BotanicalOnPrimary
import io.github.natnael_dev.dailyread.ui.theme.BotanicalOnPrimaryFixed
import io.github.natnael_dev.dailyread.ui.theme.BotanicalOutlineVariant
import io.github.natnael_dev.dailyread.ui.theme.BotanicalPrimary
import io.github.natnael_dev.dailyread.ui.theme.BotanicalPrimaryFixed
import io.github.natnael_dev.dailyread.ui.theme.BotanicalSecondary
import io.github.natnael_dev.dailyread.ui.theme.BotanicalSurfaceContainerHigh
import io.github.natnael_dev.dailyread.ui.theme.BotanicalSurfaceContainerLow
import io.github.natnael_dev.dailyread.ui.theme.BotanicalSurfaceContainerLowest
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onRelinkFolder: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // SAF Document Launchers
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                viewModel.exportBackup(outputStream)
            }
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                viewModel.restoreBackup(inputStream)
            }
        }
    }

    val openTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            viewModel.dismissRelinkPrompt()
            onRelinkFolder(treeUri)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Screen Header
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Preferences, appearance, and strict offline backup",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Notification / Status Banner
            item {
                if (uiState.exportSuccessMessage != null) {
                    StatusBanner(
                        message = uiState.exportSuccessMessage!!,
                        isSuccess = true,
                        onDismiss = { viewModel.clearMessages() }
                    )
                } else if (uiState.errorMessage != null) {
                    StatusBanner(
                        message = uiState.errorMessage!!,
                        isSuccess = false,
                        onDismiss = { viewModel.clearMessages() }
                    )
                }
            }

            // Section 1: Appearance
            item {
                SettingsSectionCard(title = "Appearance", icon = "🎨") {
                    Text(
                        text = "Choose your preferred reading theme",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ThemeOptionTile(
                            title = "System",
                            subtitle = "Auto",
                            selected = uiState.themeMode == "SYSTEM",
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.setAppTheme("SYSTEM") }
                        )
                        ThemeOptionTile(
                            title = "Light",
                            subtitle = "Ivory",
                            selected = uiState.themeMode == "LIGHT",
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.setAppTheme("LIGHT") }
                        )
                        ThemeOptionTile(
                            title = "Dark",
                            subtitle = "Midnight",
                            selected = uiState.themeMode == "DARK",
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.setAppTheme("DARK") }
                        )
                    }
                }
            }

            // Section 2: Daily Reading Goal
            item {
                SettingsSectionCard(title = "Daily Reading Goal", icon = "🎯") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${uiState.dailyGoalPages} pages",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Daily target for streak qualification",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Stepper buttons
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        if (uiState.dailyGoalPages > 1) {
                                            viewModel.setDailyGoal(uiState.dailyGoalPages - 1)
                                        }
                                    },
                                shape = CircleShape,
                                color = BotanicalSurfaceContainerHigh
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "−",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        if (uiState.dailyGoalPages < 100) {
                                            viewModel.setDailyGoal(uiState.dailyGoalPages + 1)
                                        }
                                    },
                                shape = CircleShape,
                                color = BotanicalPrimary
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "+",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BotanicalOnPrimary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Preset chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5, 10, 15, 20, 30).forEach { pages ->
                            val isSelected = uiState.dailyGoalPages == pages
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { viewModel.setDailyGoal(pages) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) BotanicalPrimaryFixed else BotanicalSurfaceContainerHigh
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$pages p",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) BotanicalOnPrimaryFixed else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section 3: Backup & Restore (DEC-029, DEC-040)
            item {
                SettingsSectionCard(title = "Strict Offline Backup", icon = "💾") {
                    Text(
                        text = "DailyRead has zero internet permission. Create offline JSON snapshots of your reading ledger, books, streaks, and bookmarks to store safely or transfer between devices.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                val dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                                createDocumentLauncher.launch("dailyread_backup_$dateStr.json")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BotanicalPrimary,
                                contentColor = BotanicalOnPrimary
                            ),
                            enabled = !uiState.isExporting && !uiState.isRestoring
                        ) {
                            if (uiState.isExporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = BotanicalOnPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "Export Backup",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                openDocumentLauncher.launch(arrayOf("application/json", "*/*"))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            enabled = !uiState.isExporting && !uiState.isRestoring
                        ) {
                            if (uiState.isRestoring) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = BotanicalPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "Restore Backup",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "• Excludes temporary cache files.\n" +
                                "• After restore, you will be prompted to reconnect your reading folders.",
                        style = MaterialTheme.typography.labelSmall,
                        color = BotanicalSecondary,
                        lineHeight = 18.sp
                    )
                }
            }

            // Section 4: About & Open Source Licenses
            item {
                SettingsSectionCard(title = "About & Licenses", icon = "📜") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "DailyRead",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Version 1.0 (Phase 6 Build)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BotanicalPrimaryFixed
                        ) {
                            Text(
                                text = "100% Offline",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = BotanicalOnPrimaryFixed,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.setLicensesDialogVisible(true) },
                        shape = RoundedCornerShape(12.dp),
                        color = BotanicalSurfaceContainerHigh
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Open Source Licenses",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "View →",
                                style = MaterialTheme.typography.labelMedium,
                                color = BotanicalPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // CRITICAL UX Dialog (BS-04 / DEC-040): SAF Relink Prompt
        if (uiState.showRelinkPrompt) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissRelinkPrompt() },
                title = {
                    Text(
                        text = "Reconnect Reading Folders",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Backup restored successfully!\n" +
                                    "• ${uiState.restoreSummary?.booksCount ?: 0} books\n" +
                                    "• ${uiState.restoreSummary?.dailySummariesCount ?: 0} reading ledger days\n" +
                                    "• ${uiState.restoreSummary?.plansCount ?: 0} reading plans",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Because DailyRead reads files in-place without duplicating them, please re-select your reading folder to grant Android file access permissions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            openTreeLauncher.launch(null)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BotanicalPrimary,
                            contentColor = BotanicalOnPrimary
                        )
                    ) {
                        Text("Select Folder to Relink")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissRelinkPrompt() }) {
                        Text("Later")
                    }
                }
            )
        }

        // Open Source Licenses Dialog
        if (uiState.showLicensesDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.setLicensesDialogVisible(false) },
                title = {
                    Text(
                        text = "Open Source Licenses",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            LicenseItem(
                                name = "DailyRead",
                                license = "Apache License 2.0",
                                copyright = "Copyright 2026 Natnael Dev"
                            )
                        }
                        item {
                            LicenseItem(
                                name = "Readium Kotlin Toolkit",
                                license = "BSD 3-Clause License",
                                copyright = "Copyright European Digital Reading Lab (EDRLab)"
                            )
                        }
                        item {
                            LicenseItem(
                                name = "Literata Font Family",
                                license = "SIL Open Font License 1.1",
                                copyright = "Copyright 2017 Google LLC / TypeTogether"
                            )
                        }
                        item {
                            LicenseItem(
                                name = "Manrope Font Family",
                                license = "SIL Open Font License 1.1",
                                copyright = "Copyright 2018 Mikhail Sharanda"
                            )
                        }
                        item {
                            LicenseItem(
                                name = "Jetpack Compose & AndroidX",
                                license = "Apache License 2.0",
                                copyright = "Copyright The Android Open Source Project"
                            )
                        }
                        item {
                            LicenseItem(
                                name = "Room Persistence Library",
                                license = "Apache License 2.0",
                                copyright = "Copyright The Android Open Source Project"
                            )
                        }
                        item {
                            LicenseItem(
                                name = "Kotlin & Kotlinx Coroutines",
                                license = "Apache License 2.0",
                                copyright = "Copyright JetBrains s.r.o."
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.setLicensesDialogVisible(false) }) {
                        Text("Close", fontWeight = FontWeight.Bold, color = BotanicalPrimary)
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: String,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = BotanicalSurfaceContainerLow,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = icon, fontSize = 20.sp)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun ThemeOptionTile(
    title: String,
    subtitle: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) BotanicalPrimary else BotanicalOutlineVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) BotanicalPrimaryFixed.copy(alpha = 0.3f) else BotanicalSurfaceContainerLowest
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (selected) BotanicalPrimary else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusBanner(
    message: String,
    isSuccess: Boolean,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = if (isSuccess) BotanicalPrimaryFixed else Color(0xFFFFDAD6)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = if (isSuccess) BotanicalOnPrimaryFixed else Color(0xFF93000A),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "✕",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSuccess) BotanicalOnPrimaryFixed else Color(0xFF93000A),
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onDismiss)
                    .padding(4.dp)
            )
        }
    }
}

@Composable
private fun LicenseItem(
    name: String,
    license: String,
    copyright: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = license,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = BotanicalPrimary
        )
        Text(
            text = copyright,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
