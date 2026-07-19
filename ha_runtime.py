"""Small role guard for the two application servers sharing the VPS database."""
from __future__ import annotations

import json
from datetime import datetime
from pathlib import Path


BASE_DIR = Path(__file__).resolve().parent
HA_ENV = BASE_DIR / "ha.env"
HA_STATE = BASE_DIR / "ha_state.json"


def read_env(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    if not path.is_file():
        return values
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        values[key.strip()] = value.strip().strip("\"'")
    return values


def ha_config() -> dict[str, str]:
    values = read_env(HA_ENV)
    values.setdefault("HA_NODE_ROLE", "primary")
    values.setdefault("HA_NODE_NAME", "primary")
    values.setdefault("HA_SHARED_SECRET", "")
    values.setdefault("HA_PRIMARY_URL", "https://apk.asyscntr.com")
    values.setdefault("HA_NODE_ID", "")
    values.setdefault("HA_NODE_KIND", "primary")
    values.setdefault("HA_CLUSTER_GUARD", "0")
    values.setdefault("HA_CONTROL_URL", "https://attendance-api.asyscntr.com")
    return values


def node_role() -> str:
    return ha_config()["HA_NODE_ROLE"].strip().lower()


def is_standby() -> bool:
    config = ha_config()
    cluster_guard = config.get("HA_CLUSTER_GUARD", "0").strip().lower()
    if cluster_guard in {"1", "true", "yes", "on"}:
        try:
            state = json.loads(HA_STATE.read_text(encoding="utf-8"))
            updated_at = datetime.strptime(
                str(state.get("updated_at", "")),
                "%Y-%m-%d %H:%M:%S"
            )
            age = (datetime.now() - updated_at).total_seconds()
            return age > 30 or state.get("mode") != "write"
        except Exception:
            # Fail closed when the independent witness/watchdog is unavailable.
            return True
    return node_role() == "standby"
