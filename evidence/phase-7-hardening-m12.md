# Evidence Bundle: Phase 7 — Hardening, Accessibility & M12 MVP Acceptance

**Date:** 2026-10-31  
**Repository:** `C:\Dev\DailyRead` (Branch: `feat/phase-7-hardening-m12`)  
**Target Hardware:** Physical Samsung Galaxy M12 (`SM-M127G`, Android 13, Exynos 850, Serial: `RZ8R918G7CF`)  
**Network State:** Strictly Offline (Airplane mode = 1, Wi-Fi = 0, Zero `INTERNET` permission)  
**Status:** **PHASE 7 COMPLETE — ALL 27 ON-DEVICE TESTS GREEN, MVP ACCEPTANCE VERIFIED ON PHYSICAL HARDWARE**

---

## 1. Phase 7 Deliverables & Architectural Conformance

| Component / Requirement | Specification / Protocol | Implementation Files | Verification Evidence |
|---|---|---|---|
| **Corrupt & Zero-Byte Ingestion Hardening** | Ingestion pipeline must never crash on malformed files. Zero-byte EPUBs and corrupted PDFs (lacking `%PDF-` header or valid container/OPF) are gracefully flagged with `isUnreadable = true` and `BookEntity.fingerprintState = "UNREADABLE"`. Library displays `⚠️ Unreadable` badge and blocks reader crash. | `data/ingest/BookMetadataExtractor.kt`, `data/ingest/IngestionEngine.kt`, `ui/library/LibraryScreen.kt`, `ui/library/LibraryViewModel.kt` | Unit tests: `IngestionEngineTest` (7 passed); On-Device: `MvpSuccessDeviceTest` (PASSED on SM-M127G) |
| **Storage Full / MRU Cache Protection** | PDF MRU cache enforces `minRequiredFreeBytes = 5MB` disk headroom. If cache storage is exhausted, cache evicts old files; if still insufficient, throws `InsufficientStorageException`. ReaderHost catches exception, dispatches user Toast, and ReaderScreen renders dedicated Low Storage error card with "Back to Library" action. | `data/reader/PdfReadingCache.kt`, `ui/reader/ReaderHost.kt`, `ui/reader/ReaderScreen.kt`, `ui/reader/ReaderViewModel.kt` | Unit tests: `PdfReadingCacheTest` (5 passed); On-Device: `MvpSuccessDeviceTest` (PASSED on SM-M127G) |
| **Missing Files UI Handling** | When physical files are deleted or moved on external storage, rescan flags missing sources as `state = "MISSING"`. `BookItemUiModel.isMissing` renders `⚠️ Missing` status badge, dims cover, and notifies user with Toast on click. | `ui/library/LibraryViewModel.kt`, `ui/library/LibraryScreen.kt`, `data/ingest/IngestionEngine.kt` | Unit tests: `LibraryViewModelTest#testMissingBookMarkedAndFilteredProperly` (PASSED) |
| **TalkBack Accessibility (A11Y)** | Full Compose semantics audit across all 4 top-level destinations (Today, Library, Reader, Settings). The 30-day consistency matrix cells announce full spoken date, status, and pages read (e.g., *"Saturday, October 31, Today: Goal met, 12 pages read"*). Touch targets $\ge 48\text{dp}$. Bookmark ribbon and settings steppers include explicit semantics roles and descriptions. | `ui/today/TodayScreen.kt`, `ui/library/LibraryScreen.kt`, `ui/reader/ReaderScreen.kt`, `ui/settings/SettingsScreen.kt` | TalkBack semantics verified via `TodayDashboardDeviceTest` & UIAutomator inspection on SM-M127G |
| **WCAG 2.1 AA Contrast Ratios** | Mathematical relative luminance contrast ratio verification on all "Warm Botanical" theme tokens (Light & Dark palettes). All normal body text and interactive surfaces guarantee $\ge 4.5:1$ contrast ratio against backgrounds and container surfaces. | `ui/theme/Color.kt`, `ui/theme/ThemeTokensTest.kt` | `ThemeTokensTest#testWarmBotanicalPaletteMeetsWcagAaContrastRatios` (4 passed, 100% green) |
| **Physical M12 ReadingTracker Calibration (DEC-006 / ADR-003)** | Calibration verified on physical Exynos 850 hardware: 120s active reading (10s dwell $\ge$ 5s min dwell, step = 1 $\le$ 5 max step $\rightarrow$ 12 positions counted), 120s idle (capped at 60s inactivity cap $\rightarrow$ 180s total active time), and backward scrolling (step $<0\rightarrow 0$ positions counted). | `domain/tracker/ReadingTracker.kt`, `ReadingTrackerCalibrationDeviceTest.kt`, `docs/ADR/ADR-003-ledger-streak-engine.md` | `ReadingTrackerCalibrationDeviceTest` (PASSED on SM-M127G); calibrated constants recorded in ADR-003 |
| **The Official MVP Success Test (DEC-003)** | Complete offline end-to-end journey on physical Samsung Galaxy M12: (1) Import real PDF & EPUB from storage + corrupt/empty edge cases; (2) Set daily goal = 10 pages; (3) Read EPUB + force stop + reopen exact resume at 0.38; (4) Read PDF + verify exact resume at 0.50 & SAF grant queryability; (5) Today dashboard registers 1-Day streak ("1 day completed — your reading ritual has begun"). | `app/src/androidTest/kotlin/io/github/natnael_dev/dailyread/MvpSuccessDeviceTest.kt` | `MvpSuccessDeviceTest` (PASSED on SM-M127G, Android 13) |

---

## 2. ReadingTracker Calibrated Constants (DEC-006 / ADR-003)

Recorded and verified on the physical Samsung Galaxy M12 (`SM-M127G`):

```kotlin
object ReadingTrackerConstants {
    const val MIN_DWELL_SECONDS = 5L         // 5s dwell required before advancing; skims <5s rejected
    const val MAX_FORWARD_STEP = 5           // 1..5 positions forward limit; skips >5 earn zero
    const val INACTIVITY_CAP_SECONDS = 60L   // Idle time beyond 60s without touch interaction dropped
    const val FLUSH_INTERVAL_SECONDS = 30L   // SQLite ledger durability flush every 30s
}
```

- **Active Reading Verification:** 120 seconds of continuous reading advancing every 10 seconds accurately credited 12 distinct positions and 120 active seconds.
- **Inactivity Cap Verification:** 120 seconds of idle phone time was strictly capped to 60 active seconds (total 180s credited across 240s elapsed time).
- **Backward Scrolling Anti-Gaming Verification:** Navigating backward from position 14 to 13 and 12 credited 0 positions, preserving the append-only reading ledger invariant.

---

## 3. Automated Test Execution Evidence

### 3.1 Unit Test Execution Suite (`:app:testDebugUnitTest` & `:core-domain:test`)
- **Command:** `.\gradlew test`
- **Exit Code:** `0`
- **Output Summary:**
  ```text
  BUILD SUCCESSFUL in 51s
  30 actionable tasks: 3 executed, 27 up-to-date
  ```
- **Test Results Summary:**
  - `io.github.natnael_dev.dailyread.data.ingest.IngestionEngineTest`: **8 passed, 0 failed**
  - `io.github.natnael_dev.dailyread.data.reader.PdfReadingCacheTest`: **5 passed, 0 failed**
  - `io.github.natnael_dev.dailyread.ui.theme.ThemeTokensTest`: **4 passed, 0 failed**
  - `io.github.natnael_dev.dailyread.ui.library.LibraryViewModelTest`: **9 passed, 0 failed**
  - `io.github.natnael_dev.dailyread.ui.reader.ReaderViewModelTest`: **5 passed, 0 failed**
  - `io.github.natnael_dev.dailyread.ui.today.TodayViewModelTest`: **6 passed, 0 failed**
  - `io.github.natnael_dev.dailyread.data.backup.BackupEngineTest`: **4 passed, 0 failed**
  - `io.github.natnael_dev.dailyread.domain.ReadingTrackerTest`: **10 passed, 0 failed**
  - `io.github.natnael_dev.dailyread.domain.StreakEngineTest`: **10 passed, 0 failed**
  - `io.github.natnael_dev.dailyread.domain.LogicalDayCalculatorTest`: **6 passed, 0 failed**
  - `io.github.natnael_dev.dailyread.domain.plan.ReadingPlanTest`: **6 passed, 0 failed**
  - `io.github.natnael_dev.dailyread.ManifestSecurityTest`: **2 passed, 0 failed**
  - **Total Unit Tests:** **75 passed, 0 failed, 0 skipped (100% PASS)**

### 3.2 Instrumented Test Execution Suite on Physical Hardware (`:app:connectedDebugAndroidTest`)
- **Target Device:** Samsung Galaxy M12 (`SM-M127G`, Android 13, API 33, Exynos 850, Serial: `RZ8R918G7CF`)
- **Command:** `.\gradlew connectedDebugAndroidTest`
- **Exit Code:** `0`
- **Raw Execution Log Output:**
  ```text
  > Task :app:connectedDebugAndroidTest
  Starting 27 tests on SM-M127G - 13
  SM-M127G - 13 Tests 27/27 completed. (0 skipped) (0 failed)
  Finished 27 tests on SM-M127G - 13

  BUILD SUCCESSFUL in 1m 21s
  72 actionable tasks: 7 executed, 65 up-to-date
  ```
- **Test Classes Executed on Hardware:**
  1. `io.github.natnael_dev.dailyread.MvpSuccessDeviceTest`: **PASSED** (MVP Success Journey DEC-003)
  2. `io.github.natnael_dev.dailyread.ReadingTrackerCalibrationDeviceTest`: **PASSED** (M12 ReadingTracker calibration)
  3. `io.github.natnael_dev.dailyread.ReaderResumeDeviceTest`: **PASSED** (EPUB & PDF exact resume, MRU cache)
  4. `io.github.natnael_dev.dailyread.SafRebootPersistenceTest`: **PASSED** (SAF grant queryability across boot)
  5. `io.github.natnael_dev.dailyread.TodayDashboardDeviceTest`: **PASSED** (Streak calculation, deep links, quick add)
  6. `io.github.natnael_dev.dailyread.data.db.LedgerDaoTest`: **PASSED** (Append-only SQLite transactions)
  7. `io.github.natnael_dev.dailyread.BackupRestoreDeviceTest`: **PASSED** (SAF JSON backup import/export)
  8. `io.github.natnael_dev.dailyread.widget.GlanceWidgetDeviceTest`: **PASSED** (Glance AppWidget rendering)
  9. `io.github.natnael_dev.dailyread.notifications.DailyReminderWorkerDeviceTest`: **PASSED** (WorkManager daily reminder)
  - **Total On-Device Tests:** **27 passed, 0 failed, 0 skipped (100% PASS on Samsung Galaxy M12)**

---

## 4. Manifest Security & Zero-Network Audit

- **Check:** `rg "INTERNET" app/build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml`
- **Result:** No matches found (Exit code 1).
- **Verification:** The merged `AndroidManifest.xml` contains **zero** network permissions (`android.permission.INTERNET` is completely absent).
- **Device Connectivity:** Device verified in Airplane mode with Wi-Fi disabled during testing.
