from __future__ import annotations

import functools
import os
import queue
import re
import socket
import sqlite3 as native_sqlite3
import subprocess
import sys
import threading
import time
from pathlib import Path
from typing import Any


BASE_DIR = Path(__file__).resolve().parent
VENDOR_DIR = BASE_DIR / "vendor"
if str(VENDOR_DIR) not in sys.path:
    sys.path.insert(0, str(VENDOR_DIR))

import pymysql
import sqlglot


CONFIG_LOCK = threading.Lock()
START_LOCK = threading.Lock()
MYSQL_POOL = queue.LifoQueue(maxsize=max(2, int(os.environ.get("MYSQL_POOL_SIZE", "12"))))


def _discard_mysql_connection(raw) -> None:
    try:
        raw.close()
    except Exception:
        pass


def _acquire_mysql_connection(connect_kwargs):
    while True:
        try:
            raw, released_at = MYSQL_POOL.get_nowait()
        except queue.Empty:
            break
        try:
            # Koneksi yang baru saja dikembalikan sudah diuji oleh rollback.
            # Ping hanya diperlukan setelah lama menganggur agar request normal
            # tidak membayar round-trip tunnel tambahan.
            if time.monotonic() - released_at > 30.0:
                raw.ping(reconnect=False)
            return raw
        except Exception:
            _discard_mysql_connection(raw)
    return pymysql.connect(**connect_kwargs)


def _release_mysql_connection(raw) -> None:
    if raw is None:
        return
    try:
        # Sama seperti menutup koneksi biasa: transaksi tanpa commit tidak boleh bocor
        # ke request berikutnya.
        raw.rollback()
    except Exception:
        _discard_mysql_connection(raw)
        return
    try:
        MYSQL_POOL.put_nowait((raw, time.monotonic()))
    except queue.Full:
        _discard_mysql_connection(raw)


def _read_env_file(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    if not path.is_file():
        return values
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        key = key.strip()
        value = value.strip()
        if len(value) >= 2 and value[0] == value[-1] and value[0] in "\"'":
            value = value[1:-1]
        if key:
            values[key] = value
    return values


@functools.lru_cache(maxsize=1)
def database_config() -> dict[str, str]:
    requested = os.environ.get("APP_DB_CONFIG", "mysql.env").strip() or "mysql.env"
    config_path = Path(requested)
    if not config_path.is_absolute():
        config_path = BASE_DIR / config_path
    values = _read_env_file(config_path)
    for key in (
        "APP_DB_BACKEND",
        "MYSQL_HOST",
        "MYSQL_PORT",
        "MYSQL_DATABASE",
        "MYSQL_USER",
        "MYSQL_PASSWORD",
        "MYSQL_AUTOSTART",
    ):
        if key in os.environ:
            values[key] = os.environ[key]
    values.setdefault("APP_DB_BACKEND", "sqlite")
    values.setdefault("MYSQL_HOST", "127.0.0.1")
    values.setdefault("MYSQL_PORT", "3306")
    values.setdefault("MYSQL_DATABASE", "tracer_app")
    values.setdefault("MYSQL_USER", "tracer_app")
    values.setdefault("MYSQL_PASSWORD", "")
    values.setdefault("MYSQL_AUTOSTART", "1")
    return values


def using_mysql() -> bool:
    return database_config().get("APP_DB_BACKEND", "sqlite").lower() == "mysql"


def _port_is_open(host: str, port: int) -> bool:
    try:
        with socket.create_connection((host, port), timeout=1):
            return True
    except OSError:
        return False


def _prepare_mysql_option_file(runtime: Path, option_file: Path) -> None:
    """Keep the portable MySQL paths valid after the application folder is moved."""
    paths = {
        "basedir": runtime / "server",
        "datadir": runtime / "data",
        "log-error": runtime / "logs" / "mysql-error.log",
        "pid-file": runtime / "mysql.pid",
    }
    original = option_file.read_text(encoding="utf-8")
    updated = original
    for key, path in paths.items():
        value = path.as_posix()
        updated = re.sub(rf"(?mi)^{re.escape(key)}\s*=.*$", f"{key}={value}", updated)
    if updated != original:
        option_file.write_text(updated, encoding="utf-8")


def ensure_mysql_server() -> None:
    config = database_config()
    host = config["MYSQL_HOST"].strip().lower()
    port = int(config["MYSQL_PORT"])
    if _port_is_open(host, port):
        return
    if config.get("MYSQL_AUTOSTART", "1").lower() not in {"1", "true", "yes", "on"}:
        return
    if host not in {"127.0.0.1", "localhost", "::1"}:
        return

    with START_LOCK:
        if _port_is_open(host, port):
            return
        runtime = BASE_DIR / "mysql_runtime"
        executable = runtime / "server" / "bin" / "mysqld.exe"
        option_file = runtime / "my.ini"
        if not executable.is_file() or not option_file.is_file():
            raise RuntimeError(
                "MySQL lokal belum disiapkan. Jalankan setup_mysql_runtime.py terlebih dahulu."
            )
        _prepare_mysql_option_file(runtime, option_file)
        creation_flags = getattr(subprocess, "CREATE_NO_WINDOW", 0)
        subprocess.Popen(
            [str(executable), f"--defaults-file={option_file}"],
            cwd=str(executable.parent.parent),
            stdin=subprocess.DEVNULL,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
            creationflags=creation_flags,
        )
        deadline = time.time() + 45
        while time.time() < deadline:
            if _port_is_open(host, port):
                return
            time.sleep(0.5)
        raise RuntimeError(f"MySQL tidak siap di {host}:{port}")


def _replace_placeholders(sql: str) -> str:
    result: list[str] = []
    quote = ""
    index = 0
    while index < len(sql):
        char = sql[index]
        if quote:
            result.append(char)
            if char == quote:
                if index + 1 < len(sql) and sql[index + 1] == quote:
                    result.append(sql[index + 1])
                    index += 1
                else:
                    quote = ""
            elif char == "\\" and index + 1 < len(sql):
                result.append(sql[index + 1])
                index += 1
        elif char in {"'", '"', "`"}:
            quote = char
            result.append(char)
        elif char == "?":
            result.append("%s")
        else:
            result.append(char)
        index += 1
    return "".join(result)


def _escape_mysql_percent(sql: str) -> str:
    result: list[str] = []
    index = 0
    while index < len(sql):
        char = sql[index]
        if char != "%":
            result.append(char)
            index += 1
            continue
        next_char = sql[index + 1] if index + 1 < len(sql) else ""
        if next_char in {"s", "%"}:
            result.extend(["%", next_char])
            index += 2
            continue
        result.append("%%")
        index += 1
    return "".join(result)


@functools.lru_cache(maxsize=2048)
def translate_mysql_sql(sql: str) -> str:
    clean = str(sql or "").strip()
    upper = clean.upper()
    if upper == "BEGIN IMMEDIATE":
        return "START TRANSACTION"
    try:
        translated = sqlglot.transpile(clean, read="sqlite", write="mysql")[0]
    except Exception:
        translated = clean

    translated = re.sub(
        r"\bINSERT\s+OR\s+IGNORE\s+INTO\b",
        "INSERT IGNORE INTO",
        translated,
        flags=re.IGNORECASE,
    )
    translated = re.sub(
        r"\bINSERT\s+OR\s+REPLACE\s+INTO\b",
        "REPLACE INTO",
        translated,
        flags=re.IGNORECASE,
    )
    translated = re.sub(
        r"\s+ON\s+CONFLICT\s*\([^)]*\)\s+DO\s+UPDATE\s+(?:SET\s+)?",
        " ON DUPLICATE KEY UPDATE ",
        translated,
        flags=re.IGNORECASE | re.DOTALL,
    )
    translated = re.sub(
        r"\bexcluded\.([A-Za-z_][A-Za-z0-9_]*)\b",
        r"VALUES(\1)",
        translated,
        flags=re.IGNORECASE,
    )
    translated = re.sub(
        r"\bCOLLATE\s+NOCASE\b",
        "COLLATE utf8mb4_unicode_ci",
        translated,
        flags=re.IGNORECASE,
    )
    if re.match(r"^CREATE\s+TABLE\b", translated, flags=re.IGNORECASE):
        translated = re.sub(r"\bTEXT\b", "VARCHAR(255)", translated, flags=re.IGNORECASE)
    translated = re.sub(
        r"^(CREATE\s+(?:UNIQUE\s+)?INDEX)\s+IF\s+NOT\s+EXISTS\s+",
        r"\1 ",
        translated,
        flags=re.IGNORECASE,
    )
    return _escape_mysql_percent(_replace_placeholders(translated))


class SyntheticCursor:
    def __init__(self, rows: list[tuple[Any, ...]] | None = None):
        self._rows = list(rows or [])
        self._index = 0
        self.rowcount = len(self._rows)
        self.lastrowid = None
        self.description = None

    def fetchone(self):
        if self._index >= len(self._rows):
            return None
        row = self._rows[self._index]
        self._index += 1
        return row

    def fetchall(self):
        rows = self._rows[self._index :]
        self._index = len(self._rows)
        return rows

    def fetchmany(self, size=1):
        rows = self._rows[self._index : self._index + size]
        self._index += len(rows)
        return rows

    def close(self):
        return None

    def __iter__(self):
        return iter(self.fetchall())


class MySQLCursorAdapter:
    def __init__(self, connection: "MySQLConnectionAdapter"):
        self._connection = connection
        self._cursor = None
        self._synthetic: SyntheticCursor | None = None

    def _raw_cursor(self):
        if self._cursor is None:
            self._cursor = self._connection._raw.cursor()
        return self._cursor

    def _set_synthetic(self, rows=None):
        self._synthetic = SyntheticCursor(rows)
        return self

    def execute(self, sql, params=None):
        clean = str(sql or "").strip()
        pragma_table = re.match(
            r"^PRAGMA\s+table_info\s*\(\s*([A-Za-z_][A-Za-z0-9_]*)\s*\)\s*$",
            clean,
            flags=re.IGNORECASE,
        )
        if pragma_table:
            table = pragma_table.group(1)
            raw = self._raw_cursor()
            raw.execute(f"SHOW COLUMNS FROM `{table}`")
            rows = []
            for index, row in enumerate(raw.fetchall()):
                rows.append(
                    (
                        index,
                        row[0],
                        row[1],
                        0 if str(row[2]).upper() == "YES" else 1,
                        row[4],
                        1 if str(row[3]).upper() == "PRI" else 0,
                    )
                )
            return self._set_synthetic(rows)
        if clean.upper().startswith("PRAGMA"):
            return self._set_synthetic([])

        translated = translate_mysql_sql(clean)
        raw = self._raw_cursor()
        self._synthetic = None
        try:
            raw.execute(translated, params or ())
        except pymysql.err.OperationalError as error:
            if error.args and error.args[0] == 1061 and re.match(
                r"^CREATE\s+(?:UNIQUE\s+)?INDEX\b", translated, re.IGNORECASE
            ):
                return self._set_synthetic([])
            raise
        return self

    def executemany(self, sql, params):
        clean = str(sql or "").strip()
        translated = translate_mysql_sql(clean)
        raw = self._raw_cursor()
        self._synthetic = None
        raw.executemany(translated, params)
        return self

    def fetchone(self):
        if self._synthetic is not None:
            return self._synthetic.fetchone()
        return self._raw_cursor().fetchone()

    def fetchall(self):
        if self._synthetic is not None:
            return self._synthetic.fetchall()
        return self._raw_cursor().fetchall()

    def fetchmany(self, size=1):
        if self._synthetic is not None:
            return self._synthetic.fetchmany(size)
        return self._raw_cursor().fetchmany(size)

    def close(self):
        if self._cursor is not None:
            self._cursor.close()
            self._cursor = None
        self._synthetic = None

    def __iter__(self):
        if self._synthetic is not None:
            return iter(self._synthetic)
        return iter(self._raw_cursor())

    def __next__(self):
        return next(iter(self))

    def __getattr__(self, name):
        if self._synthetic is not None and hasattr(self._synthetic, name):
            return getattr(self._synthetic, name)
        return getattr(self._raw_cursor(), name)


class MySQLConnectionAdapter:
    def __init__(self, **kwargs):
        self._raw = _acquire_mysql_connection(kwargs)
        self._closed = False

    def _ping(self):
        try:
            self._raw.ping(reconnect=True)
        except pymysql.MySQLError:
            ensure_mysql_server()
            self._raw.ping(reconnect=True)

    def cursor(self, *args, **kwargs):
        return MySQLCursorAdapter(self)

    def execute(self, sql, params=None):
        cursor = self.cursor()
        return cursor.execute(sql, params)

    def executemany(self, sql, params):
        cursor = self.cursor()
        return cursor.executemany(sql, params)

    def commit(self):
        return self._raw.commit()

    def rollback(self):
        return self._raw.rollback()

    def close(self):
        if self._closed:
            return None
        raw = self._raw
        self._raw = None
        self._closed = True
        _release_mysql_connection(raw)
        return None

    def __getattr__(self, name):
        return getattr(self._raw, name)


def connect(database_file=None, timeout=15, check_same_thread=True, **kwargs):
    if not using_mysql():
        return native_sqlite3.connect(
            database_file,
            timeout=timeout,
            check_same_thread=check_same_thread,
            **kwargs,
        )
    ensure_mysql_server()
    config = database_config()
    return MySQLConnectionAdapter(
        host=config["MYSQL_HOST"],
        port=int(config["MYSQL_PORT"]),
        user=config["MYSQL_USER"],
        password=config["MYSQL_PASSWORD"],
        database=config["MYSQL_DATABASE"],
        charset="utf8mb4",
        autocommit=False,
        connect_timeout=max(5, int(timeout)),
        read_timeout=120,
        write_timeout=120,
        init_command="SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED",
    )


class DatabaseAPI:
    OperationalError = (
        native_sqlite3.OperationalError,
        pymysql.err.OperationalError,
        pymysql.err.InternalError,
    )
    IntegrityError = (native_sqlite3.IntegrityError, pymysql.err.IntegrityError)
    Error = (native_sqlite3.Error, pymysql.MySQLError)
    Row = native_sqlite3.Row

    @staticmethod
    def connect(*args, **kwargs):
        return connect(*args, **kwargs)


database_api = DatabaseAPI()
