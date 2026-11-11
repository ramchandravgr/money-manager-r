#!/usr/bin/env python3
"""Offline probe for Realbyte .mmbak — mirrors MoneyManagerMmbakDatabaseOpener logic."""

from __future__ import annotations

import io
import sqlite3
import sys
import tempfile
import zipfile
from pathlib import Path

MAX_UNCOMPRESSED_BYTES = 64 * 1024 * 1024

TXN_TABLES = ("INOUTCOME", "ZINOUTCOME", "inoutcome")
ASSET_TABLES = ("ASSETS", "ZASSET", "ZASSETS", "ASSET", "assets")
CAT_TABLES = ("ZCATEGORY", "ZCATEGORY2", "CATEGORY", "category")

UID_COLS = ("UID", "ZUID", "C_UID", "ID", "ZID")
DATE_COLS = ("WDATE", "ZWDATE", "wdate", "ZDATE", "DATE", "date", "ZTIME")
MONEY_COLS = ("ZMONEY", "ZZMONEY", "AMOUNT", "ZAMOUNT", "MONEY", "money")
TYPE_COLS = ("DO_TYPE", "ZDO_TYPE", "TYPE", "ZTYPE", "inoutType")
IS_DEL_COLS = ("IS_DEL", "C_IS_DEL", "ZIS_DEL", "isDel", "ZISDEL", "ZISDELETED")


def is_db_zip_name(name: str) -> bool:
    lower = name.lower()
    return lower.endswith((".db", ".sqlite", ".sqlite3", ".mmbak"))


def pick_zip_member(zf: zipfile.ZipFile) -> str:
    best_db = None
    best_db_size = -1
    best_any = None
    best_any_size = -1
    for info in zf.infolist():
        if info.is_dir():
            continue
        size = info.file_size
        if size > best_any_size:
            best_any_size = size
            best_any = info.filename
        if is_db_zip_name(info.filename) and size > best_db_size:
            best_db_size = size
            best_db = info.filename
    return best_db or best_any or ""


def extract_db_bytes(data: bytes) -> tuple[bytes, str]:
    if data[:2] == b"PK":
        with zipfile.ZipFile(io.BytesIO(data)) as zf:
            member = pick_zip_member(zf)
            if not member:
                raise ValueError("empty zip")
            info = zf.getinfo(member)
            if info.file_size > MAX_UNCOMPRESSED_BYTES:
                raise ValueError("db too large")
            return zf.read(member), f"zip:{member}"
    if data[:16].startswith(b"SQLite format"):
        return data, "raw-sqlite"
    raise ValueError("not zip or sqlite")


def first_present(candidates: tuple[str, ...], available: set[str]) -> str | None:
    upper = {c.upper(): c for c in available}
    for key in candidates:
        if key in available:
            return key
        found = upper.get(key.upper())
        if found:
            return found
    return None


def resolve_table(con: sqlite3.Connection, candidates: tuple[str, ...]) -> str | None:
    for name in candidates:
        row = con.execute(
            "SELECT name FROM sqlite_master WHERE type='table' AND name=? COLLATE NOCASE LIMIT 1",
            (name,),
        ).fetchone()
        if row:
            return row[0]
    return None


def column_names(con: sqlite3.Connection, table: str) -> list[str]:
    rows = con.execute(f'PRAGMA table_info("{table.replace(chr(34), chr(34)*2)}")').fetchall()
    return [r[1] for r in rows]


def parse_date_value(raw: object) -> int:
    import datetime as dt

    if raw is None:
        return 0
    if isinstance(raw, (int, float)):
        n = int(raw)
        if n >= 1_000_000_000_000:
            return n
        if n >= 1_000_000_000:
            return n * 1000
        if 19_000_101 <= n <= 99_991_231:
            s = str(n).zfill(8)
            y, m, d = int(s[:4]), int(s[4:6]), int(s[6:8])
            return int(dt.datetime(y, m, d).timestamp() * 1000)
        return n
    text = str(raw).strip()
    if not text:
        return 0
    for fmt in ("%Y-%m-%d %H:%M:%S", "%Y-%m-%d", "%Y/%m/%d %H:%M:%S", "%Y/%m/%d"):
        try:
            return int(dt.datetime.strptime(text, fmt).timestamp() * 1000)
        except ValueError:
            continue
    if text.lstrip("-").isdigit():
        return parse_date_value(int(text))
    return 0


def probe(path: Path) -> None:
    data = path.read_bytes()
    print(f"\n=== {path.name} ({len(data)} bytes) ===")
    try:
        db_bytes, source = extract_db_bytes(data)
    except Exception as e:
        print(f"OPEN FAIL: {e}")
        return
    print(f"Container: {source}, db size {len(db_bytes)}")

    with tempfile.NamedTemporaryFile(suffix=".db", delete=False) as tmp:
        tmp.write(db_bytes)
        tmp_path = tmp.name

    con = sqlite3.connect(f"file:{tmp_path}?mode=ro", uri=True)
    try:
        tables = [r[0] for r in con.execute("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name")]
        print(f"Tables ({len(tables)}): {', '.join(tables[:20])}{'...' if len(tables) > 20 else ''}")

        txn_table = resolve_table(con, TXN_TABLES)
        asset_table = resolve_table(con, ASSET_TABLES)
        cat_table = resolve_table(con, CAT_TABLES)
        print(f"Resolved txn={txn_table!r} asset={asset_table!r} category={cat_table!r}")

        if not txn_table:
            print("PARSE: no transaction table — 0 rows importable")
            return

        cols = set(column_names(con, txn_table))
        uid = first_present(UID_COLS, cols)
        date_c = first_present(DATE_COLS, cols)
        money = first_present(MONEY_COLS, cols)
        typ = first_present(TYPE_COLS, cols)
        is_del = first_present(IS_DEL_COLS, cols)
        print(f"Columns: uid={uid} date={date_c} money={money} type={typ} is_del={is_del}")
        if not all([uid, date_c, money, typ]):
            print("PARSE: missing required columns — 0 rows importable")
            print(f"All columns: {sorted(cols)}")
            return

        q = lambda c: f'"{c}"'
        where = ""
        if is_del:
            where = f" WHERE ({q(is_del)} IS NULL OR {q(is_del)} = 0)"
        sql = f"SELECT {q(uid)}, {q(date_c)}, {q(money)}, {q(typ)} FROM {q(txn_table)}{where}"
        rows = con.execute(sql).fetchall()
        print(f"Rows in backup (non-deleted): {len(rows)}")

        type_counts: dict[int, int] = {}
        min_d = max_d = None
        importable = 0
        for uid_v, date_v, _amt, do_type in rows:
            dt = int(do_type) if str(do_type).lstrip("-").isdigit() else -1
            type_counts[dt] = type_counts.get(dt, 0) + 1
            if dt in (0, 1):
                importable += 1
            norm = parse_date_value(date_v)
            min_d = norm if min_d is None else min(min_d, norm)
            max_d = norm if max_d is None else max(max_d, norm)

        print(f"DO_TYPE counts: {dict(sorted(type_counts.items()))}")
        print(f"Importable income/expense (do_type 0/1): {importable}")
        if min_d is not None:
            from datetime import datetime, timezone

            def fmt(ms: int) -> str:
                return datetime.fromtimestamp(ms / 1000, tz=timezone.utc).strftime("%Y-%m-%d")

            print(f"Date range (normalized UTC): {fmt(min_d)} .. {fmt(max_d)}")
            raw_sample = rows[0][1]
            print(f"Sample raw WDATE={raw_sample!r} -> normalized ms={parse_date_value(raw_sample)}")

        # Old vs new zip picker
        if data[:2] == b"PK":
            with zipfile.ZipFile(io.BytesIO(data)) as zf:
                members = [i for i in zf.infolist() if not i.is_dir()]
                first_db = next(
                    (i for i in members if is_db_zip_name(i.filename)),
                    None,
                )
                largest = max(members, key=lambda i: i.file_size)
                picked = pick_zip_member(zf)
                print(f"ZIP members: {len(members)}")
                for i in members[:8]:
                    print(f"  - {i.filename} ({i.file_size} bytes)")
                if first_db and picked != first_db.filename:
                    print(
                        f"NOTE: first .db was {first_db.filename!r} ({first_db.file_size} B); "
                        f"code now picks {picked!r} ({zf.getinfo(picked).file_size} B)",
                    )
    finally:
        con.close()
        Path(tmp_path).unlink(missing_ok=True)


def main() -> None:
    for arg in sys.argv[1:] or ["tools/mmbak-samples/oneplus-latest.mmbak"]:
        probe(Path(arg))


if __name__ == "__main__":
    main()
