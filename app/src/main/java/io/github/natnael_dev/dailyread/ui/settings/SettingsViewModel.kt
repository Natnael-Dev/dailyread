package io.github.natnael_dev.dailyread.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.natnael_dev.dailyread.data.backup.BackupEngine
import io.github.natnael_dev.dailyread.data.db.dao.ReadingLedgerDao
import io.github.natnael_dev.dailyread.data.db.dao.SettingsDao
import io.github.natnael_dev.dailyread.data.db.entity.GoalVersionEntity
import io.github.natnael_dev.dailyread.data.db.entity.SettingEntity
import io.github.natnael_dev.dailyread.domain.model.Goal
import io.github.natnael_dev.dailyread.domain.time.Clock
import io.github.natnael_dev.dailyread.domain.time.LogicalDayCalculator
import io.github.natnael_dev.dailyread.domain.time.SystemClock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

class SettingsViewModel(
    private val settingsDao: SettingsDao,
    private val readingLedgerDao: ReadingLedgerDao,
    private val backupEngine: BackupEngine,
    private val clock: Clock = SystemClock(),
    private val logicalDayCalculator: LogicalDayCalculator = LogicalDayCalculator(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            loadSettings()
        }
    }

    private suspend fun loadSettings() {
        val theme = settingsDao.getSetting("app_theme") ?: "SYSTEM"

        val now = clock.now()
        val zoneId = clock.zoneId()
        val today = logicalDayCalculator.computeLogicalDay(now, zoneId)
        val activeGoal = readingLedgerDao.getActiveGoalForDay(today.toIsoString())
        val target = activeGoal?.target ?: Goal.DEFAULT.target

        _uiState.update {
            it.copy(
                themeMode = theme,
                dailyGoalPages = target
            )
        }
    }

    fun setAppTheme(mode: String) {
        viewModelScope.launch {
            settingsDao.setSetting(SettingEntity(key = "app_theme", value = mode))
            _uiState.update { it.copy(themeMode = mode) }
        }
    }

    fun setDailyGoal(pages: Int) {
        val clampedPages = pages.coerceIn(1, 100)
        viewModelScope.launch {
            val now = clock.now()
            val zoneId = clock.zoneId()
            val today = logicalDayCalculator.computeLogicalDay(now, zoneId)

            readingLedgerDao.insertGoalVersion(
                GoalVersionEntity(
                    effectiveFromDay = today.toIsoString(),
                    goalType = "PAGES",
                    target = clampedPages
                )
            )
            settingsDao.setSetting(
                SettingEntity(
                    key = "daily_goal_target",
                    value = clampedPages.toString()
                )
            )
            _uiState.update { it.copy(dailyGoalPages = clampedPages) }
        }
    }

    fun exportBackup(outputStream: OutputStream) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, errorMessage = null, exportSuccessMessage = null) }
            try {
                withContext(ioDispatcher) {
                    backupEngine.exportToStream(outputStream)
                }
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportSuccessMessage = "Backup exported successfully!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        errorMessage = "Export failed: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun restoreBackup(inputStream: InputStream) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRestoring = true,
                    errorMessage = null,
                    exportSuccessMessage = null
                )
            }
            try {
                val summary = withContext(ioDispatcher) {
                    backupEngine.importFromStream(inputStream)
                }
                // Reload settings after restore
                loadSettings()
                _uiState.update {
                    it.copy(
                        isRestoring = false,
                        restoreSummary = summary,
                        showRelinkPrompt = true // CRITICAL UX (BS-04)
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isRestoring = false,
                        errorMessage = "Restore failed: ${e.localizedMessage ?: "Invalid backup file"}"
                    )
                }
            }
        }
    }

    fun dismissRelinkPrompt() {
        _uiState.update { it.copy(showRelinkPrompt = false) }
    }

    fun setLicensesDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showLicensesDialog = visible) }
    }

    fun clearMessages() {
        _uiState.update {
            it.copy(
                errorMessage = null,
                exportSuccessMessage = null
            )
        }
    }

    fun resetOnboarding() {
        viewModelScope.launch {
            settingsDao.setSetting(SettingEntity(key = "onboarding_completed", value = "false"))
        }
    }
}
