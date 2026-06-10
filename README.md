# Expense Tracker

A minimal, local-first **spending** tracker — spends only, no income. Everything lives on-device and the whole dataset backs up to a file the user controls; there is no app server. Built with Kotlin Multiplatform + Compose Multiplatform, Android-first.

The premise: tracking where your money goes is most useful when it's effortless. So beyond fast manual entry, on Android the app reads the payment notifications your bank, wallet and card apps already post, detects the spend on-device, and hands you a prefilled, categorized expense to confirm — nothing leaves the phone, and nothing is saved without you.

## Features

1. **Quick expense entry** — amount, category, date, merchant, note. Spends only; no income, no balance.
2. **Monthly summary** — total spent this month plus a per-category breakdown with proportion bars. The month selector is shared across tabs.
3. **Activity** — transactions grouped by day; tap to edit or delete.
4. **Reports** — a 12-month spend trend (column chart) and the selected month's category breakdown.
5. **Categories** — editable, each with an icon and colour; a sensible default set is seeded on first launch.
6. **Recurring expenses** — repeat daily/weekly/monthly with an optional end date; due occurrences are materialized on app start.
7. **Auto-capture (Android)** — payment notifications from finance apps are detected on-device and collected in an inbox as prefilled expenses you confirm with one tap. See the deep-dive below.
8. **Multi-currency** — pick a home currency (ISO 4217); every amount is stored and shown in it. When an auto-captured payment is in another currency, the app detects that on-device and offers a one-tap converted amount at save time (daily exchange rates), recording the original in the note. The conversion is a suggestion, never forced. See the deep-dive below.
9. **Widget** — a home-screen widget shows the current month's spend and a one-tap "add expense" (Jetpack Glance).
10. **Backup & restore** — export/import the full dataset as a JSON file via the system picker (save to Drive/Dropbox/local); OS auto-backup is the automatic safety net.
11. **On-device, no server** — all data and all parsing stay on the phone (the one outbound call is a keyless, daily exchange-rate fetch — no personal data sent).

## Project structure

* `/composeApp` — shared Compose Multiplatform code.
    - `commonMain` — code shared across all targets (UI, ViewModels, repositories, capture/backup logic, SQLDelight schema)
    - `androidMain`, `iosMain` — platform `actual`s (Android: ML Kit GenAI / Gemini Nano extractor, SAF backup, capture, widget; iOS: stubs)
* `/androidApp` — Android entry point. `MainActivity`, plus the manifest-declared `SpendNotificationListener` and `ExpenseTrackerWidgetReceiver` (see gotcha #1).
* `/iosApp` — iOS app entry point. Not generated yet; the shared framework and `MainViewController` are ready.

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html). Day-to-day development conventions are in [CLAUDE.md](CLAUDE.md).

## Build

Requires Java 21 (`.java-version`).

Android debug
```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
./gradlew :androidApp:assembleDebug
```

Android release (unsigned)
```bash
./gradlew :androidApp:assembleRelease
```

iOS
```Generate the iosApp Xcode project, then run from Xcode. The shared framework links via :composeApp:linkDebugFrameworkIosSimulatorArm64.```

## Documentation: Auto-capture

The headline feature. **Android-only by platform design** — iOS has no API to read other apps' notifications (a future iOS path would parse forwarded bank emails instead).

The model is **detect → notify → prefill → confirm**: the background never saves an expense or runs the LLM (Gemini Nano can't run in the background). It only records what it detects and nudges you; the expense is created in the foreground, on your tap.

### 1. The pipeline

| Stage | Responsibility |
| :--- | :--- |
| `SpendNotificationListener` | A `NotificationListenerService` (granted via system "Notification access"). Reads posted notifications, ignores its own; forwards the notification key + post time. |
| `NotificationCaptureProcessor` | Gates on (a) capture enabled and (b) the posting package being one the user explicitly chose to watch (`settings.capturePackages`; empty by default, so nothing is captured until the user opts apps in). Runs **regex only** — never guesses a category, never creates an expense. |
| `recordIfNew` | Inserts a `captured_notification` **inbox** row (amount + merchant from regex), deduped on notification key + amount. |
| `CaptureNotifications` | Posts one **isolated** push per detected transaction (notification id = the inbox row id; tapping cancels only itself, leaves the others). |

Tapping a push — or an item in the in-app **inbox** — deep-links (`DeepLinks` bus → `AndroidEntry` intents) into a **prefilled expense form**. The expense is created only when you tap **Save**, at which point the inbox row is linked to it (`expenseId`); re-opening a processed item edits that expense, never duplicates. The inbox lists every detection (unread highlighting, "mark all read") and is the fallback when `POST_NOTIFICATIONS` is denied.

### 2. Extraction strategy

Extraction differs by where it runs:

1. **Background — rules only.** `TransactionDetector` regex finds the amount, the merchant, and confirms it's an outgoing spend (skipping credits/refunds/salary). Fast, deterministic, and the only option in the background.
2. **Foreground — on-device LLM.** When AI is enabled and the model is available, the **ML Kit GenAI Prompt API** (Gemini Nano, via the system AICore) reads the raw notification in the prefilled form and extracts `{amount, merchant, category, currency}` itself — including cross-checking the figures (e.g. picking the spent amount over the available balance) and resolving the currency from whatever form the notification used (`S$` / `SGD` / `Rs`). AI-filled fields are marked with a sparkle and clear that mark once you edit them.
3. **Fallback.** When AI is off, unavailable, or the model isn't downloaded yet, the form uses the deterministic regex values instead. `MlKitGenAiExpenseExtractor` returns null on any failure and the caller falls back cleanly.

Availability is probed via ML Kit's `FeatureStatus`; the model downloads on demand (with progress) when AI is turned on in Settings. Everything runs on-device; no notification content leaves the phone.

### 3. Backup rationale (why it's simple)

Backup is deliberately **not** a programmatic Google Drive integration. That would need a Google Cloud OAuth client tied to the app's package + signing SHA-1, the Drive API enabled, and a consent screen — overkill for a no-server, share-with-everyone app, and a friction wall to distribute.

Instead, two zero-config layers:
- **Manual export/import via the Storage Access Framework.** The app writes `expense-tracker-backup-<date>.json` and hands it to the system file picker; the user saves it anywhere (their Drive, Dropbox, local) and imports it to restore. `BackupService` does the JSON (de)serialization over the SQLDelight database.
- **OS auto-backup.** `android:allowBackup="true"` lets Android back the app's data up to the user's account automatically and restore it on reinstall / new device, with no action.

### 4. Multi-currency & FX

The model is **display-currency only**: there is no per-expense currency column and no multi-currency ledger. Every amount is stored as 2-decimal minor units in the user's **home currency** (an ISO 4217 code in `SettingsStorage.defaultCurrencyCode`; symbol/precision derived via `CurrencyMeta`). The home-currency picker is restricted to 2-decimal currencies because the storage/display layer (`Money`) is 2-dp; foreign currencies of other precisions (JPY, KWD…) are still recognised as the *source* of a capture.

When an auto-captured payment is in a different currency, conversion is a **non-enforced, save-time suggestion**, not background work:
- The foreground LLM step returns the source `currency` alongside the amount. If it differs from the home currency, the amount field is left **blank** and a suggestion is offered; the user taps to accept the converted value (which also appends "Originally …" to the note) or types their own. A failed/late rate never blocks saving.
- Rates come from `FxRateService`: a single keyless GET to `frankfurter.dev` (ECB data) for the home currency as base, **cached once a day** (`FxRateCache`, a JSON blob), inverted per lookup, with a stale-cache fallback when offline. The conversion itself is `round(amountMinor × rate)` (`CurrencyConverter`) — both sides share the 2-dp-minor scale, so the math is precision-safe.
- Changing the home currency does **not** re-convert existing expenses (they keep their numbers, show the new symbol); the user is warned before the switch. The original notification text is snapshotted onto the expense (`expense.sourceNotificationText`) and shown as a reference on the form.

### 5. Platform gotchas

* **Manifest-declared Android components must live in `:androidApp`.** The `com.android.kotlin.multiplatform.library` plugin tree-shakes shared-library classes that nothing in app code references. A `Service`/`Receiver` referenced only by string in the merged manifest gets dropped from the APK → `ClassNotFoundException` when the system binds it. So `SpendNotificationListener` and `ExpenseTrackerWidgetReceiver` live in `:androidApp` and pull in the `:composeApp` logic they call. (Manifest *declarations* can live in either module — they merge — but the *class* must be in the app module.)
* **On-device AI = ML Kit GenAI Prompt API (`com.google.mlkit:genai-prompt`), running Gemini Nano via the system AICore.** It is **foreground-only** (background inference is blocked), present only on AICore-capable hardware (Pixel 8+, Galaxy S24/S25, and similar), and downloads its model on demand. `minSdk 26` (≤ the app's `29`), so no manifest override is needed; every call is runtime-guarded with the regex fallback. (The earlier experimental `com.google.ai.edge.aicore` SDK was dropped — it returned `NOT_AVAILABLE` on stock devices.)
* **`kotlin.time.Instant` / `Clock` still need `@OptIn(ExperimentalTime::class)`** under Kotlin 2.3.21. kotlinx-datetime 0.8 uses `.day` and `.month.number` (not `dayOfMonth`).
* **POST_NOTIFICATIONS** is requested at first launch (Android 13+) so the capture confirmations can show.
