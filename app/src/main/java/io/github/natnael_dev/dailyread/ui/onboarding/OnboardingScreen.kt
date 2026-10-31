package io.github.natnael_dev.dailyread.ui.onboarding

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.natnael_dev.dailyread.ui.theme.BotanicalOnPrimary
import io.github.natnael_dev.dailyread.ui.theme.BotanicalOnPrimaryFixed
import io.github.natnael_dev.dailyread.ui.theme.BotanicalPrimary
import io.github.natnael_dev.dailyread.ui.theme.BotanicalPrimaryFixed
import io.github.natnael_dev.dailyread.ui.theme.BotanicalSecondary
import io.github.natnael_dev.dailyread.ui.theme.BotanicalSecondaryContainer
import io.github.natnael_dev.dailyread.ui.theme.BotanicalSurfaceContainerHigh
import io.github.natnael_dev.dailyread.ui.theme.BotanicalSurfaceContainerLow
import io.github.natnael_dev.dailyread.ui.theme.BotanicalSurfaceContainerLowest

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    onSelectFolder: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentStep by remember { mutableIntStateOf(1) }

    val openTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            onSelectFolder(uri)
            onComplete()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Top Step Progress Indicator
            OnboardingProgressHeader(currentStep = currentStep)

            Spacer(modifier = Modifier.height(16.dp))

            // Animated Screen Content
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut()
                        )
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> width } + fadeOut()
                        )
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                label = "onboarding_step"
            ) { step ->
                when (step) {
                    1 -> Step1Content(
                        onNext = { currentStep = 2 },
                        onSkip = onComplete
                    )
                    2 -> Step2Content(
                        onNext = { currentStep = 3 },
                        onBack = { currentStep = 1 }
                    )
                    3 -> Step3Content(
                        onSelectFolder = { openTreeLauncher.launch(null) },
                        onBack = { currentStep = 2 },
                        onSkip = onComplete
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingProgressHeader(currentStep: Int) {
    val stepLabel = when (currentStep) {
        1 -> "Welcome"
        2 -> "Storage & Privacy"
        else -> "Connect Library"
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "STEP $currentStep OF 3",
                style = MaterialTheme.typography.labelSmall,
                color = BotanicalSecondary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Text(
                text = stepLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (i in 1..3) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (i <= currentStep) BotanicalPrimary else BotanicalSurfaceContainerHigh
                        )
                )
            }
        }
    }
}

@Composable
private fun Step1Content(
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Hero Medallion
        HeroMedallion(iconText = "🌿", backgroundColor = BotanicalSurfaceContainerLow)

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Make reading a daily habit",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Read a little every day. Build your streak, track your progress, and keep turning pages.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Bento Value Proposition Cards
        BentoCard(
            icon = "🔥",
            iconBg = BotanicalSecondaryContainer,
            title = "Build your streak",
            badge = "Habit",
            subtitle = "Make reading a habit, one day at a time with automatic freeze protection."
        )

        Spacer(modifier = Modifier.height(12.dp))

        BentoCard(
            icon = "📈",
            iconBg = BotanicalSurfaceContainerHigh,
            title = "Track your pacing",
            badge = "Pacing",
            subtitle = "Gentle daily goals without guilt or distractions."
        )

        Spacer(modifier = Modifier.height(12.dp))

        BentoCard(
            icon = "📚",
            iconBg = BotanicalSurfaceContainerHigh,
            title = "Your books, your library",
            badge = "Local",
            subtitle = "Bring your own local EPUB and PDF files directly."
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Actions
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BotanicalPrimary,
                contentColor = BotanicalOnPrimary
            )
        ) {
            Text(
                text = "Continue",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(
            onClick = onSkip,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Explore app first",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun Step2Content(
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Hero Medallion
        HeroMedallion(iconText = "🛡️", backgroundColor = BotanicalSurfaceContainerLow)

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Private by design, 100% offline",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "DailyRead has zero internet permission and never sends your data to any server.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Bento Cards
        BentoCard(
            icon = "📁",
            iconBg = BotanicalSecondaryContainer,
            title = "Zero-Copy In-Place Reading",
            badge = "SAF",
            subtitle = "Books stay in your storage. DailyRead never duplicates or uploads files."
        )

        Spacer(modifier = Modifier.height(12.dp))

        BentoCard(
            icon = "🔒",
            iconBg = BotanicalSurfaceContainerHigh,
            title = "Zero Cloud & Zero Tracking",
            badge = "Offline",
            subtitle = "No accounts, no analytics, no external servers. Everything is on-device."
        )

        Spacer(modifier = Modifier.height(12.dp))

        BentoCard(
            icon = "✈️",
            iconBg = BotanicalSurfaceContainerHigh,
            title = "Read Anywhere, Anytime",
            badge = "Strict",
            subtitle = "Airplane mode, off-grid, or deep focus. DailyRead is always ready."
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Actions
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BotanicalPrimary,
                contentColor = BotanicalOnPrimary
            )
        ) {
            Text(
                text = "Next: Connect Library",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Back",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun Step3Content(
    onSelectFolder: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Hero Medallion
        HeroMedallion(iconText = "📂", backgroundColor = BotanicalSurfaceContainerLow)

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Select your book folder",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Pick the folder where your EPUB and PDF files are stored on this device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Informational Callout Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = BotanicalSurfaceContainerLow,
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "How it works",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "• Choose any folder on your device storage or SD card.\n" +
                            "• DailyRead discovers EPUB and PDF titles in place.\n" +
                            "• Your reading positions, bookmarks, and streaks are recorded locally.\n" +
                            "• You can add more folders later anytime in Library.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        // Actions
        Button(
            onClick = onSelectFolder,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BotanicalPrimary,
                contentColor = BotanicalOnPrimary
            )
        ) {
            Text(
                text = "Select Folder to Import",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                onClick = onBack,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Back",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TextButton(
                onClick = onSkip,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "I'll do this later",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun HeroMedallion(iconText: String, backgroundColor: Color) {
    Surface(
        modifier = Modifier.size(108.dp),
        shape = RoundedCornerShape(32.dp),
        color = backgroundColor,
        shadowElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(22.dp),
                color = BotanicalSurfaceContainerLowest,
                shadowElevation = 1.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = iconText,
                        fontSize = 38.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun BentoCard(
    icon: String,
    iconBg: Color,
    title: String,
    badge: String,
    subtitle: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = BotanicalSurfaceContainerLow,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    fontSize = 20.sp
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = BotanicalPrimaryFixed
                    ) {
                        Text(
                            text = badge,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = BotanicalOnPrimaryFixed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
