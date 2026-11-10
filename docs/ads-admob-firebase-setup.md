# AdMob + Firebase (Remote Config & Crashlytics)

## Plan (implementation order)

1. **Gradle** — Firebase BOM, Crashlytics, Remote Config, Play Services Ads; `google-services` + Crashlytics plugins when `app/google-services.json` exists.
2. **Manifest** — AdMob `APPLICATION_ID` (from `local.properties` / test default); `AD_ID` permission.
3. **Remote Config** — `ads_enabled` (default **false**), `min_version_code` (default **0** = no block); **one network fetch per local calendar day** on release (`MmrRemoteConfigDailyFetchStore` + SDK `minimumFetchIntervalInSeconds` = 86_400). Cached values apply all day; **next day** cold start or bringing the app to the foreground runs fetch again if due. Debug SDK interval remains **0** but the same daily gate applies.
4. **AdMob** — Init only when `ads_enabled`; **max content rating T**; unit IDs from `BuildConfig` (test IDs if unset).
5. **UI** — Wire `MmrAdSlot` to loaded creatives (banner / MREC / native); keep placement rules in `MmrAdPlacementPolicy`.
6. **Crashlytics** — `MmrCrashlytics` policy + verification. See **[`docs/crashlytics.md`](crashlytics.md)**.

## Firebase console (you)

**Project (2026-11-09):** **Money Inbox** — `money-inbox-156b5` — Android app `com.rrparallelstudio.miapp` registered; Remote Config **`ads_enabled`** published **`false`**. Crashlytics page is active (first crash after SDK + `google-services.json`).

Full URL + click-path reference: **`PROJECT_MEMORY.md`** → §6 **Firebase, AdMob & related console URLs**.

### Quick links (Money Inbox)

| Step | URL |
| --- | --- |
| Overview | https://console.firebase.google.com/project/money-inbox-156b5/overview |
| Download `google-services.json` | https://console.firebase.google.com/project/money-inbox-156b5/settings/general/android:com.rrparallelstudio.miapp |
| Remote Config | https://console.firebase.google.com/project/money-inbox-156b5/config/env/firebase |
| Crashlytics | https://console.firebase.google.com/project/money-inbox-156b5/crashlytics/app/android:com.rrparallelstudio.miapp/issues |
| AdMob (TODO) | https://admob.google.com/ |

### Checklist

1. Create / open Firebase project → add Android app `com.rrparallelstudio.miapp`.
2. Download **`app/google-services.json`** into `app/` (not committed; use `google-services.json.example` as reminder).
3. Remote Config → parameter **`ads_enabled`** = `false` until launch (then `true` when ready).
4. Remote Config → parameter **`min_version_code`** (Number) = **`0`** until you need a mandatory update; then set to the **lowest allowed `versionCode`** (same formula as `app/build.gradle.kts`) and **Publish**. Installs below that see a blocking **Update required** screen (`MmrAppVersionPolicy`).
5. Enable **Crashlytics** in Firebase console.
6. AdMob → link to Firebase; create app + ad units; put IDs in `local.properties` (see below).

## local.properties (optional overrides)

```properties
ADMOB_APP_ID=ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy
ADMOB_UNIT_HOME_BANNER=ca-app-pub-.../...
ADMOB_UNIT_HOME_MREC=ca-app-pub-.../...
ADMOB_UNIT_STATS_NATIVE=ca-app-pub-.../...
```

If omitted, **Google test IDs** are used (safe for debug; do not use test IDs for production traffic).

## Operational

- **No ad UI** while loading, on error, when Remote Config off, or when load fails.
- Bump `MmrLedgerYearBounds` / placements only in code; unit IDs are server-side via AdMob + optional Remote Config later.
