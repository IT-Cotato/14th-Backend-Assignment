import json
from pathlib import Path
import subprocess
import sys
import unittest


SCRIPT = Path(__file__).resolve().parents[1] / "protect-env.py"


class ProtectEnvTest(unittest.TestCase):
    def invoke(self, payload):
        return subprocess.run(
            [sys.executable, "-X", "utf8", str(SCRIPT)], input=payload,
            text=True, capture_output=True, encoding="utf-8",
        )

    def test_env_access_is_blocked(self):
        for tool in ("Read", "Edit", "Write"):
            for path in (".env", "config/.env.local", "C:\\project\\.env", "/tmp/한글 폴더/.ENV"):
                with self.subTest(tool=tool, path=path):
                    result = self.invoke(json.dumps({"tool_name": tool, "tool_input": {"file_path": path}}))
                    self.assertEqual(result.returncode, 2)
                    self.assertTrue(result.stderr)

    def test_source_files_pass(self):
        for path in ("src/Main.java", "docs/environment.md", ".envrc"):
            with self.subTest(path=path):
                result = self.invoke(json.dumps({"tool_name": "Write", "tool_input": {"file_path": path}}))
                self.assertEqual(result.returncode, 0)
                self.assertEqual(result.stdout, "")

    def test_invalid_payload_is_blocked(self):
        for payload in ("not json", "{}", '{"tool_name":"Read","tool_input":{}}'):
            with self.subTest(payload=payload):
                result = self.invoke(payload)
                # 도구 종류가 없는 입력도 차단해야 한다.
                self.assertEqual(result.returncode, 2)


if __name__ == "__main__":
    unittest.main()
