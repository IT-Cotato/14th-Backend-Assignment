"""E2E 테스트 파일에 고정 대기나 하드코딩한 비밀번호가 들어가는 것을 막는다.

Write/Edit 도구로 frontend/e2e/ 아래 파일을 쓸 때만 검사한다.
"""

import json
import re
import sys

RULES = [
    (re.compile(r"\bwaitForTimeout\s*\("), "waitForTimeout 같은 고정 대기 대신 expect(...)의 자동 대기를 쓴다."),
    (
        re.compile(r"""password\s*[:=]\s*['"`][^'"`$]{4,}['"`]""", re.IGNORECASE),
        "비밀번호를 코드에 적지 않는다. helpers.ts 의 testPassword()를 쓴다.",
    ),
]


def written_text(tool_name, tool_input):
    if tool_name == "Write":
        return tool_input.get("content", "")
    if tool_name == "Edit":
        return tool_input.get("new_string", "")
    return ""


def main():
    try:
        payload = json.load(sys.stdin)
        tool_name = payload["tool_name"]
        tool_input = payload["tool_input"]
        path = tool_input.get("file_path", "").replace("\\", "/")
    except (ValueError, KeyError, TypeError, AttributeError):
        return 0  # 입력을 해석할 수 없으면 이 훅은 관여하지 않는다 (env 보호 훅과 달리 차단 목적이 아님)

    if "/frontend/e2e/" not in f"/{path}" or tool_name not in {"Write", "Edit"}:
        return 0

    text = written_text(tool_name, tool_input)
    problems = [message for pattern, message in RULES if pattern.search(text)]
    if problems:
        print("E2E 규칙 훅: " + " ".join(problems) + " (.claude/rules/e2e.md)", file=sys.stderr)
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())
