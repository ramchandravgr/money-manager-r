# Budgets home-screen widget — row layout (for review)

**Scope:** Live Glance widget only. **Widget picker previews are frozen** — do not change preview XML / `providePreview` for this work.

**Default size:** 3×1 → **180×40 dp** min (launcher formula: 70×cells−30). Resizes **3×1 … 5×6** (8 responsive breakpoints, platform max 16).

---

## What went wrong in the last tweak (build on device now)

| Change | Intended effect | Side effect |
|--------|-----------------|-------------|
| `6dp` spacer after category | Push Month / % right | **Steals 6dp from category** → more truncation |
| Tighter period / % mins (38 / 34 dp) | More category room | **Period** can clip on “Period” budgets |
| Label row `Vertical.Top` | Avoid bar overlap | Text rides **high** in the slot; bar still feels cramped |
| Extra `4dp` above progress bar | Clearance | Shrinks label band on **short** 3×1 rows |

**Your screenshot before that tweak:** Month ↔ 0% spacing looked good; category was the main pain (ellipsis). **After:** category got worse.

---

## Proposed row design (Option A — recommended)

One horizontal **label band**, then **gap**, then **full-width progress track**.

### 3×1 inner width (~168 dp after 6 dp widget padding)

```
┌──────────────────────────────────────────────────────────────────────────┐
│ 6dp pad                                                                  │
│  ┌────────────────────────────┐  ┌──────┐ │ ┌────┐ │ ┌─────┐            │
│  │ Food & Dining              │  │ 8dp  │ │Month│ │ 79% │  ← label band │
│  │ (ellipsis if needed)       │  │ gap  │ │     │ │     │    ~18–20dp   │
│  └────────────────────────────┘  └──────┘ │ └────┘ │ └─────┘            │
│                              ↑            ↑        ↑                     │
│                         category      period    percent                  │
│                         (remainder)   40dp fix  36dp fix                 │
│  ························· 8dp text → bar gap ·························  │
│  ████████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  progress 6dp       │
│ 6dp pad                                                                  │
└──────────────────────────────────────────────────────────────────────────┘
```

### Column rules (fixed right block — category gets the rest)

| Zone | Width | Alignment | Notes |
|------|-------|-----------|--------|
| **Category** | `inner − gap − separators − period − percent` | Start | Single line, ellipsize end |
| **Gap** | **8 dp** | — | Between category and first divider (pushes Month/% right **without** shrinking category below math) |
| **Divider** | 1 dp + **4 dp** margin each side | — | Same as today between Month and % |
| **Period** | **40 dp** fixed | Center | Fits `Period`; `Month` / `Week` / `Year` centered |
| **Divider** | same | — | Unchanged spacing you liked |
| **%** | **36 dp** fixed | End | Fits `100%` |

**Separators total:** 2 × (1 + 4 + 4) = **18 dp**.

**Example @ 168 dp inner:** category ≈ 168 − 8 − 18 − 40 − 36 = **66 dp** (wider than current build with spacer + tight mins).

### Vertical rhythm (per row slot)

| Element | 3×1 (tight) | Taller resize (scale up with slot) |
|---------|-------------|----------------------------------|
| Label band height | **18 dp** min, text **vertically centered** | Grows with slot; type scales via existing `rememberBudgetsWidgetTextScale` |
| Text → bar gap | **8 dp** | 8–10 dp |
| Progress bar | **6 dp** height, full width | Same |
| Row slot | `innerHeight / rowCount` when rows fit; else scroll | Unchanged |

**No** `defaultWeight()` on category (Glance clips trailing columns). **No** extra spacer that is also subtracted from category width.

---

## Option B (alternative — if you want % even tighter on the edge)

Same as A, but **% column 32 dp** and **period 38 dp** → **+6 dp** to category. Use only if `100%` still fits at your font scale on device.

---

## Option C (revert spacing tweak only)

Keep **current** period / % mins and **remove** spacer + top alignment + extra bar gap; only add **8 dp** category→period gap **included in width math** (replace spacer pattern). Minimal diff from “when Month/% spacing looked good.”

---

## What we need from you

Reply with **A**, **B**, or **C**, and any tweaks:

- Gap before Month: **6 / 8 / 10 dp**
- Text→bar gap: **6 / 8 / 10 dp**
- Period column: **38 / 40 / 44 dp**

Open **`docs/budget-widget-row-mockup.html`** in a browser for a rough visual (not pixel-perfect Glance).

---

## Implementation checklist (after approval)

- [ ] Revert harmful bits from last tweak (spacer + top align)
- [ ] Implement chosen option in `MmrBudgetWidgetRowLayout.kt` + `BudgetWidgetRow`
- [ ] Do **not** touch preview assets or `MmrWidgetPreviewRegistrar`
- [ ] Keep `budgetsResponsive` ≤ 8 sizes (RemoteViews limit)
