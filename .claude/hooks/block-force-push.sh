#!/usr/bin/env bash
# PreToolUse(Bash): 명령 문자열에 나타나는 강제 push를 차단한다.
# 참고: disler/claude-code-damage-control의 패턴 검사 방식.
# 완전한 셸 해석기가 아니므로 복잡한 인용문은 오탐할 수 있다.
# 별칭, 변수로 조립한 명령, 외부 스크립트 내부는 검사하지 않는다.

set -uo pipefail

block() {
  printf '%s\n' "$1" >&2
  exit 2
}

command -v jq >/dev/null 2>&1 ||
  block "Blocked: jq is unavailable; hook check could not run."

command_text=$(jq -ers '
  if length != 1 then
    error("expected one event")
  else
    .[0].tool_input.command |
    if type == "string" then .
    else error("expected command string")
    end
  end
' 2>/dev/null) || block "Blocked: invalid hook input."

# 줄 연결과 따옴표로 감싼 옵션을 보수적으로 처리한다.
command_text=${command_text//$'\\\r\n'/}
command_text=${command_text//$'\\\n'/}
command_text=${command_text//\"/}
command_text=${command_text//\'/}

# git push 및 git -C 경로 push 등 명령 문자열을 찾는다.
push_pattern='^[[:space:]]*(command[[:space:]]+)?git(\.exe)?[[:space:]]+([^;&|]*[[:space:]]+)?push([[:space:]]|$)'

long_force='(^|[[:space:]])--(force|force-with-lease|force-if-includes|mirror)([=[:space:]]|$)'
short_force='(^|[[:space:]])-[^-o[:space:]]*f[^[:space:]]*([[:space:]]|$)'
force_ref='(^|[[:space:]])\+[^[:space:]]+'

# 명령을 실행하지 않고 문자열만 검사한다.
while IFS= read -r part || [[ -n "$part" ]]; do
  if [[ "$part" =~ $push_pattern ]]; then
    if [[ "$part" =~ $long_force ||
          "$part" =~ $short_force ||
          "$part" =~ $force_ref ]]; then
      block "Blocked: AI force-push is prohibited. Use a normal push."
    fi
  fi
done < <(printf '%s\n' "$command_text" | tr ';&|' '\n')

exit 0