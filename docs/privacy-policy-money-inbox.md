# Money Inbox — Privacy Policy

**Effective date:** 10 November 2026  

**Developer:** RR Parallel Studio  
**Contact:** [rrparallelstudio@gmail.com](mailto:rrparallelstudio@gmail.com)  
**App:** Money Inbox (package `com.rrparallelstudio.miapp`)

---

## Summary

Money Inbox is a **local-first** personal finance app. Your ledger, budgets, loans, and most settings stay **on your device**. Bank messages are **not** read from your SMS inbox on the Google Play version of the app. You **paste** text, or **optionally** point the app at an **XML export folder** you choose (for example from SMS Backup & Restore), so the app can suggest transactions for you to **approve** before anything is saved.

The app includes **Google Mobile Ads (AdMob)** and **Firebase** (Remote Config and Crashlytics) in the published build. **Advertising is off by default** and is only requested when a remote configuration flag is enabled. We do **not** operate servers that store your financial ledger.

---

## What data the app uses

### On your device (core use)

- **Ledger and finances:** transactions, accounts, categories, budgets, loans, assets, scheduled rules, and related data you enter or approve.
- **Bank and transaction messages you provide:** text you **paste**, or SMS rows read from an **XML file in a folder you select** via the system file picker (Storage Access Framework). Parsing and storage happen **on device** until you delete the data or uninstall the app.
- **Receipt photos:** images you attach to transactions, stored in app storage on your device.
- **App settings:** display preferences, parsers, optional feature toggles, and security settings (for example optional app lock), stored locally.

### Optional — Transaction scan (SMS Backup XML)

In **Settings → Transaction scan**, you may:

- Choose a **backup folder** on your device (written by another app such as SMS Backup & Restore).
- Set how many months of messages to consider.

The app reads **only files in that folder that you granted access to**. It does **not** use SMS read/receive permissions on the Play build. Imported message content is processed locally for the **Approval Center**; nothing is sent to RR Parallel Studio.

### Optional — Google Drive backup

If you **turn on** Google Drive backup in Settings:

- You sign in with **your** Google account through Google’s sign-in flow.
- The app may upload **signed backup archives** to **your own** Google Drive (for example under a Money Inbox / Backups folder you allow).
- [Google’s Privacy Policy](https://policies.google.com/privacy) applies to data in Google Drive and to sign-in.
- RR Parallel Studio does **not** receive a copy of your Drive backups on our servers.

### Optional — local network features

If you use **Peer Device Sync** or **Web Manage** on your Wi‑Fi:

- Ledger data (and optionally receipts) may be exchanged **only on your local network** between devices or browsers **you** connect.
- These paths use **local HTTP** on your Wi‑Fi unless you configure otherwise in the future; they are **not** sent to RR Parallel Studio.
- You should only use these features on networks you trust.

### Optional — local backups and export

If you export a backup or spreadsheet, **you** choose where the file is saved (Downloads, another app, cloud storage, etc.). RR Parallel Studio does not receive those files unless you send them to us yourself (for example by email for support).

---

## Third-party services in the app

The Play build may connect to **Google** services when network is available. We use them as described below. RR Parallel Studio does not sell your ledger to third parties.

### Google AdMob (advertising SDK)

The app **ships with the AdMob SDK** so it can show ads when enabled.

- **Default behaviour:** Firebase **Remote Config** parameter `ads_enabled` defaults to **false**. Unless that flag is turned on for your install, the app **does not initialise ad loading** and **does not show ad placements** (banner, medium rectangle, native) in the UI.
- **When ads are enabled:** Google may show ads in limited places (for example on the Home daily view and Trends) according to in-app placement rules. Google may use the **advertising ID** (where available and permitted on your device) and similar device data to deliver and measure ads, as described in [Google’s Privacy Policy](https://policies.google.com/privacy) and [How Google uses information from sites or apps that use our services](https://policies.google.com/technologies/partner-sites).
- **Ratings:** Ad requests are configured for a **general (T) audience** content rating in code.
- **Our role:** We do not receive your personal ledger from AdMob. Ad personalisation and retention are governed by **Google** and your device settings (including “Opt out of Ads Personalisation” where offered).

### Firebase Remote Config

Used to deliver configuration flags (for example **`ads_enabled`** and **`min_version_code`** for mandatory updates) from Firebase without requiring an app update. Fetches use your app’s Firebase project association and may include standard request metadata described in Google’s documentation. RR Parallel Studio does not use Remote Config to upload your transactions.

### Firebase Crashlytics

Used to collect **crash and stability reports** when the app is built with Firebase configuration (release builds from Play include this). Reports may include stack traces, device model, OS version, and diagnostic keys we set in code (for example app **version name**, **version code**, and **build type**) to help fix bugs. Reports are sent to **Google** under the Firebase terms. They are **not** designed to include your transaction amounts or SMS text; avoid sending sensitive content in manual bug reports unless you choose to contact us directly.

### Google Play services

Required for some Google APIs (for example sign-in and Drive when you opt in).

---

## What we do not do

- We do **not** read or receive your device’s **live SMS inbox** in the **Google Play** distribution (no SMS read/receive permission for inbox scanning).
- We do **not** sell your personal financial data.
- We do **not** require an account with RR Parallel Studio to use the core app.
- We do **not** host a central database of your transactions.

---

## Permissions (Play build)

Depending on features you use, the app may request or declare:

| Permission / declaration | Why |
| ------------------------ | --- |
| **Internet** | Optional Google Drive; Firebase Remote Config and Crashlytics; AdMob when ads are enabled |
| **Access network state** | Check connectivity before network work |
| **Advertising ID** (`AD_ID`) | Declared because the AdMob SDK is included; used by Google when ads are shown |
| **Notifications** | Reminders you enable (loans, recurring rules, assets, etc.) |
| **Wi‑Fi / multicast (local)** | Optional Peer Device Sync discovery on your LAN |

The Play build does **not** declare SMS read/receive for automatic inbox access.

---

## Your choices

- **Ads:** When `ads_enabled` is on, you may limit ad personalisation in **Android system settings** and through Google’s ad settings where available.
- **Drive, LAN, Web Manage, transaction scan:** All **optional**; do not enable them if you do not want those features.
- **Data on device:** Delete transactions, clear approvals, or uninstall the app to remove local data (subject to Android behaviour).
- **Drive copies:** Delete backup files in Google Drive yourself.

---

## Data retention and deletion

- **On device:** Deleted in-app data and uninstallation remove local app storage (subject to your device).
- **Google Drive:** Remains until you delete files in Drive or revoke the app’s access in your Google account.
- **Crashlytics / AdMob / Firebase:** Retention and deletion are handled under **Google’s** policies and consoles. RR Parallel Studio cannot delete Crashlytics events on Google’s servers on your behalf; contact us at the email below if you have questions.
- **Support email:** If you email us, we process what you send only to respond to you.

---

## Children

Money Inbox is not directed at children under 13 (or the age required in your region). We do not knowingly collect personal information from children. The app is intended for adults managing their own finances.

---

## International users

Your ledger is processed **on your device** unless you opt into Google Drive or enable network features above. If you use Google services, data may be processed in countries where Google operates, under Google’s terms. By using optional Google features, you agree to Google’s terms for those services.

Where law requires consent for advertising or analytics, you are responsible for ensuring your use complies; we configure ads to remain off until remotely enabled and configure ad content for a general audience.

---

## Security

Backups can be **signed** to detect tampering. Optional app lock (if enabled) uses your device’s lock/biometric mechanisms. LAN and Web Manage are **unencrypted HTTP on your local network** by design; use only on trusted networks.

---

## Changes to this policy

We may update this policy when the app changes (new features, SDK updates, or legal requirements). The **effective date** at the top will change when we publish an update. Continued use after an update means you accept the revised policy. For material changes, we may also notify you in-app or on the store listing where appropriate.

---

## Contact

Questions about this policy or Money Inbox: **rrparallelstudio@gmail.com**

---

## Publisher information

**RR Parallel Studio** develops Money Inbox. This policy applies to the Android app distributed on Google Play as **Money Inbox** (`com.rrparallelstudio.miapp`).

---

*This page is intended for Google Play and user reference. It is not legal advice.*
