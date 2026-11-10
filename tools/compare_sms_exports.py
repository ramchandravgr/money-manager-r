"""Compare two SMS Backup & Restore XML exports against sms_templates.json."""

from __future__ import annotations

import argparse
import os
import sys
from collections import Counter, defaultdict

_REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
if _REPO not in sys.path:
    sys.path.insert(0, os.path.dirname(__file__))

from analyze_unmatched_sms import (  # noqa: E402
    BHIM_COLLECT,
    SKIP,
    TXN,
    VI_PROMO,
    load_patterns,
    norm_sig,
    sender_suffix,
    try_match,
)
import xml.etree.ElementTree as ET

_DEFAULT_A = os.path.join(_REPO, "tools", "sms-exports", "sms-20261109004349.xml")
_DEFAULT_B = os.path.join(_REPO, "tools", "sms-exports", "oneplus-sms-20261109131524.xml")


def audit_export(path: str, label: str, patterns) -> dict:
    total_sms = 0
    txn_like = 0
    promo_skipped = 0
    matched = 0
    by_template: Counter[str] = Counter()
    by_intent: Counter[str] = Counter()
    unmatched_sigs: Counter[str] = Counter()
    unmatched_by_sender: Counter[str] = Counter()
    sig_samples: dict[str, tuple[str, str]] = {}

    for _, e in ET.iterparse(path, events=("end",)):
        if e.tag != "sms":
            continue
        total_sms += 1
        body = e.get("body") or ""
        addr = e.get("address") or ""
        if not TXN.search(body) or SKIP.search(body):
            e.clear()
            continue
        txn_like += 1
        if VI_PROMO.search(body) or BHIM_COLLECT.search(body):
            promo_skipped += 1
            e.clear()
            continue
        tid, intent = try_match(patterns, body, addr)
        if tid is not None:
            matched += 1
            by_template[tid] += 1
            by_intent[intent or "SETTLED"] += 1
        else:
            sig = norm_sig(body)
            unmatched_sigs[sig] += 1
            unmatched_by_sender[sender_suffix(addr)] += 1
            if sig not in sig_samples:
                sig_samples[sig] = (sender_suffix(addr), body.replace("\n", " ")[:220])
        e.clear()

    unmatched = sum(unmatched_sigs.values())
    audited = txn_like - promo_skipped
    return {
        "label": label,
        "path": path,
        "total_sms": total_sms,
        "txn_like": txn_like,
        "promo_skipped": promo_skipped,
        "audited": audited,
        "matched": matched,
        "unmatched": unmatched,
        "by_template": by_template,
        "by_intent": by_intent,
        "unmatched_sigs": unmatched_sigs,
        "unmatched_by_sender": unmatched_by_sender,
        "sig_samples": sig_samples,
    }


def pct(num: int, den: int) -> str:
    if den == 0:
        return "—"
    return f"{100.0 * num / den:.1f}%"


def print_section(title: str) -> None:
    print()
    print("=" * len(title))
    print(title)
    print("=" * len(title))


def compare(a: dict, b: dict) -> None:
    print_section("SUMMARY")
    rows = [
        ("Total SMS", a["total_sms"], b["total_sms"]),
        ("Txn-like (keyword filter)", a["txn_like"], b["txn_like"]),
        ("Vi/BHIM promo skipped", a["promo_skipped"], b["promo_skipped"]),
        ("Audited (txn-like − promo)", a["audited"], b["audited"]),
        ("Template matched", a["matched"], b["matched"]),
        ("Unmatched", a["unmatched"], b["unmatched"]),
    ]
    print(f"{'Metric':<28} {a['label']:>12} {b['label']:>12}")
    for name, va, vb in rows:
        print(f"{name:<28} {va:>12} {vb:>12}")
    print()
    print(
        f"Match rate (on audited):  {pct(a['matched'], a['audited']):>8}   "
        f"{pct(b['matched'], b['audited']):>8}"
    )

    all_tpl = a["by_template"] + b["by_template"]
    only_a = set(a["by_template"]) - set(b["by_template"])
    only_b = set(b["by_template"]) - set(a["by_template"])
    print_section("TEMPLATES (top 15 by combined hits)")
    print(f"{'template_id':<36} {a['label']:>10} {b['label']:>10}")
    for tid, _ in all_tpl.most_common(15):
        print(f"{tid:<36} {a['by_template'][tid]:>10} {b['by_template'][tid]:>10}")
    if only_a:
        print(f"\nTemplates only firing on {a['label']}: {', '.join(sorted(only_a)[:12])}" + (
            "…" if len(only_a) > 12 else ""
        ))
    if only_b:
        print(f"Templates only firing on {b['label']}: {', '.join(sorted(only_b)[:12])}" + (
            "…" if len(only_b) > 12 else ""
        ))

    sigs_a = set(a["unmatched_sigs"])
    sigs_b = set(b["unmatched_sigs"])
    shared = sigs_a & sigs_b
    only_sigs_a = sigs_a - sigs_b
    only_sigs_b = sigs_b - sigs_a
    shared_msgs = sum(a["unmatched_sigs"][s] + b["unmatched_sigs"][s] for s in shared)

    print_section("UNMATCHED SIGNATURES")
    print(f"Distinct unmatched sigs:  {len(sigs_a)} (S25)  {len(sigs_b)} (OP)  shared: {len(shared)}")
    print(f"Unmatched messages in shared sigs: {shared_msgs} (both exports combined)")
    print(f"S25-only sig patterns: {len(only_sigs_a)}  |  OnePlus-only: {len(only_sigs_b)}")

    def top_only(export: dict, only_set: set[str], title: str, n: int = 12) -> None:
        print(f"\n--- {title} (top {n} by count on that export) ---")
        ranked = [(s, export["unmatched_sigs"][s]) for s in only_set]
        ranked.sort(key=lambda x: -x[1])
        for sig, cnt in ranked[:n]:
            suf, sample = export["sig_samples"].get(sig, ("?", ""))
            print(f"[{cnt:4d}] ({suf}) {sig}")
            print(f"       {sample[:200]}")

    top_only(a, only_sigs_a, f"Unmatched only on {a['label']}")
    top_only(b, only_sigs_b, f"Unmatched only on {b['label']}")

    print(f"\n--- Shared unmatched sigs (top 10 by S25 count) ---")
    shared_ranked = sorted(shared, key=lambda s: -a["unmatched_sigs"][s])
    for sig in shared_ranked[:10]:
        ca, cb = a["unmatched_sigs"][sig], b["unmatched_sigs"][sig]
        suf, _ = a["sig_samples"].get(sig) or b["sig_samples"].get(sig, ("?", ""))
        print(f"[S25 {ca:3d} | OP {cb:3d}] ({suf}) {sig[:120]}")

    senders_a = set(a["unmatched_by_sender"])
    senders_b = set(b["unmatched_by_sender"])
    print_section("UNMATCHED SENDERS (suffix)")
    print(
        f"In unmatched only on {a['label']}: {len(senders_a - senders_b)} senders  "
        f"(top: {', '.join(x for x, _ in (a['unmatched_by_sender'] - b['unmatched_by_sender']).most_common(8))})"
    )
    print(
        f"In unmatched only on {b['label']}: {len(senders_b - senders_a)} senders  "
        f"(top: {', '.join(x for x, _ in (b['unmatched_by_sender'] - a['unmatched_by_sender']).most_common(8))})"
    )


def main() -> None:
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--a", default=_DEFAULT_A, help="Export A (default S25)")
    p.add_argument("--b", default=_DEFAULT_B, help="Export B (default OnePlus)")
    p.add_argument(
        "--label-a",
        default="S25",
        help="Short label for export A",
    )
    p.add_argument("--label-b", default="OnePlus", help="Short label for export B")
    p.add_argument("-o", "--out", help="Also write report to this file")
    args = p.parse_args()

    patterns = load_patterns()
    report_a = audit_export(args.a, args.label_a, patterns)
    report_b = audit_export(args.b, args.label_b, patterns)

    import io
    from contextlib import redirect_stdout

    buf = io.StringIO()
    with redirect_stdout(buf):
        compare(report_a, report_b)
    text = buf.getvalue()
    print(text, end="")
    if args.out:
        with open(args.out, "w", encoding="utf-8") as f:
            f.write(text)
        print(f"\nWrote {args.out}")


if __name__ == "__main__":
    main()
