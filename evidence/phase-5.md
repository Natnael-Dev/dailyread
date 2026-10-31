# Phase 5 Evidence: Background WorkManager Reminders & Jetpack Glance Home-Screen Widget

**Project:** DailyRead  
**Application ID:** `io.github.natnael_dev.dailyread`  
**Phase:** Phase 5 — Background WorkManager Reminders & Jetpack Glance Widget  
**Feature Branch:** `feat/phase-5-background-widgets`  
**Target Branch:** `main`  
**Date:** 2026-10-31  
**Verification Device:** Google Pixel 6 (`emulator-5554`), Android 14 (API 34)  

---

## 1. Executive Summary

Phase 5 delivers the background engagement loop and home-screen presence for DailyRead:
1. **Psychological Reminder Engine (DEC-045 & DEC-034):**
   - Implemented daily inexact WorkManager background reminders without requiring exact-alarm permissions (`SCHEDULE_EXACT_ALARM` or `USE_EXACT_ALARM`).
   - Built a multi-armed bandit tone selection engine (`ReminderCopySelector.kt`) supporting 5 distinct behavioral strategies: Loss Aversion, Urgency Escalation, Content Curiosity, Passive-Aggressive, and the 3-day Surrender Hook.
   - Respects user system notification settings and the "Default ON" specification with quiet-hours and daily goal suppression.
2. **Jetpack Glance Home-Screen Widget (ADR-007):**
   - Developed a read-only home-screen widget using Jetpack Glance (`1.1.1`) and Glance Material 3 tokens matching the warm botanical editorial design language (Ivory `#FCF9EF`, Charcoal `#1C1C16`, Terracotta `#94452C`, Sage Green `#4D6148`).
   - State is derived directly from Room at render time (`WidgetStateDeriver.kt`) without requiring an active foreground application process.
   - Deep-linking action launches `MainActivity` directly into the last-read book reader via verified book ID intent extras.
   - Scheduled an inexact WorkManager rollover worker (`WidgetRolloverWorker.kt`) near the 03:00 logical boundary, supplemented by broadcast receivers on timezone, date, boot, and package update events.
3. **Workflow & Safety Invariants:**
   - Updated `docs/USER_PREFERENCES.md` to codify the strict Feature Branch + Pull Request workflow.
   - Confirmed machine-level `PreToolUse` safety hook integrity.
   - Enforced zero `INTERNET` permission in `AndroidManifest.xml`.
   - Maintained Room schema version at v1 with zero schema migration requirement.
   - 100% test pass rate across 105 automated tests (81 unit tests + 24 connected tests on physical/emulated Pixel 6 API 34).

---

## 2. Git Feature Branch & Conventional Commits

All changes were authored and pushed exclusively to the feature branch `feat/phase-5-background-widgets`:

```text
e3a307f docs(workflow): add git and remote sync workflow rules to user preferences
fb90342 build(deps): add glance and workmanager dependencies with test libraries
f10aa1d feat(notifications): implement reminder copy selection engine with psychological hooks and bandit logic
1833d35 feat(reminders): implement daily habit reminders and inexact scheduling via workmanager
caf4cc3 feat(widget): implement jetpack glance home-screen widget with room state derivation and botanical styling
991f9d1 test(widget): add instrumented tests for glance widget state derivation, deep link, and reminder worker
```

---

## 3. Security & Architecture Invariants Verification

| Invariant | Specification | Evidence / Audit Result | Status |
|---|---|---|---|
| **Zero Exact Alarms** | No `SCHEDULE_EXACT_ALARM` or `USE_EXACT_ALARM` | Manifest inspection confirms zero exact-alarm permissions. Scheduling uses WorkManager `ExistingPeriodicWorkPolicy.KEEP` and inexact interval delays. | `ENFORCED` |
| **Zero Internet** | Manifest must never declare `android.permission.INTERNET` | Verified by `ManifestSecurityTest.kt` passing 100% green. | `ENFORCED` |
| **No Schema Bump** | Room database schema version must remain at v1 | `DailyReadDatabase.kt` remains at version 1; `WidgetSnapshotEntity`, `SettingEntity`, and `BookEntity` pre-declared in v1 schema. | `ENFORCED` |
| **Render-Time Room Derivation** | Glance widget must compute state from Room directly | `WidgetStateDeriver.kt` reads `ReadingLedgerDao`, `BookDao`, and `SettingsDao` at render time. Verified in `WidgetStateDeriverTest` and `GlanceWidgetDeviceTest`. | `ENFORCED` |
| **03:00 Day Rollover** | Widget and reminders must align with 03:00 logical boundary | `WidgetRolloverScheduler.kt` and `DailyReminderWorker.kt` compute delays against `LogicalDayCalculator` with 03:00 boundary. | `ENFORCED` |

---

## 4. Psychological Hook Engine (DEC-045) Matrix

| Tone Strategy | Trigger Condition | Notification Copy Example | Behavioral Intent |
|---|---|---|---|
| **Loss Aversion** | Active streak >= 3 days | *"Don't let your 7-day streak vanish into the ether. Read 3 pages before 03:00."* | Loss aversion: streaks are painful to rebuild. |
| **Urgency Escalation** | Evening reminder (>= 20:00) | *"Tonight's window is closing. 5 minutes to keep your daily commitment alive."* | Scarcity & deadline urgency. |
| **Content Curiosity** | Book in progress with title | *"Your next chapter in 'Principles of Psychology' is waiting. Pick up where you left off."* | Zeigarnik effect / curiosity gap. |
| **Passive-Aggressive** | Streak broken or at risk | *"Your books aren't going to read themselves. Even one page counts."* | Guilt / accountability prompt. |
| **Surrender Hook** | Inactive for >= 3 days | *"We get it. Life gets busy. Whenever you're ready to read again, we'll be here."* | Reverse psychology / reduces notification fatigue. |

---

## 5. Raw Automated Test Execution Evidence

### 5.1 Unit Test Suite Execution (`:core-domain` + `:app`)

Command: `.\gradlew.bat testDebugUnitTest`  
Exit Code: `0`  
Result: **81 / 81 Tests Passed (100% Green, 0 Failures, 0 Skipped)**

#### `:core-domain` Test Results (32 Tests)
- `io.github.natnael_dev.dailyread.domain.LogicalDayCalculatorTest`: 6 passed
  - `SC-10: 02:59 assigns to prior day, 03:01 assigns to today`
  - `SC-11: Reading interval spanning 03:00 boundary splits cleanly into two logical days`
  - `SC-12: Reading timestamp dated after device current instant is flagged and rejected`
  - `SC-13: Goal raised today applies starting tomorrow, today evaluated against prior target`
  - `SC-19: Backward wall-clock jump flagged suspect, monotonic elapsed realtime preserved`
  - `SC-20: Configurable dayStart rollover shifts cut-over boundary cleanly`
- `io.github.natnael_dev.dailyread.domain.ReadingTrackerTest`: 10 passed (SC-TRK-01 to SC-TRK-10)
- `io.github.natnael_dev.dailyread.domain.StreakEngineTest`: 10 passed (SC-01 to SC-09, SC-21)
- `io.github.natnael_dev.dailyread.domain.plan.ReadingPlanTest`: 6 passed

#### `:app` Unit Test Results (49 Tests)
- `io.github.natnael_dev.dailyread.ManifestSecurityTest`: 2 passed
  - `manifestDoesNotDeclareInternetPermission`
  - `manifestDisablesAutoBackup`
- `io.github.natnael_dev.dailyread.data.ingest.IngestionEngineTest`: 6 passed
- `io.github.natnael_dev.dailyread.data.reader.PdfReadingCacheTest`: 5 passed
- `io.github.natnael_dev.dailyread.notifications.ReminderCopySelectorTest`: 6 passed
  - `selectCopy_lossAversion_whenStreakIsHigh`
  - `selectCopy_surrender_whenInactiveForThreeDays`
  - `selectCopy_urgencyEscalation_whenLateInDay`
  - `selectCopy_contentCuriosity_includesBookTitle`
  - `selectCopy_passiveAggressive_whenFallback`
  - `allTones_produceNonEmptyTitleAndBody`
- `io.github.natnael_dev.dailyread.notifications.ReminderSchedulerTest`: 4 passed
  - `calculateInitialDelayMs_whenTargetIsInFuture_schedulesForToday`
  - `calculateInitialDelayMs_whenTargetHasPassed_schedulesForTomorrow`
  - `calculateInitialDelayMs_atExactTargetTime_schedulesForTomorrow`
  - `calculateInitialDelayMs_customHourAndMinute`
- `io.github.natnael_dev.dailyread.ui.library.LibraryViewModelTest`: 9 passed
- `io.github.natnael_dev.dailyread.ui.reader.ReaderViewModelTest`: 5 passed
- `io.github.natnael_dev.dailyread.ui.theme.ThemeTokensTest`: 4 passed
- `io.github.natnael_dev.dailyread.ui.today.TodayViewModelTest`: 6 passed
- `io.github.natnael_dev.dailyread.widget.WidgetStateDeriverTest`: 2 passed
  - `deriveState_computesCorrectStreakAndPages`
  - `deriveState_whenGoalAlreadyMet_reportsQualifiedTrue`

---

### 5.2 Instrumented Device Test Suite Execution (`Pixel_6_API_34`)

Command: `.\gradlew.bat connectedDebugAndroidTest`  
Device: `Pixel_6_API_34(AVD) - 14` (`emulator-5554`)  
Exit Code: `0`  
Result: **24 / 24 Tests Passed (100% Green, 0 Failures, 0 Skipped)**

```text
> Task :app:connectedDebugAndroidTest

Starting 24 tests on Pixel_6_API_34(AVD) - 14
Pixel_6_API_34(AVD) - 14 Tests 1/24 completed. (0 skipped) (0 failed)
Pixel_6_API_34(AVD) - 14 Tests 4/24 completed. (0 skipped) (0 failed)
Pixel_6_API_34(AVD) - 14 Tests 7/24 completed. (0 skipped) (0 failed)
Pixel_6_API_34(AVD) - 14 Tests 9/24 completed. (0 skipped) (0 failed)
Pixel_6_API_34(AVD) - 14 Tests 23/24 completed. (0 skipped) (0 failed)
Pixel_6_API_34(AVD) - 14 Tests 24/24 completed. (0 skipped) (0 failed)
Finished 24 tests on Pixel_6_API_34(AVD) - 14

BUILD SUCCESSFUL in 3m 10s
72 actionable tasks: 6 executed, 66 up-to-date
```

**Instrumented Test Classes on Device:**
1. `io.github.natnael_dev.dailyread.PopulateLibraryDeviceTest` (1 test: `populateLibraryForScreenshot`) — PASSED
2. `io.github.natnael_dev.dailyread.ReaderResumeDeviceTest` (4 tests: `testPdfExactResumePersistenceAndReadingCache`, `testEpubExactResumePersistence`, `testAmharicEpubRenderSmoke`, `testBookmarkCrudOnDevice`) — PASSED
3. `io.github.natnael_dev.dailyread.SafRebootPersistenceTest` (1 test: `testRebootPermissionPersistence`) — PASSED
4. `io.github.natnael_dev.dailyread.data.db.LedgerDaoTest` (9 tests: full DAO SQLite round-trips) — PASSED
5. `io.github.natnael_dev.dailyread.TodayDashboardDeviceTest` (3 tests: `testTodayDashboardLaunchesSuccessfully`, `testContinueReadingDeepLinkTarget`, `testQuickAddPagesIncrementsDatabase`) — PASSED
6. `io.github.natnael_dev.dailyread.BackupRestoreDeviceTest` (1 test: `testMockBackupRestoreAndSafRePermissionFlow`) — PASSED
7. `io.github.natnael_dev.dailyread.notifications.DailyReminderWorkerDeviceTest` (2 tests: `testWorkerSuppressesWhenGoalAlreadyMet`, `testWorkerExecutesWhenGoalNotMet`) — PASSED
8. `io.github.natnael_dev.dailyread.widget.GlanceWidgetDeviceTest` (3 tests: `testWidgetDerivesStateDirectlyFromRoom`, `testWidgetDeepLinkIntentLaunchesAppToBook`, `testWidgetRolloverWorkerExecutesSuccessfully`) — PASSED

---

## 6. Pull Request Metadata

- **Head Branch:** `feat/phase-5-background-widgets`
- **Base Branch:** `main`
- **PR Status:** Ready for Audit / Review
- **Repository:** `Natnael-Dev/dailyread`
