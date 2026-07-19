"""Explicit, auditable role changes for the two-node deployment.

Promotion is intentionally manual until an independent external witness is
configured; this prevents two isolated servers from both accepting writes.
"""
from __future__ import annotations

import argparse
import os
from pathlib import Path

from ha_runtime import HA_ENV, ha_config


def write_config(values: dict[str, str]) -> None:
    lines = [
        "# Managed by ha_role.py. Keep the shared secret private.",
        f"HA_NODE_NAME={values['HA_NODE_NAME']}",
        f"HA_NODE_ROLE={values['HA_NODE_ROLE']}",
        f"HA_SHARED_SECRET={values['HA_SHARED_SECRET']}",
        f"HA_PRIMARY_URL={values['HA_PRIMARY_URL']}",
        "",
    ]
    temporary = HA_ENV.with_suffix(".env.new")
    temporary.write_text("\n".join(lines), encoding="utf-8")
    os.replace(temporary, HA_ENV)


def main() -> int:
    parser = argparse.ArgumentParser()
    sub = parser.add_subparsers(dest="command", required=True)
    promote = sub.add_parser("promote", help="Izinkan node ini menerima penulisan data.")
    promote.add_argument("--name", default="backup")
    standby = sub.add_parser("standby", help="Jadikan node ini hanya-baca dan arahkan sync ke primary.")
    standby.add_argument("--primary-url", required=True)
    standby.add_argument("--name", default="primary")
    args = parser.parse_args()
    values = ha_config()
    if not values.get("HA_SHARED_SECRET"):
        raise SystemExit("HA_SHARED_SECRET belum diisi.")
    if args.command == "promote":
        values["HA_NODE_NAME"] = args.name
        values["HA_NODE_ROLE"] = "primary"
        values["HA_PRIMARY_URL"] = ""
        write_config(values)
        print("Node ini sekarang aktif. Pastikan server utama lama telah dimatikan/dijadikan standby.")
    else:
        values["HA_NODE_NAME"] = args.name
        values["HA_NODE_ROLE"] = "standby"
        values["HA_PRIMARY_URL"] = args.primary_url.rstrip("/")
        write_config(values)
        print("Node ini sekarang standby dan penulisan data diblokir.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
