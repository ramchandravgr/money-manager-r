"""Decrypt Monefy Android backup (AES-ECB) and dump summary + CSV exports."""
from __future__ import annotations

import argparse
import csv
import json
import sqlite3
from datetime import datetime, timezone
from pathlib import Path

from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes

MONEYFY_KEY = b"MyDifficultPassw"
AES_BLOCK = 16


def decrypt_backup(encrypted: Path, decrypted_db: Path) -> None:
    data = encrypted.read_bytes()
    if len(data) % AES_BLOCK != 0:
        raise ValueError(f"Encrypted size {len(data)} is not a multiple of AES block size")
    cipher = Cipher(algorithms.AES(MONEYFY_KEY), modes.ECB())
    decryptor = cipher.decryptor()
    decrypted_db.write_bytes(decryptor.update(data) + decryptor.finalize())
    magic = decrypted_db.read_bytes()[:16]
    if magic != b"SQLite format 3\x00":
        raise ValueError(f"Decrypted file is not SQLite (header={magic!r})")


def ms_to_iso(ms: int | None) -> str:
    if ms is None:
        return ""
    return datetime.fromtimestamp(ms / 1000.0, tz=timezone.utc).astimezone().isoformat()


def dump_schema(conn: sqlite3.Connection) -> dict:
    cur = conn.cursor()
    cur.execute("SELECT name, sql FROM sqlite_master WHERE type='table' ORDER BY name")
    tables = []
    for name, sql in cur.fetchall():
        cur.execute(f'SELECT COUNT(*) FROM "{name}"')
        count = cur.fetchone()[0]
        cur.execute(f'PRAGMA table_info("{name}")')
        cols = [row[1] for row in cur.fetchall()]
        tables.append({"name": name, "count": count, "columns": cols, "sql": sql})
    return {"tables": tables}


def export_transactions_csv(conn: sqlite3.Connection, out_csv: Path) -> int:
    cur = conn.cursor()
    cur.execute(
        """
        SELECT
            t._id,
            t.amountCents,
            t.createdOn,
            t.note,
            a.title AS account,
            c.title AS category,
            c.categoryType AS entry_type,
            t.deletedOn
        FROM transactions t
        LEFT JOIN accounts a ON a._id = t.account_id
        LEFT JOIN categories c ON c._id = t.category_id
        ORDER BY t.createdOn
        """
    )
    rows = cur.fetchall()
    headers = [d[0] for d in cur.description]
    with out_csv.open("w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(headers + ["amount", "createdOn_iso"])
        for row in rows:
            cents = row[1] or 0
            created = row[2]
            w.writerow(list(row) + [cents / 1000.0, ms_to_iso(created)])
    return len(rows)


def export_transfers_csv(conn: sqlite3.Connection, out_csv: Path) -> int:
    cur = conn.cursor()
    cur.execute(
        """
        SELECT
            tr._id,
            tr.amountCents,
            tr.createdOn,
            tr.note,
            af.title AS account_from,
            at.title AS account_to,
            tr.deletedOn
        FROM transfers tr
        LEFT JOIN accounts af ON af._id = tr.accountFrom
        LEFT JOIN accounts at ON at._id = tr.accountTo
        ORDER BY tr.createdOn
        """
    )
    rows = cur.fetchall()
    headers = [d[0] for d in cur.description]
    with out_csv.open("w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(headers + ["amount", "createdOn_iso"])
        for row in rows:
            cents = row[1] or 0
            w.writerow(list(row) + [cents / 1000.0, ms_to_iso(row[2])])
    return len(rows)


def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument("backup", type=Path)
    p.add_argument("--out-dir", type=Path, default=Path("docs/samples/monefy_extracted"))
    args = p.parse_args()

    args.out_dir.mkdir(parents=True, exist_ok=True)
    db_path = args.out_dir / "monefy_decrypted.db"
    decrypt_backup(args.backup, db_path)

    conn = sqlite3.connect(db_path)
    conn.row_factory = sqlite3.Row
    schema = dump_schema(conn)
    (args.out_dir / "schema.json").write_text(json.dumps(schema, indent=2), encoding="utf-8")

    tx_count = export_transactions_csv(conn, args.out_dir / "transactions.csv")
    xfer_count = export_transfers_csv(conn, args.out_dir / "transfers.csv")

    sample_cur = conn.cursor()
    sample_cur.execute(
        """
        SELECT t.amountCents, t.createdOn, t.note, a.title, c.title, c.categoryType
        FROM transactions t
        LEFT JOIN accounts a ON a._id = t.account_id
        LEFT JOIN categories c ON c._id = t.category_id
        WHERE t.deletedOn IS NULL
        ORDER BY t.createdOn DESC
        LIMIT 5
        """
    )
    recent = []
    for r in sample_cur.fetchall():
        cents = r[0] or 0
        recent.append(
            {
                "amountCents": cents,
                "amount": cents / 1000.0,
                "createdOn_iso": ms_to_iso(r[1]),
                "note": r[2],
                "account": r[3],
                "category": r[4],
                "entry_type": r[5],
            }
        )
    conn.close()

    summary = {
        "source_backup": str(args.backup),
        "decrypted_db": str(db_path),
        "format": "AES-128-ECB (key from Monefy APK) over SQLite",
        "table_counts": {t["name"]: t["count"] for t in schema["tables"]},
        "transactions_exported": tx_count,
        "transfers_exported": xfer_count,
        "recent_transactions_sample": recent,
    }
    (args.out_dir / "summary.json").write_text(json.dumps(summary, indent=2), encoding="utf-8")
    print(json.dumps(summary, indent=2))


if __name__ == "__main__":
    main()
