"""Run browser tests against disposable, loopback-only MySQL and Spring Boot."""
import base64
import os
from pathlib import Path
import secrets
import shutil
import socket
import subprocess
import sys
import tempfile
import time
import urllib.request

FRONTEND = Path(__file__).resolve().parents[1]
BACKEND = FRONTEND.parent / "backend"


def reserve_port(port=0):
    with socket.socket() as listener:
        listener.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        listener.bind(("127.0.0.1", port))
        return listener.getsockname()[1]


def wait_until(process, ready, label, timeout=60):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        if process.poll() is not None:
            raise RuntimeError(f"{label} exited with status {process.returncode}.")
        if ready():
            return
        time.sleep(0.2)
    raise RuntimeError(f"{label} did not become ready within {timeout}s.")


def backend_ready():
    try:
        with urllib.request.urlopen("http://127.0.0.1:8080/api/study-posts", timeout=1) as response:
            return response.status == 200
    except OSError:
        return False


def stop(process):
    if process and process.poll() is None:
        process.terminate()
        try:
            process.wait(timeout=10)
        except subprocess.TimeoutExpired:
            process.kill()
            process.wait()


def run():
    for executable in ("mysqld", "mysql", "node"):
        if not shutil.which(executable):
            raise RuntimeError(f"{executable} is required on PATH.")
    java_home = os.environ.get("JAVA_HOME")
    java = str(Path(java_home) / "bin/java") if java_home else "java"
    if not shutil.which(java):
        raise RuntimeError("Set JAVA_HOME to a Java 17 JDK or put Java 17 on PATH.")
    playwright = FRONTEND / "node_modules/@playwright/test/cli.js"
    if not playwright.is_file():
        raise RuntimeError("Run npm ci in frontend before running E2E tests.")
    reserve_port(8080)
    reserve_port(3000)
    subprocess.run(["./gradlew", "bootJar"], cwd=BACKEND, check=True)
    jars = list((BACKEND / "build/libs").glob("*.jar"))
    executable_jars = [jar for jar in jars if not jar.name.endswith("-plain.jar")]
    if len(executable_jars) != 1:
        raise RuntimeError("Expected exactly one executable Spring Boot JAR in backend/build/libs.")
    jar = executable_jars[0]

    with tempfile.TemporaryDirectory(prefix="cotato-e2e-") as directory:
        root = Path(directory)
        data = root / "data"
        db_socket = root / "mysql.sock"
        db_port = reserve_port()
        base = ["mysqld", "--no-defaults", f"--datadir={data}"]
        mysql = ["mysql", "--no-defaults", "--protocol=SOCKET", f"--socket={db_socket}", "-uroot"]
        db = app = None
        with (root / "mysql.log").open("w") as db_log, (root / "spring.log").open("w") as app_log:
            try:
                subprocess.run(base + ["--initialize-insecure"], stdout=db_log, stderr=subprocess.STDOUT,
                               check=True, timeout=60)
                db = subprocess.Popen(base + [f"--socket={db_socket}", f"--port={db_port}",
                                      "--bind-address=127.0.0.1", f"--pid-file={root / 'mysql.pid'}", "--mysqlx=OFF"],
                                      stdout=db_log, stderr=subprocess.STDOUT)
                wait_until(db, lambda: subprocess.run(mysql + ["-e", "SELECT 1"], capture_output=True,
                           timeout=3).returncode == 0, "MySQL")
                subprocess.run(mysql + ["-e", "CREATE DATABASE cotato_e2e CHARACTER SET utf8mb4"],
                               check=True, timeout=10)
                app_env = {key: value for key, value in os.environ.items()
                           if not key.startswith(("SPRING_", "SERVER_", "DB_", "JDK_JAVA_OPTIONS",
                                                  "JAVA_TOOL_OPTIONS", "_JAVA_OPTIONS"))}
                app_env.update(
                    DB_URL=f"jdbc:mysql://127.0.0.1:{db_port}/cotato_e2e",
                    DB_USERNAME="root", DB_PASSWORD="", SPRING_PROFILES_ACTIVE="local",
                    JWT_SIGNING_KEY=base64.b64encode(secrets.token_bytes(32)).decode(),
                    SERVER_ADDRESS="127.0.0.1", SERVER_PORT="8080",
                )
                app = subprocess.Popen([java, "-jar", str(jar)], cwd=root, env=app_env,
                                       stdout=app_log, stderr=subprocess.STDOUT)
                wait_until(app, backend_ready, "Spring Boot")
                print("Isolated MySQL and Spring Boot ready. Starting Playwright on port 3000.", flush=True)
                return subprocess.run(
                    ["node", str(playwright), "test", *sys.argv[1:]], cwd=FRONTEND,
                    env={**os.environ, "E2E_ISOLATED_MYSQL": "1", "VITE_API_BASE_URL": ""},
                    check=False,
                ).returncode
            except (RuntimeError, subprocess.SubprocessError):
                for filename in ("mysql.log", "spring.log"):
                    print((root / filename).read_text(errors="replace")[-2600:], file=sys.stderr)
                raise
            finally:
                stop(app)
                stop(db)


if __name__ == "__main__":
    try:
        sys.exit(run())
    except (OSError, RuntimeError, subprocess.SubprocessError) as error:
        sys.exit(f"E2E environment failed: {error}")
