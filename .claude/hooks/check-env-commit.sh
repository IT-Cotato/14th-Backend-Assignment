#!/usr/bin/env bash
# PreToolUse 훅: Bash 도구로 git commit을 실행하기 직전에,
# 스테이징된 파일 중 민감 파일(.env 등)이 있으면 커밋을 차단한다.

input="$(cat)"

# 이번 Bash 호출이 git commit이 아니면 그냥 통과시킨다.
if ! echo "$input" | grep -q '"git commit'; then
  exit 0
fi

sensitive=$(git diff --cached --name-only 2>/dev/null \
  | grep -E '(^|/)\.env($|\.[^/]*$)|application-local\.ya?ml$' \
  | grep -v '\.env\.example$')

if [ -n "$sensitive" ]; then
  # 내용 변경 없는 순수 rename(이미 git에 있던 안전한 파일을 옮긴 것)은 제외한다.
  pure_renames=$(git diff --cached -M100 --diff-filter=R --name-only 2>/dev/null)
  sensitive=$(comm -23 <(echo "$sensitive" | sort) <(echo "$pure_renames" | sort) | sed '/^$/d')
fi

if [ -n "$sensitive" ]; then
  echo "커밋이 차단되었습니다: 스테이징된 파일 중 민감 파일이 있습니다." >&2
  echo "$sensitive" >&2
  echo "git reset <파일>로 스테이징에서 제외한 뒤 다시 시도하세요." >&2
  exit 2
fi

exit 0
