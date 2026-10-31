# User preferences

## Confirmed by owner
- **Communication:** plain English; explain trade-offs; recommendations welcome but decisions are the owner's. Claude plans, writes prompts and audits; Antigravity executes.
- **Workflow:** plan-only first (no code), owner relays plan to Claude for approval, then execution with an evidence bundle, then Claude audit, then owner tests on the Galaxy M12 at the end of each phase.
- **Autonomy:** Antigravity works freely on feature branches; must ask first for dependencies, architecture/schema changes, deletions/force-push, third-party APKs, scraping or network outside official docs (`AGENTS.md` section 4).
- **Git:** very atomic commits and all standard GitHub good practice (branches, issues, PRs, CI, templates). Conventional Commits; the owner does not want casual lowercase "human-sounding" messages in this project. No AI signatures.
- **Product:** free, non-commercial, open source, zero paid services, zero server. English UI. PDF and EPUB only. Habit-first UI with tactile streak and progress clarity (Duolingo/Snapchat style) plus a distraction-free reader. Themes: System/Light/Dark UI; reader Light, Sepia, Dark, AMOLED black.
- **Research:** learn-only mode (patterns, formulas, test scenarios, edge cases; original Apache-2.0 code). No reverse engineering of closed apps. Consenting developers may be interviewed.
- **Device:** Samsung Galaxy M12 (Android 13) is the primary testbed; minSdk delegated to the architect.
- **Questions to the owner:** asked inline in chat (no popup/button pickers).
- **Backup & restore UX:** After importing `data.json`, prompt the user to re-select reading folders via SAF to batch-relink books and restore persistable permissions.
- **Library scanning UX:** Quick file discovery runs in the foreground; deep fingerprinting/digest runs as an asynchronous background task with explicit progress UI rather than blocking the user.
- **Visual tone & personality:** Warm and motivating personality.
- **Milestone celebrations:** Subtle, non-intrusive celebrations for milestones (7, 30, 100 days); strictly respect system "reduce motion" accessibility settings.
- **Default daily goal:** 10 "pages" per day. EPUB "pages" are defined as Readium positions (~1,000 characters).
- **Reader controls (MVP):** Theme, Font Size, and Font Family. Line-spacing and margin adjustments are deferred to post-MVP.
- **Reminders default (Override):** Default state is ON. Users can disable later in Settings. Note: On Android 13+ (Samsung M12), `POST_NOTIFICATIONS` runtime permission must be requested and granted before notifications can be posted.
- **Notification psychology (Override):** "No guilt-tripping" restriction is overridden for notification copy and scheduling logic. App will utilize strong psychological hooks, urgency, and loss-aversion mechanics (Duolingo style).
- **UI implementation directive:** Implement UI strictly screen-by-screen, pixel-perfectly matching the design assets provided in `dailyread-dossier/img_assets` (`concept_02_nordic_paper_ink.png`, `pkg01_5_design_alternatives.png`, `pkg01_master_visual_overview.png`).
- **Review cadence:** Formally confirmed end-of-phase device testing on the physical Samsung Galaxy M12 (with `Pixel_6_API_34` emulator fallback).
- **Ethiopic font:** Confirmed conditional bundling based on API 26/28 emulator testing.

## Git & Remote Sync Workflow
- **Push Cadence:** Push to GitHub after every logical atomic commit.
- **Branch Protection & Feature Branches:** Never push directly to `main`. Machine-level safety hooks (`PreToolUse`) and GitHub branch protection strictly guard `main`. Always create a feature branch (e.g., `feat/phase-X-description`), push the branch to `origin`, and open a Pull Request targeting `main`.
- **CI & Merge Gate:** Ensure CI passes on the PR before merging (or leave the PR open for owner review if admin merge is required).

## Suggested, not yet confirmed
- 3 AM default day start, other provisional constants (see SPEC section 2).
