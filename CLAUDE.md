# Expense Tracker

A minimal, local-first **spending** tracker (spends only — no income), Android-first. Its differentiator: on Android, payment notifications from bank/wallet/card apps are auto-converted into categorized expenses on-device. See [README.md](README.md) for product features, the auto-capture architecture deep-dive, the backup rationale, and platform gotchas.

## Tech stack

Kotlin Multiplatform (Android + iOS) with Compose Multiplatform for UI. One shared codebase, platform-specific implementations only when unavoidable. Stack and conventions follow `../nourge/nourge-app`.

Key libraries (see `gradle/libs.versions.toml` for versions):
- **UI:** Compose Multiplatform, Material3 (dynamic color on Android 12+), `material-icons-extended`, `navigation-compose`, `vico` (charts)
- **State:** AndroidX Lifecycle ViewModel + StateFlow
- **Local DB:** `SQLDelight` — `ExpenseTrackerDatabase`, schema in `composeApp/src/commonMain/sqldelight/app/expensetracker/db/*.sq`. The one deviation from nourge (which is key-value only)
- **Prefs:** `multiplatform-settings` (`SettingsStorage`)
- **On-device AI:** Gemini Nano via the **ML Kit GenAI Prompt API** (`com.google.mlkit:genai-prompt`, foreground-only), with a regex rules fallback. (Replaced the experimental `com.google.ai.edge.aicore` SDK, which was unreachable on stock devices.)
- **Widget:** Jetpack Glance
- **FX rates:** `frankfurter.dev` (keyless ECB data) via **Ktor**, cached once/day on-device (`FxRateCache`) — powers the multi-currency save-time conversion suggestion. This and crash diagnostics are the app's only two outbound network calls; neither carries financial data or notification content
- **Crash reporting:** Firebase **Crashlytics** (release builds only; anonymous diagnostics, no expenses/notification content). Staged so it stays inert until a `google-services.json` is added — the Firebase Gradle plugins are applied *conditionally* on that file's presence (`androidApp/build.gradle.kts`), so the repo builds without it. See `RELEASE_TODO.md`
- **Backup:** Storage Access Framework export/import (JSON) + OS auto-backup — no server, no cloud SDK

## Project layout

```
composeApp/src/                    # Shared KMP library (namespace app.expensetracker.shared)
  commonMain/kotlin/app/expensetracker/   # 95% of code lives here
    ui/            # screens/ (one file per screen) + components/, theme/, CategoryVisuals
    viewmodel/     # one ViewModel per screen + PeriodScopedViewModel base + PeriodController (shared month selection)
    repository/    # ExpenseRepository, CategoryRepository, RecurringRepository (SQLDelight Flows)
    storage/       # SettingsStorage (multiplatform-settings wrapper)
    data/          # domain models, Money (minor units), CurrencyMeta + CurrencyConverter (multi-currency), TimePeriod, DefaultCategories
    db/            # Database holder (driver injected per platform)
    capture/       # ExpenseExtractor (expect; ParsedExpense carries LLM-detected currencyCode), TransactionDetector, CategoryMatcher, CaptureRules (currency/keyword/package data), RulesExpenseExtractor, GenAiResponse (LLM JSON parsing)
    fx/            # FxRateService + FxRateCache (daily-cached FX rates from frankfurter.dev, for the save-time conversion suggestion)
    backup/        # BackupService (JSON export/restore over the DB)
    platform/      # PlatformCapabilities, Backup (expect)
    navigation/    # Routes, DeepLinks bus
  commonMain/sqldelight/app/expensetracker/db/   # .sq schema + .sqm migrations (Category, Expense, RecurringRule, CapturedNotification)
  commonMain/sqldelight/databases/      # committed schema snapshots (1.db, …) for migration verification
  androidMain/kotlin/app/expensetracker/   # actuals: ML Kit GenAI (Gemini Nano) extractor, SAF backup, capture processor, Glance widget
  iosMain/kotlin/app/expensetracker/       # MainViewController, iOS actuals (no-op stubs where the platform can't)
  commonTest/kotlin/app/expensetracker/    # shared tests (parsing/matching/Flow logic live here)
  androidHostTest/kotlin/app/expensetracker/  # JVM-only tests that need platform drivers (e.g. SQLite JDBC for DB-backed repo tests)
androidApp/                        # Android entry point (applicationId app.expensetracker)
  src/main/kotlin/app/expensetracker/MainActivity.kt, ExpenseTrackerApp.kt
  src/main/kotlin/app/expensetracker/capture/SpendNotificationListener.kt   # manifest-declared — see README gotcha
  src/main/kotlin/app/expensetracker/widget/ExpenseTrackerWidgetReceiver.kt          # manifest-declared — see README gotcha
iosApp/                            # NOT generated yet — shared framework + MainViewController are ready
gradle/libs.versions.toml          # single source of truth for versions
```

## Day-to-day commands

Run from repo root. Requires Java 21 (`.java-version`). **Do not run a full build — it is expensive.**

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home

# Android — APK in :androidApp (debug applicationId is app.expensetracker.debug)
./gradlew :androidApp:assembleDebug

# iOS — verify shared code compiles for the simulator target
./gradlew :composeApp:compileKotlinIosSimulatorArm64

# Tests — runs commonTest + androidHostTest together
./gradlew :composeApp:testAndroidHostTest
./gradlew :composeApp:testAndroidHostTest --tests "app.expensetracker.capture.*"
```

## Conventions

**Multiplatform first.** Write everything in `commonMain` unless a platform API genuinely requires `expect`/`actual`. Before adding `actual` files, ask whether a common abstraction exists.

**Manifest-declared Android components live in `:androidApp`.** The KMP-library plugin tree-shakes shared-library classes nothing references in code, so a `Service`/`Receiver` referenced only by string in the manifest is dropped from the APK. Keep such components in `:androidApp`; they pull in the `:composeApp` logic they call. (Full explanation in README.)

**Material3 + native components first.** Latest Material3, dynamic color on Android 12+. Reach for the platform/library component before building a custom one. Theme tokens live in `ui/theme/`.

**Money & currency.** Amounts are stored as `Long` minor units in the user's **home currency**, set as an ISO 4217 code (`SettingsStorage.defaultCurrencyCode`; symbol/precision derived via `CurrencyMeta`, format via `Money`). The home currency is always 2-decimal (the picker is restricted to 2-dp currencies), so `Money`'s 2-decimal assumption holds. There is **no per-expense currency column** — when an auto-captured payment is in a different currency, the foreground LLM detects it and the form offers a **non-enforced save-time conversion suggestion** (`FxRateService` daily rates → `CurrencyConverter`); the converted amount is stored in the home currency and the original is appended to the note. Changing the home currency does not re-convert existing expenses (user is warned). The originating notification is snapshotted onto `expense.sourceNotificationText`.

**Reactive data.** Repositories expose SQLDelight queries as Flows (`asFlow().mapToList`); ViewModels `combine` them and `stateIn`. The shown month is global via `PeriodController`.

**Database schema & migrations.** The `.sq` files are the canonical *current* schema (`CREATE TABLE` lives there). Versioned schema snapshots are committed under `composeApp/src/commonMain/sqldelight/databases/` (`1.db`, `2.db`, …) and `verifyMigrations = true` makes the build fail if the `.sq` schema and the `.sqm` migration chain disagree — so an app update can never ship a missing/wrong migration and crash on existing installs. The check runs on `:composeApp:check` and is also wired into `testAndroidHostTest`. **Whenever you change a `.sq` schema** (add/alter/drop a table or column):
1. Edit the `.sq` to the new desired schema.
2. Add a migration `composeApp/src/commonMain/sqldelight/app/expensetracker/db/<N>.sqm`, where `<N>` is the version you're migrating *from* (e.g. `1.sqm` = v1→v2) — `ALTER TABLE …` / `CREATE TABLE …` for the delta. The runtime version is `(highest .sqm number) + 1`; the driver auto-runs `Schema.migrate` on upgrade (no driver code change).
3. Regenerate the snapshot: `./gradlew :composeApp:generateCommonMainExpenseTrackerDatabaseSchema` (writes the new `<N+1>.db`); commit it.
4. `./gradlew :composeApp:testAndroidHostTest` (or `:composeApp:verifyCommonMainExpenseTrackerDatabaseMigration`) must pass.

**On-device first.** Notification parsing and AI run entirely on-device; expenses and notification content never leave the device. The only outbound calls are the daily FX-rate fetch and anonymous crash diagnostics (Crashlytics, release only) — neither carries financial data or notification content. Release builds gate Kermit to `Severity.Info` (`AppInitializer.android.kt`) so app internals never reach logcat in production. Backup writes a user-controlled file — never auto-upload to a remote endpoint.

**Notification capture keyword tiers.** `isLikelySpend` uses three tiers in `CaptureRules`: `blockPhrases` (hard reject — OTPs, declined, bill-due reminders) → `strongSpendKeywords` (win even with income words present) → `weakSpendKeywords` (lose to `strongIncomeKeywords`). All keyword matching is word-boundary aware, so inflections are listed explicitly (e.g. `"charge"` does not cover `"charges"`). Both `"transfer"` and `"transferred"` are weak spend signals: because capture is review-to-confirm (a false positive is one inbox dismiss, a miss is an untracked spend), recall beats precision, so a bare "X transferred to your account" is captured even though it may be incoming — a genuine credit ("salary … transferred") is still suppressed by the strong-income tier. `"cashback"` is intentionally absent from income keywords — banks append it to payment confirmations. Two further layers reject promo false-positives: `promoKeywords` (when marketing copy is present, only a *currency-tagged* amount counts as a spend — a bare number is promo math like "500 bonus miles") and `plausibleAmount` in `TransactionDetector` (percentages, advertised "20 off" discounts, date ranges/times, and digits glued into larger tokens are never amounts). Extend `CaptureRules` when a bank's format is missed; verify with `TransactionDetectorTest`.

## Testing

**What to test and where:**
- `commonTest` — pure logic, ViewModel state flows, repository Flows, storage round-trips. Runs on JVM via `testAndroidHostTest`. The vast majority of tests live here.
- `androidHostTest` — tests that need a JVM-only driver, currently: `ExpenseRepository` tests backed by an in-memory SQLite JDBC driver (`JdbcSqliteDriver(IN_MEMORY)`). Add tests here only when `commonTest` can't cover it (i.e. you genuinely need a real DB or JVM-specific platform class).

**Patterns in use:**
- **Turbine** — test `Flow`/`StateFlow` emissions with `.test { awaitItem() }`. Use for any reactive query or observable state.
- **`runTest` + `TestDispatcher`** — wrap coroutine tests in `runTest`. Pass `Dispatchers.Unconfined` to repository constructors in tests so emissions are synchronous.
- **`MapSettings`** — from `multiplatform-settings-test`; use instead of real `SettingsStorage` to avoid touching shared state between tests.
- **Constructor injection** — repositories and services take their dependencies as constructor params with production defaults (e.g. `ExpenseRepository(db, dispatcher)`). Tests pass fakes/in-memory variants. ViewModels currently use `ServiceLocator` directly and are harder to unit-test in isolation; prefer testing at the repository/logic layer instead.
- **`launchIo {}`** — shorthand defined in `ViewModelExt.kt` for `viewModelScope.launch(PlatformCapabilities.ioDispatcher)`. Use it for all blocking I/O in ViewModels instead of raw `launch`.

**What's not wired up:** No Compose UI tests or Android instrumented tests. The app is still in active feature development — screens change shape too often for UI tests to pay off yet. Revisit when the screen surfaces stabilize; the right path at that point is Robolectric + `compose-ui-test-junit4`, which runs on JVM inside `testAndroidHostTest` without an emulator.

## Working norms

- Ask before making big assumptions — small clarifications are cheaper than rework.
- Keep changes scoped. No drive-by refactors or speculative abstractions.
- When something gets long enough to deserve its own doc (architecture deep-dive, runbook), put it in a separate `.md` and link from here rather than growing this file.
