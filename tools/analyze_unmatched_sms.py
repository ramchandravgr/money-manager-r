import json
import re
import xml.etree.ElementTree as ET
from collections import Counter, defaultdict

EXPORT = r"D:\Development\MMR\sms-20261022133803.xml"
TEMPLATES = r"D:\Development\MMR\app\src\main\assets\sms_templates.json"

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


def main() -> None:
    patterns = load_patterns()
    unmatched = []
    for _, e in ET.iterparse(EXPORT, events=("end",)):
        if e.tag != "sms":
            continue
        body = e.get("body") or ""
        addr = e.get("address") or ""
        if not TXN.search(body) or SKIP.search(body):
            e.clear()
            continue
        tid, intent = try_match(patterns, body, addr)
        if tid is None:
            unmatched.append((addr, body))
        e.clear()

    print("unmatched count:", len(unmatched))
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
