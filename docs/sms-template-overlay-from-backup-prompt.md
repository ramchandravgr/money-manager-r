# SMS template overlay from SMS Backup & Restore XML

Use this when you want **custom parsing rules** for bank SMS that Money Inbox does not match yet. You upload your **SMS Backup & Restore** export (`sms-YYYYMMDDHHMMSS.xml`) to any chat agent, paste the prompt below, then import the JSON the agent returns in the app.

**In Money Inbox:** Settings → Approvals → **Transaction scan** → **SMS templates** (or direct **SMS template overlay** if linked) → **Import overlay JSON**.

The app merges your file on top of built-in templates (same `id` **replaces** a built-in rule). At most **120** overlay templates; invalid regex or duplicate overlay `id` values are rejected.

---

## Copy this prompt into your chat agent

Paste everything in the fenced block below, then **attach or upload** your `sms-*.xml` file in the same message.

````
You are helping me build an SMS template overlay JSON file for the Android app **Money Inbox** (package `com.rrparallelstudio.miapp`).

## Input

I will upload an SMS Backup & Restore XML file. Each message is an `<sms>` element with attributes:
- `address` — sender (e.g. `VM-HDFCBK`, `HDFCBK-S`, `AX-AXISBK`)
- `body` — full message text
- `date` — epoch milliseconds

Ignore OTP/login/promo/statement-only messages unless they are clearly money movement.

## Your job

1. Parse the XML and find **transaction-like** SMS that look like spends, UPI sends, credits, card spends, ATM, refunds, etc.
2. Group unmatched messages by **sender suffix** + **normalized body signature** (replace amounts with `#`, card masks like `xx1234` with `xx####`, long alphanumeric ids with `<id>`).
3. For each recurring pattern (at least 2–3 similar messages), write **one** template that matches the pattern on **normalized** text (see normalization rules).
4. Prefer **fewer, broader** regexes over dozens of near-duplicates. Cap output at **80 templates** (hard app limit 120).
5. Output **only** valid JSON—no markdown fence, no commentary before or after the JSON.

## JSON schema (required)

Root object:

```json
{
  "version": 1,
  "templates": [ … ]
}
```

Each template object:

| Field | Required | Values / notes |
|-------|----------|----------------|
| `id` | Yes | Unique snake_case, start with bank/channel hint, e.g. `hdfc_upi_sent_custom` |
| `senderSuffixes` | Yes | Array of uppercase sender keys **without** `XX-` prefix and **without** trailing `-S` / `-G` where applicable. Match how Indian DLT addresses appear (e.g. `HDFCBK`, `HDFCBN`, `AXISBK`). Empty array `[]` = match any sender (avoid unless necessary). |
| `priority` | Yes | Integer 1–150; **higher runs first**. Use 70–110 for bank-specific; 40–60 for generic fallbacks. |
| `bodyRegex` | Yes | Java-compatible regex (NOT PCRE `(?P<name>)`; use `(?<name>…)` named groups). Max 4000 chars. Applied **case-insensitive** to **normalized** body (lowercase, see below). |
| `transactionType` | Yes | `EXPENSE` or `INCOME` |
| `paymentChannel` | No | `UPI`, `CARD`, `NEFT`, `NACH`, `WALLET`, `NET_BANKING`, `UNKNOWN` — omit or empty if unclear |
| `instrument` | No | `SAVINGS`, `CREDIT_CARD`, `MEAL_WALLET`, `FUEL_WALLET`, `UNKNOWN` |
| `intent` | No | Default `SETTLED` (creates approval). Use `CARD_PAYMENT` for credit-card bill payment SMS only (do not double-count spend). Use `CARD_REFUND` for merchant refunds to card. Use `INVESTMENT` for NPS/EPFO/MF notices. Use `MANDATE` / `STATEMENT` for non-ledger notices. |
| `amountGroup` | No | Named group for amount; default `amount` |
| `merchantGroup` | No | Named group for payee/merchant; default `merchant`. Use `""` if no merchant. |
| `last4Group` | No | Named group for account/card last 4; default `last4`. Use `""` if none. |
| `refGroup` | No | UPI/bank reference; default `ref`. Use `""` if none. |
| `balanceGroup` | No | Available balance; default `balance`. Use `""` if none. |

## Normalization (match regex against this style of text)

Before regex runs, the app:
- Lowercases text
- Replaces `₹` and standalone `Rs.` / `Rs:` / `rs ` (not inside words like “reversed”) with `inr `
- Collapses all whitespace (including newlines) to single spaces

So your `bodyRegex` should assume **lowercase** and often use `inr\\s+` instead of `rs\\.?`.

## Regex tips

- Amount: `(?<amount>[0-9]+(?:,[0-9]+)*(?:\\.[0-9]{1,2})?)`
- Card/account last 4: `(?<last4>[0-9]{4})` with prefixes like `xx`, `x`, `\\*`, `a/c x`
- Escape backslashes in JSON: `\\s`, `\\.`, `\\*`
- Use non-greedy merchant captures where needed: `(?<merchant>.+?)`
- Test mentally: regex must **not** match OTP (“otp”, “one-time password”, “do not share”)

## Sender matching

The app compares `senderSuffixes` to the sender after:
- Stripping leading `XX-` country/operator prefix
- Uppercasing
- Partial suffix match (e.g. `HDFCBK` matches `VM-HDFCBK-S`)

## Skip these (do not create SETTLED templates)

- OTP, login, KYC-only, EMI reminders with no new money movement you already track
- “Statement generated”, “passbook balance” summaries
- Telecom promos (Vi/Airtel data gifts) unless they are wallet top-ups with clear amount
- Duplicate patterns already covered by obvious generic HDFC/Axis/ICICI/UPI phrases—focus on **gaps** in my export

## Example template (structure only)

```json
{
  "id": "union_mob_bk_debit",
  "senderSuffixes": ["UNIONB", "UNIONBK"],
  "priority": 88,
  "bodyRegex": "a/c \\*[0-9]+ debited for inr\\s+(?<amount>[0-9]+(?:,[0-9]+)*(?:\\.[0-9]{1,2})?)\\s+on .*?mob bk",
  "transactionType": "EXPENSE",
  "paymentChannel": "UPI",
  "instrument": "SAVINGS"
}
```

## Deliverable

1. A short bullet list (max 15 lines): top senders you targeted, how many template groups, what you intentionally skipped.
2. Then the full `version` + `templates` JSON object only, pretty-printed, valid UTF-8.

Begin by analyzing my uploaded XML.
````

---

## After you get the JSON

1. Save as `.json` (e.g. `my_sms_templates_overlay.json`).
2. Money Inbox → **SMS templates** → **Import overlay JSON**.
3. Open **Approval Center** and tap **Refresh** (with Transaction scan folder set) or paste a test SMS manually to verify.

If import fails, the app shows validation errors (bad regex, duplicate `id`, too many templates). Fix those entries and re-import.

## Optional: local check (developers)

From the repo root, compare your export against built-in templates:

```bash
set MMR_SMS_EXPORT=path\to\your\sms-20261110120000.xml
python tools/analyze_unmatched_sms.py
```

Use unmatched signatures as hints for what the chat agent should target.

## Privacy

SMS exports contain financial and personal data. Only upload to agents you trust, or run a **local** model. Delete the chat and file when done.
