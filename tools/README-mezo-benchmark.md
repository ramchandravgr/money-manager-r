# Mezo ledger vs MMR parser benchmark

Offline regression against Mezo `AAAA1` rows (not a gold standard).

## Refresh Mezo export (after emulator summarization)

```powershell
$sqlite = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\sqlite3.exe"
$db = "C:\Users\madur\Downloads\Mezo\segregation_inspect_post_summary\bugle_db"
& $sqlite -header -csv $db @"
SELECT a._id AS mezo_id, a.a_8 AS amount, a.a_21 AS date_ms, a.a_4 AS sender_id,
  a.a_10 AS type_code, m.address,
  replace(replace(p.text, char(10), ' '), char(13), ' ') AS body
FROM AAAA1 a
LEFT JOIN messages m ON m._id = a.a_2
LEFT JOIN parts p ON p.message_id = m._id
GROUP BY a._id;
"@ | Out-File -Encoding utf8 tools\mezo_ledger_export.csv

Copy-Item tools\mezo_ledger_export.csv app\src\test\resources\mezo_ledger_export.csv -Force
```

## Run comparison

```powershell
cd D:\Development\MMR
.\gradlew :app:testDebugUnitTest --tests com.mmr.core.parser.MezoLedgerParserComparisonTest
```

Report: `tools/mezo_mmr_gap_report.txt`

**Interpretation**

- Mezo **informational** rows (mostly CC bill payments) are intentional on MMR (`CARD_PAYMENT`).
- **Ignored** rows should shrink with parser fixes; OTP-for-purchase SMS may remain ignored (Mezo false positives).
