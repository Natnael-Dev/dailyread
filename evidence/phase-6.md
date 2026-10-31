# Evidence Bundle: Phase 6 — Backup/Restore, Onboarding & Settings

**Date:** 2026-10-31  
**Repository:** `C:\Dev\dailyread-phase6` (Branch: `feat/phase-6-backup-onboarding`)  
**Target Device / Environment:** Android SDK 34 (`Pixel_6_API_34` Emulator, Android 14)  
**Status:** **PHASE 6 COMPLETE — ALL TESTS GREEN, VERIFIED ON DEVICE**  

---

## 1. Phase 6 Deliverables & Architectural Conformance

| Component / Requirement | Design / Protocol Specification | Implementation Files | Verification Evidence |
|---|---|---|---|
| **Strict Offline Backup Export (DEC-029, DEC-040)** | Room database serialization to pure JSON `dailyread_backup_YYYYMMDD.json` via SAF `ACTION_CREATE_DOCUMENT`. Includes books, book sources, reading positions, bookmarks, reading intervals, daily positions, daily summaries, goal versions, plans, settings, and library sources. Excludes cache and temporary reading files. | `data/backup/BackupEngine.kt`, `data/backup/SimpleJson.kt`, `data/backup/BackupData.kt` | `BackupEngineTest.kt` (4/4 passed), `BackupRestoreDeviceTest.kt` (1/1 passed on device) |
| **Strict Offline Backup Import (DEC-029, DEC-040)** | JSON parsing via SAF `ACTION_OPEN_DOCUMENT` and batch transactional insertion into Room database. Zero external JSON dependencies (pure Kotlin parser). | `data/backup/BackupEngine.kt`, `data/backup/SimpleJson.kt`, `data/db/dao/Daos.kt` | `BackupEngineTest.kt` (4/4 passed), `BackupRestoreDeviceTest.kt` (1/1 passed on device) |
| **Critical UX SAF Re-permission Flow (BS-04)** | Post-restore prompt alerting user to re-select folder trees via `ACTION_OPEN_DOCUMENT_TREE` to batch-relink persistable permissions for restored books, preserving reading progress and positions. | `ui/settings/SettingsViewModel.kt`, `ui/settings/SettingsScreen.kt`, `MainActivity.kt` | `BackupRestoreDeviceTest.kt#testMockBackupRestoreAndSafRePermissionFlow` (PASSED on device), `SettingsViewModelTest.kt` |
| **3-Screen Welcome Onboarding Flow** | Matches Stitch `dailyread_welcome_onboarding`: Screen 1 (Value prop: offline, private, habit-first), Screen 2 (SAF zero-copy local storage), Screen 3 (SAF folder picker trigger). Displayed on first run only, tracked via `onboarding_completed` in Room Settings. | `ui/onboarding/OnboardingScreen.kt`, `MainActivity.kt` | Verified on `Pixel_6_API_34` emulator; screenshots `onboarding_screen1.png`, `onboarding_screen2.png`, `onboarding_screen3.png` |
| **Settings UI & Dynamic Appearance** | Appearance theme switcher (System Default, Light Ivory, Botanical Dark), daily reading goal stepper & preset chips, backup export/restore launchers, and Open Source Licenses modal. | `ui/settings/SettingsScreen.kt`, `ui/settings/SettingsViewModel.kt`, `ui/settings/SettingsUiState.kt` | Verified on `Pixel_6_API_34` emulator; screenshots `settings_screen.png`, `settings_backup_restore.png`, `settings_licenses.png` |

---

## 2. Automated Test Results (Unit & Instrumented)

### 2.1 Unit Test Execution Suite (`:app:testDebugUnitTest` & `:core-domain:test`)
- **Command:** `.\gradlew test`
- **Working Directory:** `C:\Dev\dailyread-phase6`
- **Exit Code:** `0`
- **Result:** `BUILD SUCCESSFUL in 1m`
- **Test Results Summary:**
  - `io.github.natnael_dev.dailyread.data.backup.BackupEngineTest`: **4 passed, 0 failed**
  - `io.github.natnael_dev.dailyread.ui.settings.SettingsViewModelTest`: **6 passed, 0 failed**
  - `io.github.natnael_dev.dailyread.data.ingest.IngestionEngineTest`: **6 passed, 0 failed**
  - `io.github.natnael_dev.dailyread.data.reader.PdfReadingCacheTest`: **5 passed, 0 failed**
  - `io.github.natnael_dev.dailyread.ui.library.LibraryViewModelTest`: **9 passed, 0 failed**
  - `io.github.natnael_dev.dailyread.ui.reader.ReaderViewModelTest`: **5 passed, 0 failed**
  - `io.github.natnael_dev.dailyread.ui.today.TodayViewModelTest`: **6 passed, 0 failed**
  - `io.github.natnael_dev.dailyread.ui.theme.ThemeTokensTest`: **4 passed, 0 failed**
  - `io.github.natnael_dev.dailyread.ManifestSecurityTest`: **2 passed, 0 failed**
  - `io.github.natnael_dev.dailyread.domain.LogicalDayCalculatorTest`: **6 passed, 0 failed**
  - `io.github.natnael_dev.dailyread.domain.plan.ReadingPlanTest`: **6 passed, 0 failed**
  - `io.github.natnael_dev.dailyread.domain.ReadingTrackerTest`: **10 passed, 0 failed**
  - `io.github.natnael_dev.dailyread.domain.StreakEngineTest`: **10 passed, 0 failed**
  - **Total Unit Tests:** **73 passed, 0 failed, 0 skipped (100% PASS)**

### 2.2 Instrumented On-Device Test (`:app:connectedDebugAndroidTest`)
- **Command:** `.\gradlew :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=io.github.natnael_dev.dailyread.BackupRestoreDeviceTest"`
- **Target Device:** `Pixel_6_API_34(AVD) - 14` (Android 14, API 34)
- **Exit Code:** `0`
- **Raw Execution Output:**
  ```text
  > Task :app:connectedDebugAndroidTest
  Starting 1 tests on Pixel_6_API_34(AVD) - 14
  Finished 1 tests on Pixel_6_API_34(AVD) - 14

  BUILD SUCCESSFUL in 1m 55s
  72 actionable tasks: 25 executed, 47 up-to-date
  ```
- **Verification Assertions:**
  1. Room database restored from mock JSON backup (1 book, 1 book source, 1 reading position, 1 plan, 2 settings, 1 library source).
  2. BS-04 SAF re-permission workflow executed via matched `treeUri` and `updateSourceState(id, "OK")`.
  3. Reading progress (`progression = 0.42`) and active plan targets (`targetDays = 40`, `totalUnits = 500`) confirmed intact.
  4. Database re-export produced valid JSON containing restored records.

---

## 3. Visual Artifacts & Screen Evidence

All screenshots captured live on `Pixel_6_API_34` (1080x2400):

| Screenshot Filename | Description | File Size |
|---|---|---|
| `evidence/screenshots/onboarding_screen1.png` | Onboarding Step 1: Value proposition (Offline, Private, Habit-first, streak pacing) | 348,566 B |
| `evidence/screenshots/onboarding_screen2.png` | Onboarding Step 2: Storage Access Framework zero-copy, local-only storage explanation | 384,014 B |
| `evidence/screenshots/onboarding_screen3.png` | Onboarding Step 3: Connect Library folder picker CTA ("Select Folder to Import") | 275,208 B |
| `evidence/screenshots/settings_screen.png` | Settings Screen: Appearance section (System / Light Ivory / Botanical Dark) | 459,748 B |
| `evidence/screenshots/settings_backup_restore.png` | Settings Screen: Daily reading goal adjustment stepper + Strict offline backup/restore buttons | 426,512 B |
| `evidence/screenshots/settings_licenses.png` | Settings Screen: Open Source Licenses attribution modal (Readium, Jetpack, Kotlin, Room) | 462,026 B |

---

## 4. Security & Offline Invariant Verification

- **Zero INTERNET Permission:** Merge manifest inspection verified `android.permission.INTERNET` is absent. `ManifestSecurityTest` passes.
- **Backup Exclusion:** `android:allowBackup="false"` is strictly enforced in `AndroidManifest.xml`.
- **Cache Exclusion:** Backup engine serializes only persistent Room records. PDF render caches (`PdfReadingCache`) are stored in app cache directory and excluded from backup JSON.
- **Zero Third-Party Cloud SDKs:** Zero tracking, zero telemetry, zero proprietary binary fonts.
