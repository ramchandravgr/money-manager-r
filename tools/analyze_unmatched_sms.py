import json
import re
import xml.etree.ElementTree as ET
from collections import Counter, defaultdict

import os

_REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
EXPORT = os.environ.get(
    "MMR_SMS_EXPORT",
    os.path.join(_REPO, "tools", "sms-exports", "sms-20261109004349.xml"),
)
TEMPLATES = os.path.join(_REPO, "app", "src", "main", "assets", "sms_templates.json")

SKIP = re.compile(
    r"\b(otp|one-time password|secret otp|pre-approved|statement is generated|login|"
    r"registration initiated|activated|wish you|reported your fund bal|passbook balance|"
    r"itr filing|tds by employer)\b",
    re.I,
)
TXN = re.compile(
    r"\b(spent|sent rs|debited|credited|payment of|received towards|refund|withdrawn|"
    r"transferred|purchase of|paid|credit alert)\b",
    re.I,
)


def java_regex(s: str) -> str:
    return re.sub(r"\(\?<(\w+)>", r"(?P<\1>", s)


def norm(b: str) -> str:
    b = re.sub(r"rs[:.]?\s*", "inr ", b, flags=re.I)
    return re.sub(r"\s+", " ", b.strip()).lower()


def sender_suffix(addr: str) -> str:
    a = (addr or "").upper()
    m = re.match(r"^[A-Z]{2}-(.+)$", a)
    if m:
        a = m.group(1)
    return re.sub(r"-[A-Z]$", "", a)


def sender_matches(key: str, suffixes: list[str]) -> bool:
    if not suffixes:
        return True
    return any(key == s or key.endswith(s) or s in key for s in suffixes)


def norm_sig(body: str) -> str:
    b = norm(body)
    b = re.sub(r"[0-9]+(?:,[0-9]+)*(?:\.[0-9]+)?", "#", b)
    b = re.sub(r"xx[0-9]{4}", "xx####", b)
    b = re.sub(r"[a-z0-9]{10,}", "<id>", b)
    return b[:140]


def load_patterns():
    with open(TEMPLATES, encoding="utf-8") as f:
        tpls = json.load(f)["templates"]
    tpls.sort(key=lambda t: -t["priority"])
    out = []
    for t in tpls:
        out.append(
            (
                t["id"],
                t.get("intent", "SETTLED"),
                [s.upper() for s in t.get("senderSuffixes", [])],
                re.compile(java_regex(t["bodyRegex"]), re.I),
            )
        )
    return out


def try_match(patterns, body: str, addr: str):
    nb = norm(body)
    sk = sender_suffix(addr)
    for tid, intent, suf, pat in patterns:
        if not sender_matches(sk, suf):
            continue
        if pat.search(nb):
            return tid, intent
    return None, None


VI_PROMO = re.compile(
    r"\b(special data gift|vi special gift|yayy! free|you got a bonus|1gb\s*/day)\b",
    re.I,
)
BHIM_COLLECT = re.compile(r"has requested money from you on your bhim app", re.I)


def main() -> None:
    patterns = load_patterns()
    unmatched = []
    txn_like = 0
    matched = 0
    for _, e in ET.iterparse(EXPORT, events=("end",)):
        if e.tag != "sms":
            continue
        body = e.get("body") or ""
        addr = e.get("address") or ""
        if not TXN.search(body) or SKIP.search(body):
            e.clear()
            continue
        txn_like += 1
        if VI_PROMO.search(body) or BHIM_COLLECT.search(body):
            e.clear()
            continue
        tid, intent = try_match(patterns, body, addr)
        if tid is not None:
            matched += 1
        else:
            unmatched.append((addr, body))
        e.clear()

    print("txn_like:", txn_like, "settled template matched:", matched)
    print("unmatched count (excl. vi/bhim promo):", len(unmatched))
    by_sender = Counter(sender_suffix(a) for a, _ in unmatched)
    print("by sender:", by_sender.most_common(20))
    sigs = Counter(norm_sig(b) for _, b in unmatched)
    print("\n=== TOP SIGNATURES ===")
    samples = defaultdict(list)
    for a, b in unmatched:
        s = norm_sig(b)
        if len(samples[s]) < 2:
            samples[s].append((sender_suffix(a), b[:500]))
    for sig, cnt in sigs.most_common(35):
        print(f"[{cnt}] {sig}")
        for suf, samp in samples[sig]:
            print(f"    ({suf}) {re.sub(r'\\s+', ' ', samp)[:400]}")
        print()


if __name__ == "__main__":
    main()
