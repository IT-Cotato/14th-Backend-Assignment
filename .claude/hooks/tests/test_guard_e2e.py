import json
from pathlib import Path
import subprocess
import sys
import unittest


SCRIPT = Path(__file__).resolve().parents[1] / "guard-e2e.py"


class GuardE2eTest(unittest.TestCase):
    def invoke(self, tool, path, **fields):
        payload = {"tool_name": tool, "tool_input": {"file_path": path, **fields}}
        return subprocess.run(
            [sys.executable, "-X", "utf8", str(SCRIPT)], input=json.dumps(payload),
            text=True, capture_output=True, encoding="utf-8",
        )

    def test_fixed_wait_in_e2e_is_blocked(self):
        for path in ("frontend/e2e/auth.spec.ts", "C:\\repo\\frontend\\e2e\\studies.spec.ts"):
            with self.subTest(path=path):
                result = self.invoke("Write", path, content="await page.waitForTimeout(1000)")
                self.assertEqual(result.returncode, 2)
                self.assertIn("waitForTimeout", result.stderr)

    def test_hardcoded_password_in_e2e_is_blocked(self):
        result = self.invoke("Edit", "frontend/e2e/helpers.ts", new_string="const user = { password: 'hunter2024' }")
        self.assertEqual(result.returncode, 2)

    def test_generated_password_and_normal_code_pass(self):
        for text in ("password: testPassword()", "password: `pw-${randomUUID()}`", "await expect(page).toHaveURL(/login/)"):
            with self.subTest(text=text):
                result = self.invoke("Write", "frontend/e2e/helpers.ts", content=text)
                self.assertEqual(result.returncode, 0)

    def test_files_outside_e2e_are_ignored(self):
        result = self.invoke("Write", "frontend/src/App.tsx", content="page.waitForTimeout(1)")
        self.assertEqual(result.returncode, 0)


if __name__ == "__main__":
    unittest.main()
