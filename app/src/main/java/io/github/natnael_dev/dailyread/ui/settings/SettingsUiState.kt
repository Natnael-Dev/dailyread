package io.github.natnael_dev.dailyread.ui.settings

import io.github.natnael_dev.dailyread.data.backup.BackupSummary

data class SettingsUiState(
    val themeMode: String = "SYSTEM", // "SYSTEM", "LIGHT", "DARK"
    val dailyGoalPages: Int = 10,
    val isExporting: Boolean = false,
    val isRestoring: Boolean = false,
    val exportSuccessMessage: String? = null,
    val restoreSummary: BackupSummary? = null,
    val showRelinkPrompt: Boolean = false,
    val showLicensesDialog: Boolean = false,
    val errorMessage: String? = null
)
