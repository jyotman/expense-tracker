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
- **FX rates:** `frankfurter.dev` (keyless ECB data) via **Ktor**, cached once/day on-device (`FxRateCache`) — powers the multi-currency save-time conversion suggestion; the only outbound network call in the app
- **Backup:** Storage Access Framework export/import (JSON) + OS auto-backup — no server, no cloud SDK
- **Build config:** BuildKonfig. **No Firebase** (dropped vs nourge — not needed, avoids `google-services.json`)

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
  commonTest/kotlin/app/expensetracker/    # shared tests (parsing/matching live here)
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

# Tests (parsing/matching unit tests live in commonTest, run as Android host tests)
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
4. `./gradlew :composeApp:testAndroidHostTest` (or `:composeApp:verifyCommonMainExpenseTrackerDatabaseMigration`) must pass. There are **no `.sqm` files yet** — the current 4-table schema is the `1.db` baseline.

**On-device only.** Notification parsing and AI run entirely on-device; nothing is sent to a server. Backup writes a user-controlled file — never auto-upload to a remote endpoint.

## Testing

Pure logic (amount extraction, spend/income detection, category matching) lives in testable objects (`TransactionDetector`, `CategoryMatcher`) with tests in `commonTest`. Express platform-dependent logic as pure functions taking the platform value as a parameter so it can be tested in `commonTest`.

No Compose UI tests or Android instrumented tests are wired up — flag before adding, they're non-trivial.

## Working norms

- Ask before making big assumptions — small clarifications are cheaper than rework.
- Keep changes scoped. No drive-by refactors or speculative abstractions.
- When something gets long enough to deserve its own doc (architecture deep-dive, runbook), put it in a separate `.md` and link from here rather than growing this file.
