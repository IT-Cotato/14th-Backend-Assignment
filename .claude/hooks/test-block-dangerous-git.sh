#!/bin/bash
# block-dangerous-git.sh 테스트
# - 차단해야 할 명령(exit 2)과 통과시켜야 할 명령(exit 0)을 훅에 JSON으로 넣고 기대값과 비교한다.
# - 실행: bash .claude/hooks/test-block-dangerous-git.sh
# - 훅을 고치면 이 스크립트를 돌려서 FAIL=0 인지 확인하고, 새 케이스가 생기면 여기에 추가한다.

cd "$(dirname "$0")/../.." || exit 1
HOOK=.claude/hooks/block-dangerous-git.sh
PASS=0
FAIL=0

check() {
  local expected=$1 desc=$2 actual=$3
  if [ "$actual" = "$expected" ]; then
    PASS=$((PASS + 1)); echo "OK   exp=$expected got=$actual :: $desc"
  else
    FAIL=$((FAIL + 1)); echo "FAIL exp=$expected got=$actual :: $desc"
  fi
}

# 명령 문자열을 PreToolUse JSON으로 감싸 훅에 넣는다
t() {
  local expected=$1 cmd=$2
  jq -n --arg c "$cmd" '{tool_name:"Bash",tool_input:{command:$c}}' | bash "$HOOK" >/dev/null 2>&1
  check "$expected" "$cmd" "$?"
}

echo "## 차단 (exit 2)"
t 2 'git push --force origin main'
t 2 'git push -f'
t 2 'git push --force-with-lease'
t 2 'git push origin main --force'
t 2 'cd src && git push --force'
t 2 'git reset --hard HEAD~1'
t 2 'git -C . reset --hard'
t 2 "sh -c 'git reset --hard'"
t 2 'eval "git reset --hard"'
t 2 '(cd src; git reset --hard)'
t 2 'git clean -fd'
t 2 'git clean -n -f'
t 2 'git branch -D feature/x'
t 2 'git branch --delete --force feature/x'
t 2 'git checkout -- .'
t 2 'git restore .'
t 2 'git restore --worktree .'
t 2 'git restore --staged --worktree .'
t 2 'git restore -SW .'

echo "## 통과 (exit 0)"
t 0 'git status'
t 0 'git push origin seungmin/harness'
t 0 'git push --follow-tags'
t 0 'gh pr create --base seungmin/basic'
t 0 'git commit -m "docs: git push --force 금지 안내"'
t 0 "grep -n 'reset --hard' docs/conventions.md"
t 0 'git branch -d merged-branch'
t 0 'git restore src/main/java/A.java'
t 0 'git restore --staged .'
t 0 'git restore -S .'
t 0 './gradlew build'

echo "## fail-closed (검사할 수 없으면 exit 2)"
printf 'not json' | bash "$HOOK" >/dev/null 2>&1
check 2 "잘못된 JSON 입력" "$?"

printf '{"tool_name":"Bash","tool_input":{}}' | bash "$HOOK" >/dev/null 2>&1
check 0 "command 필드 없음 (검사할 명령 없음)" "$?"

# jq만 빠진 PATH를 만들어서 jq가 없는 PC를 흉내낸다
NOJQ=$(mktemp -d)
for bin in cat grep sed perl printf; do
  p=$(command -v "$bin") && [ -x "$p" ] && ln -s "$p" "$NOJQ/$bin"
done
printf '{"tool_input":{"command":"git status"}}' | PATH="$NOJQ" "$BASH" "$HOOK" >/dev/null 2>&1
check 2 "jq 없음" "$?"
rm -rf "$NOJQ"

echo
echo "PASS=$PASS FAIL=$FAIL"
[ "$FAIL" -eq 0 ]
