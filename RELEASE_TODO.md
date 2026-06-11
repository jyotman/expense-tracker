# Release TODO — manual & browser steps

The codebase is staged for a Play Store release (R8/minify on, ProGuard rules, backup rules,
release log gating, Crashlytics wiring, privacy policy). The items below are the steps that
**can't be done from code** — they need the Firebase console, the Play Console, a keystore, or a
hosting location. Crashlytics stays inert until step 1 is done; everything else in the app builds
and runs without it.

## 1. Firebase / Crashlytics (browser)
- [ ] Create a Firebase project (or reuse one) and add an Android app with package
      `app.expensetracker`. Optionally add `app.expensetracker.debug` too if you want debug
      crashes reported (debug collection is currently off, so not required).
- [ ] Enable **Crashlytics** in the Firebase console.
- [ ] Download **`google-services.json`** and place it at `androidApp/google-services.json`.
      → As soon as this file exists, the Gradle build auto-applies the google-services +
        Crashlytics plugins (conditional apply in `androidApp/build.gradle.kts`) and crash
        reporting goes live in release builds. No code change needed.
- [ ] Decide whether to commit `google-services.json`. It contains no secrets (only public
      project IDs); nourge commits it per build variant. If you commit it, no `.gitignore`
      change is needed. If not, add it to `.gitignore`.

## 2. Verify Crashlytics works
- [ ] `./gradlew :androidApp:assembleRelease`, install, force a test crash, confirm it appears
      in the Crashlytics dashboard within a few minutes.
- [ ] Confirm the release stack trace is **deobfuscated** (mapping upload is enabled for release).

## 3. Signing (IntelliJ + keystore)
- [ ] Sign the release **App Bundle** via IntelliJ → Build → Generate Signed App Bundle / APK,
      using the existing upload keystore `~/keystores/playstore-upload-keystore.jks`
      (reusing it across apps is fine — Play App Signing re-signs each app with its own key).
- [ ] Note: `./gradlew assembleRelease` produces a **debug-signed** APK for local R8 testing
      only — do **not** upload that artifact. Upload the wizard-signed `.aab`.

## 4. Play Console (browser)
- [ ] Create the app listing for `app.expensetracker`.
- [ ] **Data safety form:** declare "Crash logs" / "Diagnostics" as collected, **not** linked to
      identity, **not** shared with third parties (Crashlytics is a processor). Mark collection as
      **required** (no in-app opt-out — matches PRIVACY.md). Declare no other data collected
      (expenses/notifications never leave the device).
- [ ] **Privacy policy URL:** host `PRIVACY.md` somewhere public (GitHub Pages, a static site,
      or a gist rendered page) and paste the URL. Confirm the contact email in `PRIVACY.md`.
- [ ] **Sensitive permissions:** complete the notification-access / `QUERY_ALL_PACKAGES`
      declarations with the justification (read user-selected payment notifications on-device to
      create expenses; never transmitted).

## 5. Store listing assets (not in repo)
- [ ] App icon — already in the project.
- [ ] Feature graphic (1024×500), phone screenshots (min 2), short + full description.

## Notes / decisions deferred
- Versioning: first release is `versionCode 1` / `versionName 0.1`. Bump `versionCode` for every
  subsequent upload.
- No CI/CD, detekt/ktlint, or fastlane — intentionally consistent with nourge (solo, manual
  release). Revisit if the project grows.
