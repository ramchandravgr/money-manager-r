import re
import xml.etree.ElementTree as ET
from collections import Counter, defaultdict

path = r"D:\Development\MMR\sms-20261022133803.xml"

TXN = re.compile(
    r"\b(spent|sent rs|debited|credited|payment of|received towards|refund|withdrawn|transferred|purchase of|paid)\b",
    re.I,
)
SKIP = re.compile(
    r"\b(otp|one-time password|secret otp|pre-approved|preapproved|statement is|e-statement|login|registration initiated|activated|maintenance|wish you|offer|download|inactive|blocked for incoming|reported your fund bal|passbook balance|itr filing|tds by employer|emi conversion for your|not been used for over|transaction limits)\b",
    re.I,
)


def sender_suffix(addr: str) -> str:
    a = (addr or "").upper()
    m = re.match(r"^[A-Z]{2}-(.+)$", a)
    if m:
        a = m.group(1)
    return re.sub(r"-[A-Z]$", "", a)


def norm_sig(body: str) -> str:
    b = body.lower()
    b = re.sub(r"[0-9]+(?:,[0-9]+)*(?:\.[0-9]+)?", "#", b)
    b = re.sub(r"\*{1,2}[0-9]{4}", "*####", b)
    b = re.sub(r"xx[0-9]{4}", "xx####", b, flags=re.I)
    b = re.sub(r"[a-z0-9]{10,}", "<id>", b)
    b = re.sub(r"\s+", " ", b).strip()
    return b[:150]


def main() -> None:
    buckets: Counter[str] = Counter()
    samples: dict[str, list[tuple[str, str]]] = defaultdict(list)

    for _event, elem in ET.iterparse(path, events=("end",)):
        if elem.tag != "sms":
            continue
        body = elem.get("body") or ""
        addr = elem.get("address") or ""
        if not TXN.search(body) or SKIP.search(body):
            elem.clear()
            continue
        sig = norm_sig(body)
        buckets[sig] += 1
        suf = sender_suffix(addr)
        if len(samples[sig]) < 2:
            samples[sig].append((suf, body[:550]))
        elem.clear()

    print("Distinct transaction signatures:", len(buckets))
    print("Total transaction-like (filtered):", sum(buckets.values()))
    print()
    print("=== TOP 45 PATTERNS BY FREQUENCY ===")
    for sig, cnt in buckets.most_common(45):
        print(f"[{cnt:3d}] {sig}")
        for suf, samp in samples[sig]:
            t = re.sub(r"\s+", " ", samp)[:420]
            print(f"      ({suf}) {t}")
        print()


if __name__ == "__main__":
    main()
