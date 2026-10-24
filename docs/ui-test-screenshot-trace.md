# UI test screenshot trace

Instrumented UI tests capture **pre** and **post** PNG frames for every traced action.

## On device

Files are written under:

`Android/data/com.mmr.app/files/ui-test-trace/<runId>/<TestClass>/<testMethod>/`

Each folder contains:

- `0000_pre_<action>.png`, `0000_post_<action>.png`, …
- `trace.log` — step index, phase, action label, filename

## Pull to this PC

After `connectedDebugAndroidTest` (auto) or manually:

```powershell
$env:ANDROID_SERIAL="<device-serial>"   # wireless ADB if needed
.\gradlew :app:pullUiTestScreenshots
```

Output: `app/build/ui-test-trace/` (mirrors the device tree).

## Disable captures

In a test or `@Before`:

```kotlin
MmrUiScreenshotTrace.enabled = false
```

## API

Use traced helpers instead of raw `performClick` / `performTextInput`:

- `composeRule.traceClickTag(MmrTestTags.navHome)`
- `composeRule.traceClickText("Save")`
- `composeRule.traceTextInputTag(MmrTestTags.transactionAmount, "10")`
- `composeRule.traceAction("custom_step") { … }`
- `composeRule.traceWaitUntil("label") { condition }`
- `composeRule.traceTextClearanceTag(MmrTestTags.transactionNote)`
- `composeRule.tracePressBack()`
- `traceActivityRecreate()` (on `MmrHiltUiTest`)

`MmrHiltUiTest` installs `MmrUiTestTraceRule` automatically.
