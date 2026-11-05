"""Read/Edit/Write 도구에서 환경 변수 파일 접근을 막는다."""

import json
import sys


def main():
    try:
        payload = json.load(sys.stdin)
        if payload["tool_name"] not in {"Read", "Edit", "Write"}:
            return 0
        path = payload["tool_input"]["file_path"]
        filename = path.replace("\\", "/").rstrip("/").rsplit("/", 1)[-1].lower()
    except (ValueError, KeyError, TypeError, AttributeError):
        print("환경 파일 보호 훅: 입력을 확인할 수 없어 접근을 차단합니다.", file=sys.stderr)
        return 2

    if filename == ".env" or filename.startswith(".env."):
        print("환경 파일 보호 훅: .env 파일은 직접 관리하고 도구로 읽거나 수정하지 않습니다.", file=sys.stderr)
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())
