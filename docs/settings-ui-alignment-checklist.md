# Settings UI alignment checklist (inset grouped / iOS-style kit)

Track completion against `MmrIosSettingsList.kt`, `MmrIosSettingsForm.kt`, `MmrIosSettingsPickers.kt`, `MmrSettingsScreenTemplate.kt`.

## High

- [x] **LAN sync** — Wi‑Fi error in inline error section; sync log via shared body/log row; Advanced as grouped nav row; trusted remove as destructive rows
- [x] **Web Manage** — Split server help (footer + dedicated section, not one dense control slot)
- [x] **Backup import** — Bottom sheet + choice rows (not centered dialog + FilterChips)
- [x] **Google Drive restore** — Bottom sheet + choice rows (not centered dialog + RadioButton)

## Medium

- [x] **Inline text rows** — Account + category + subcategory name/sort fields; LAN device name
- [x] **Type pickers** — Account type + category type via `MmrIosSettingsSingleChoiceSheet` (not FilterChip grid)
- [x] **Subcategory parent** — Read-only caption/value row (not empty `ValueRow`)

## Low

- [x] **List add action** — App bar add on Accounts/Categories (replace FAB `"+"`)
- [x] **Inset empty states** — Accounts/Categories empty in grouped helper section
- [x] **Status / busy** — `MmrSettingsStatusBanners` top; shared busy indicator (Backup, Drive, LAN)
- [x] **Google Drive setup** — No empty white card when unconfigured
- [x] **Color picker** — `MmrIosSettingsColorPickerSlot` in shared kit; category edit uses it

## Done when

All boxes checked; `compileDebugKotlin` passes; `PROJECT_MEMORY.md` updated.

**Status (2026-10-24):** Complete — `./gradlew :app:compileDebugKotlin` succeeded after final LAN/Web Manage pass.
