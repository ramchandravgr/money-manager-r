#!/usr/bin/env python3
"""
Rewrite a Money Inbox signed backup (.mibxbac / .mmrbackup) so appId matches the current package.

After the rename to com.rrparallelstudio.miapp, imports reject archives whose manifest and
backup.json still say com.mmr.app. You cannot fix this by editing the zip by hand: any change
to backup.json changes payloadSha256 and breaks the HMAC signature in manifest.json.

This script:
  1. Replaces "appId" in backup.json (top-level string field).
  2. Recomputes payloadSha256 and payloadUncompressedSize in manifest.json.
  3. Recomputes signature using the same algorithm as BackupIntegrity.kt (HmacSHA256 over
     the canonical signing payload).

Default OLD_APP_ID: com.mmr.app
Default NEW_APP_ID: com.rrparallelstudio.miapp (must match BackupJsonCodec.EXPECTED_APP_ID)

Requires: Python 3.8+

Example (copy file from device first):
  python scripts/migrate_backup_app_id.py old.mibxbac new.mibxbac
  # or replace in place:
  python scripts/migrate_backup_app_id.py --in-place old.mibxbac
  adb push new.mibxbac /sdcard/Download/update/
"""

from __future__ import annotations

import argparse
import base64
import hashlib
import hmac
import json
import shutil
import sys
import zipfile
from pathlib import Path

# Must match app/src/main/java/com/rrparallelstudio/miapp/core/backup/BackupIntegrity.kt
INTEGRITY_KEY = b"com.rrparallelstudio.miapp.backup.integrity.v1"
SIGN_ALGO = hashlib.sha256

DEFAULT_OLD = "com.mmr.app"
DEFAULT_NEW = "com.rrparallelstudio.miapp"

ENTRY_MANIFEST = "manifest.json"
ENTRY_PAYLOAD = "backup.json"
ATTACHMENTS_DIR = "attachments/"


def sha256_hex(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def sign(payload_sha256: str, attachment_hashes: list[tuple[str, str]]) -> str:
    """Same as BackupIntegrity.sign — Base64 HMAC over canonical string."""
    parts = [payload_sha256]
    for sync_id, h in sorted(attachment_hashes, key=lambda x: x[0]):
        parts.append(f"{sync_id}:{h}")
    canonical = "\n".join(parts)
    mac = hmac.new(INTEGRITY_KEY, canonical.encode("utf-8"), SIGN_ALGO)
    return base64.b64encode(mac.digest()).decode("ascii")


def migrate(src: Path, dst: Path, old_id: str, new_id: str) -> None:
    if not zipfile.is_zipfile(src):
        sys.exit(f"Not a zip backup: {src}")

    with zipfile.ZipFile(src, "r") as zin:
        names = zin.namelist()
        if ENTRY_MANIFEST not in names or ENTRY_PAYLOAD not in names:
            sys.exit("Missing manifest.json or backup.json — not a Money Inbox signed archive.")

        manifest_raw = zin.read(ENTRY_MANIFEST)
        payload_raw = zin.read(ENTRY_PAYLOAD)

        manifest = json.loads(manifest_raw.decode("utf-8"))
        payload_obj = json.loads(payload_raw.decode("utf-8"))

        current_app = payload_obj.get("appId", "")
        if current_app != old_id:
            if current_app == new_id:
                print(f"appId is already {new_id}; rewriting manifest checksums/signature only.")
            else:
                sys.exit(
                    f"backup.json appId is {current_app!r}, expected {old_id!r}. "
                    "Use --old-id if your export used a different package string.",
                )

        payload_obj["appId"] = new_id
        # Pretty-print like BackupJsonCodec.encode (toString(2)) — size differs from minified JSON.
        new_payload_str = json.dumps(payload_obj, indent=2, ensure_ascii=False)
        new_payload_bytes = new_payload_str.encode("utf-8")

        manifest["appId"] = new_id
        manifest["payloadSha256"] = sha256_hex(new_payload_bytes)
        manifest["payloadUncompressedSize"] = len(new_payload_bytes)

        attachment_hashes: list[tuple[str, str]] = []
        att_array = manifest.get("attachments") or []
        if not isinstance(att_array, list):
            sys.exit("manifest attachments is not an array")

        attachments_to_copy: list[tuple[str, bytes]] = []
        for item in att_array:
            path = item["path"]
            sync_id = item["syncId"]
            expected_sha = item["sha256"]
            if not str(path).startswith(ATTACHMENTS_DIR):
                sys.exit(f"Unexpected attachment path: {path}")
            data = zin.read(path)
            if sha256_hex(data) != expected_sha:
                sys.exit(f"Attachment SHA mismatch for {path} — archive corrupt or tampered.")
            attachment_hashes.append((sync_id, expected_sha))
            attachments_to_copy.append((path, data))

        new_sig = sign(manifest["payloadSha256"], attachment_hashes)
        manifest["signature"] = new_sig

        new_manifest_bytes = json.dumps(manifest, indent=2, ensure_ascii=False).encode("utf-8")

    dst.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(dst, "w", compression=zipfile.ZIP_DEFLATED) as zout:
        zout.writestr(ENTRY_MANIFEST, new_manifest_bytes)
        zout.writestr(ENTRY_PAYLOAD, new_payload_bytes)
        for path, data in attachments_to_copy:
            zout.writestr(path, data)

    print(f"Wrote {dst}")
    print("Verify: import from Backup & Restore in the new app.")


def main() -> None:
    p = argparse.ArgumentParser(description="Rewrite backup appId and re-sign .mibxbac archive.")
    p.add_argument("input", type=Path, help="Source .mibxbac or .mmrbackup")
    p.add_argument(
        "output",
        type=Path,
        nargs="?",
        help="Destination path (required unless --in-place)",
    )
    p.add_argument("--old-id", default=DEFAULT_OLD, help=f"Former appId (default {DEFAULT_OLD})")
    p.add_argument("--new-id", default=DEFAULT_NEW, help=f"Current appId (default {DEFAULT_NEW})")
    p.add_argument("--in-place", action="store_true", help="Replace input via temp file (careful)")
    args = p.parse_args()

    src = args.input.expanduser().resolve()

    if not src.is_file():
        sys.exit(f"Missing file: {src}")

    if args.in_place:
        tmp = src.with_suffix(src.suffix + ".tmp_migrate")
        try:
            migrate(src, tmp, args.old_id, args.new_id)
            shutil.move(str(tmp), str(src))
            print(f"Replaced in place: {src}")
        finally:
            try:
                tmp.unlink()
            except OSError:
                pass
    else:
        if args.output is None:
            sys.exit("output path is required (or use --in-place)")
        dst = args.output.expanduser().resolve()
        migrate(src, dst, args.old_id, args.new_id)


if __name__ == "__main__":
    main()
