# ADR-003: Append-only reading ledger, derived streaks
Status: PROPOSED (DEC-023, 024, 025, 026, 027, 028)
Context: Public bug histories (learn-only study) show incremental counters and tap-to-check actions near midnight cause wrong-day and double-penalty bugs; editing goals rewrote history in one tracker.
Decision: Store events (intervals, distinct positions, versioned goals); compute streaks by a pure deterministic function; caches are rebuildable. Rules are in SPEC sections 1 to 6.
Why: correctness, testability with a fake clock, no tap-to-check bugs (streaks come from reading, widgets are read-only).
Trade-offs: recompute cost (linear, negligible); more tables.
Calibration record (calibrated on Samsung Galaxy M12, DEC-006 / Phase 7):
   - Min dwell threshold: 5 seconds (MIN_DWELL_SECONDS = 5L) - verified 10s active reading credits position; <5s skimming rejected.
   - Max forward step: 5 positions (MAX_FORWARD_STEP = 5) - prevents chapter-skipping gaming.
   - Inactivity timeout / cap: 60 seconds (INACTIVITY_CAP_SECONDS = 60L) - 2 minutes idle capped to 60s active time.
   - Ledger flush interval: 30 seconds (FLUSH_INTERVAL_SECONDS = 30L) - prevents eMMC/UFS I/O exhaustion.
   - Default daily goal: 10 pages (DEFAULT_GOAL = 10).
Reversibility: medium.
