"""Independent Windows watchdog for Tracer primary/backup servers."""
from __future__ import annotations

import json
import os
import socket
import subprocess
import sys
import time
import urllib.error
import urllib.request
from datetime import datetime
from pathlib import Path


BASE_DIR = Path(__file__).resolve().parent
HA_ENV = BASE_DIR / "ha.env"
HA_STATE = BASE_DIR / "ha_state.json"
WATCHDOG_LOCK_PORT = 51991


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


def enabled(value: object) -> bool:
    return str(value or "").strip().lower() in {"1", "true", "yes", "on"}


def port_open(port: int) -> bool:
    try:
        with socket.create_connection(("127.0.0.1", port), timeout=1.5):
            return True
    except OSError:
        return False


def local_ip() -> str:
    try:
        with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as probe:
            probe.connect(("1.1.1.1", 80))
            return probe.getsockname()[0]
    except OSError:
        return "127.0.0.1"


def app_version() -> str:
    build_file = BASE_DIR / "android_attendance" / "app" / "build.gradle"
    try:
        for line in build_file.read_text(encoding="utf-8").splitlines():
            if "versionName" in line:
                return line.split("versionName", 1)[1].strip().strip("\"'")
    except OSError:
        pass
    return "server"


def post_json(url: str, secret: str, payload: dict) -> dict:
    body = json.dumps(payload, separators=(",", ":")).encode("utf-8")
    request = urllib.request.Request(
        url,
        data=body,
        method="POST",
        headers={
            "Content-Type": "application/json",
            "Accept": "application/json",
            "X-Tracer-Server-Key": secret,
            "User-Agent": "Tracer-Server-Watchdog/1.0"
        }
    )
    with urllib.request.urlopen(request, timeout=25) as response:
        return json.loads(response.read().decode("utf-8"))


def write_state(result: dict) -> None:
    state = {
        "mode": result.get("mode", "read_only"),
        "active_node_id": result.get("active_node_id", ""),
        "active_node_name": result.get("active_node_name", ""),
        "lease_until": result.get("lease_until", ""),
        "generation": result.get("generation", 0),
        "updated_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    }
    temporary = HA_STATE.with_name(
        f"{HA_STATE.name}.{os.getpid()}.new"
    )
    temporary.write_text(json.dumps(state, indent=2), encoding="utf-8")
    for attempt in range(3):
        try:
            os.replace(temporary, HA_STATE)
            return
        except PermissionError:
            if attempt == 2:
                raise
            time.sleep(0.08 * (attempt + 1))


class CloudflaredProcess:
    def __init__(self, config: dict[str, str]):
        self.config = config
        self.process: subprocess.Popen | None = None

    def running(self) -> bool:
        return bool(self.process and self.process.poll() is None)

    def start(self) -> None:
        if self.running() or not enabled(self.config.get("HA_MANAGE_CLOUDFLARED", "1")):
            return
        executable = self.config.get(
            "HA_CLOUDFLARED_EXE",
            str(BASE_DIR / "cloudflared.exe")
        )
        config_file = self.config.get(
            "HA_CLOUDFLARED_CONFIG",
            str(Path.home() / ".cloudflared" / "config-tracer-ha.yml")
        )
        tunnel = self.config.get("HA_CLOUDFLARED_TUNNEL", "tracer-server-ha")
        command = [executable, "tunnel", "--config", config_file, "run", tunnel]
        print("[HA] Menyalakan konektor Cloudflare aktif...")
        self.process = subprocess.Popen(command, cwd=str(BASE_DIR))

    def stop(self) -> None:
        if not self.running():
            self.process = None
            return
        print("[HA] Menghentikan konektor Cloudflare karena server standby...")
        self.process.terminate()
        try:
            self.process.wait(timeout=8)
        except subprocess.TimeoutExpired:
            self.process.kill()
            self.process.wait(timeout=5)
        self.process = None


def main() -> int:
    instance_lock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    try:
        instance_lock.bind(("127.0.0.1", WATCHDOG_LOCK_PORT))
        instance_lock.listen(1)
    except OSError:
        instance_lock.close()
        print("Tracer Server Failover sudah berjalan pada PC ini.")
        return 3

    config = read_env(HA_ENV)
    required = ["HA_NODE_ID", "HA_NODE_NAME", "HA_NODE_KIND", "HA_SHARED_SECRET"]
    missing = [key for key in required if not config.get(key)]
    if missing:
        print("Konfigurasi HA belum lengkap: " + ", ".join(missing))
        return 2

    control_url = config.get(
        "HA_CONTROL_URL", "https://attendance-api.asyscntr.com"
    ).rstrip("/") + "/api/ha/heartbeat"
    started_at = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    cloudflare = CloudflaredProcess(config)
    consecutive_control_errors = 0
    print("=" * 58)
    print(" TRACER SERVER FAILOVER")
    print(" Node :", config["HA_NODE_NAME"], "(" + config["HA_NODE_KIND"] + ")")
    print(" ID   :", config["HA_NODE_ID"])
    print("=" * 58)

    try:
        while True:
            flask_ok = port_open(int(config.get("HA_FLASK_PORT", "5010")))
            db_ok = port_open(int(config.get("HA_DB_TUNNEL_PORT", "13306")))
            payload = {
                "node_id": config["HA_NODE_ID"],
                "node_name": config["HA_NODE_NAME"],
                "node_kind": config["HA_NODE_KIND"],
                "hostname": socket.gethostname(),
                "local_ip": local_ip(),
                "app_url": config.get("HA_APP_URL", "https://apk.asyscntr.com"),
                "app_version": app_version(),
                "flask_ok": flask_ok,
                "db_tunnel_ok": db_ok,
                "cloudflared_ok": cloudflare.running(),
                "started_at": started_at
            }
            try:
                result = post_json(control_url, config["HA_SHARED_SECRET"], payload)
                if not result.get("success"):
                    raise RuntimeError(result.get("error", "Heartbeat ditolak"))
                write_state(result)
                consecutive_control_errors = 0
                should_publish = bool(
                    result.get("mode") == "write" and flask_ok and db_ok
                )
                if should_publish:
                    cloudflare.start()
                else:
                    cloudflare.stop()
                mode_label = "WRITE / AKTIF" if should_publish else "READ-ONLY / STANDBY"
                print(
                    datetime.now().strftime("[%H:%M:%S]"), mode_label,
                    "| DB", "OK" if db_ok else "PUTUS",
                    "| FLASK", "OK" if flask_ok else "MATI",
                    "| ACTIVE", result.get("active_node_name", "-")
                )
                interval = max(2, min(8, int(result.get("heartbeat_interval", 3))))
            except (OSError, ValueError, RuntimeError, urllib.error.URLError) as error:
                consecutive_control_errors += 1
                # Satu timeout singkat tidak boleh membuat tunnel hidup-mati.
                # Setelah tiga kegagalan beruntun, node dibuat fail-closed agar
                # server lain dapat mengambil lease tanpa dua penulis aktif.
                if consecutive_control_errors >= 3:
                    cloudflare.stop()
                    write_state({"mode": "read_only", "active_node_id": ""})
                print(
                    datetime.now().strftime("[%H:%M:%S]"),
                    f"CONTROL ERROR {consecutive_control_errors}/3:", error
                )
                interval = 4
            time.sleep(interval)
    except KeyboardInterrupt:
        print("\nWatchdog dihentikan.")
    finally:
        cloudflare.stop()
        instance_lock.close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
