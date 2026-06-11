# Privacy Policy — Expense Tracker

_Last updated: 11 June 2026_

Expense Tracker is a **local-first** spending tracker. Your financial data lives on your device
and is processed on your device. This policy explains exactly what the app does and does not do
with your information.

## Summary

- **We do not run a server.** Your expenses, categories, and the text of any payment
  notifications you let the app read never leave your device by our doing.
- **No accounts, no sign-in, no advertising, no data selling.**
- The app makes exactly **two** kinds of outbound connection, both described below: a daily
  exchange-rate lookup, and anonymous crash diagnostics. Neither carries your financial data.

## What data the app handles, and where it stays

- **Expenses you enter or confirm** (amount, category, note, date) are stored only in a private
  database on your device.
- **Payment notifications** (optional feature — see below): when you grant notification access,
  the app reads incoming notifications from payment apps you choose, on-device, to suggest an
  expense for you to confirm. The matched notification text is stored locally alongside the
  expense you confirm. It is **never transmitted off the device**.
- **On-device AI:** payment notifications are parsed entirely on-device (Gemini Nano via ML
  Kit, with an on-device rules fallback). No notification content is sent to any AI service or
  server.

## The only data that leaves your device

1. **Currency exchange rates.** To suggest a converted amount for a foreign-currency payment,
   the app fetches public exchange-rate data once per day from `frankfurter.dev` (European
   Central Bank data). This request contains **no personal or financial data** — only the
   currency codes being looked up.
2. **Anonymous crash diagnostics.** To find and fix crashes, the official release (Play Store)
   build uses Google Firebase Crashlytics. If the app crashes, a diagnostic report (stack trace,
   device model, OS version, and an anonymous installation identifier) is sent to Crashlytics.
   These reports **do not contain your expenses or any notification content**, and are not linked
   to your identity. Google's handling of this data is governed by the
   [Firebase Crashlytics terms](https://firebase.google.com/terms/crashlytics) and
   [Google's Privacy Policy](https://policies.google.com/privacy).

## Permissions we request, and why

- **Notification access** (`BIND_NOTIFICATION_LISTENER_SERVICE`): optional; only to read payment
  notifications you opt into so the app can suggest expenses. You can deny or revoke it at any
  time in system settings; the rest of the app works without it.
- **Internet:** for the daily exchange-rate lookup and crash diagnostics above.
- **Post notifications:** to show you the expense suggestions you confirm.
- **Query installed apps / boot completed:** to let you pick which payment apps to watch and to
  keep recurring expenses up to date.

## Backups

If you enable Android's system backup, the app's data is included in your device's
encrypted backup to your own Google account, and restored when you set up a new device. This is
controlled by you and Android — we never receive a copy. You can also export and import your
data yourself as a file you control.

## Your choices

- Use the app without notification access — it stays fully functional.
- Revoke notification access or any permission at any time in system settings.
- Crash diagnostics are sent only by the release build and carry no personal or financial data;
  there is no in-app opt-out. To send none at all, uninstall the app, which stops all processing.
- Export your data to a file, or clear it, at any time from within the app.

## Children

The app is not directed at children and does not knowingly collect data from children.

## Changes

We may update this policy as the app evolves. Material changes will be reflected here with a new
"Last updated" date.

## Contact

Questions about this policy: **jyotman94@gmail.com**
