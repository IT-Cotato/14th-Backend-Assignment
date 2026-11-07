#!/bin/bash
# block-env-access.sh 테스트
# - 차단해야 할 호출(exit 2)과 통과시켜야 할 호출(exit 0)을 훅에 JSON으로 넣고 기대값과 비교한다.
# - 실행: bash .claude/hooks/test-block-env-access.sh
# - 훅을 고치면 이 스크립트를 돌려서 FAIL=0 인지 확인하고, 새 케이스가 생기면 여기에 추가한다.
# - 실제 .env 파일은 열지 않는다. 훅에 JSON 문자열만 넣는다.

cd "$(dirname "$0")/../.." || exit 1
HOOK=.claude/hooks/block-env-access.sh
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

# Bash 명령 문자열을 PreToolUse JSON으로 감싸 훅에 넣는다
t() {
  local expected=$1 cmd=$2
  jq -n --arg c "$cmd" '{tool_name:"Bash",tool_input:{command:$c}}' | bash "$HOOK" >/dev/null 2>&1
  check "$expected" "Bash: $cmd" "$?"
}

# 파일 도구 호출: tf <기대값> <도구> <필드> <값>
tf() {
  local expected=$1 tool=$2 field=$3 value=$4
  jq -n --arg t "$tool" --arg f "$field" --arg v "$value" '{tool_name:$t,tool_input:{($f):$v}}' \
    | bash "$HOOK" >/dev/null 2>&1
  check "$expected" "$tool.$field=$value" "$?"
}

echo "## Bash 차단 (exit 2)"
t 2 'cat .env'
t 2 'cat ./.env'
t 2 'cat ".env"'
t 2 "cat '.env'"
t 2 'less src/main/resources/.env'
t 2 'source .env'
t 2 '. .env'
t 2 'grep PASS .env'
t 2 'cp .env /tmp/x'
t 2 'git add .env'
t 2 'cat .env.local'
t 2 'cat config/.env.production'
t 2 'cat .*'
t 2 'cat .e*'
t 2 'less .en?'
t 2 'grep -r password .'
t 2 'grep -rn x src'
t 2 'grep -Rin x .'
t 2 'grep --recursive x .'
t 2 'cd src && grep -r x .'
t 2 'rg --hidden pw'
t 2 'rg -uu pw'
t 2 'rg --no-ignore pw'
t 2 "sh -c 'cat .env'"
t 2 'eval "cat .env"'
t 2 'export $(cat .env | xargs)'
t 2 'git commit -m "x" && cat .env'
# 메시지/heredoc 안이라도 실제로 실행되는 부분은 검사한다
t 2 'git commit -m "$(cat .env)"'
t 2 'git commit -m "`cat .env`"'
t 2 'git commit -m ok -m "$(cat .env)"'
t 2 'gh pr create --title x --body "$(cat .env)"'
t 2 'gh pr create --body-file .env'
t 2 'git commit -F .env'
t 2 $'cat <<EOF > notes.md\n$(cat .env)\nEOF'
t 2 $'cat <<\'EOF\' | bash\ncat .env\nEOF'
t 2 $'bash <<\'EOF\'\ncat .env\nEOF'
t 2 $'python3 <<\'EOF\'\nprint(open(".env").read())\nEOF'
t 2 $'cat <<\'EOF\' > .env\nX=1\nEOF'
t 2 $'tee .env.local <<\'EOF\'\nX=1\nEOF'

echo "## Bash 통과 (exit 0)"
t 0 './gradlew build'
t 0 'git status'
t 0 'ls -la'
t 0 'echo $HOME'
t 0 'cat .env.example'
t 0 'cp .env.example .env.template'
t 0 'cat build.gradle'
t 0 'cat .envrc'
t 0 'grep -n dotenv build.gradle'
t 0 'cat *'
t 0 'ls *.java'
t 0 "grep -n '.*Controller' src/main/java/A.java"
t 0 "grep -r --exclude='.env*' pw ."
t 0 'grep -r --exclude=.env* pw .'
t 0 'grep -n pw src/main/resources/application.yml'
t 0 'rg pw'
t 0 "rg --hidden -g '!.env*' pw"
t 0 'git grep -n pw'
t 0 'git commit -m "docs: .env 안내 추가"'
t 0 "git commit -m 'chore: .gitignore에 .env.* 추가'"
t 0 'git add .gitignore && git commit -m "chore: .env.local 무시"'
# 커밋/PR 초안 작성 흐름 (오탐 회귀 방지)
t 0 $'git commit -m "$(cat <<\'EOF\'\nchore: 하네스 구축\n\n.env, .env.local ignore 보강\nEOF\n)"'
t 0 $'git commit -m "chore: x" -m ".env.* 추가"'
t 0 'gh pr create --title "chore: .env 보강" --body ".env.local 무시"'
t 0 $'gh pr create --title x --body "$(cat <<\'EOF\'\n## 변경\n- .env 보호 훅\nEOF\n)"'
t 0 'gh issue create --title "x" --body "cat .env 오탐 수정"'
t 0 $'cat > .local/claude/commits/draft.md <<\'EOF\'\n# 초안\n.env 문자열 그대로\nEOF'
t 0 $'cat <<EOF > notes.md\n일반 텍스트 .env\nEOF'
t 0 $'tee -a docs/x.md <<\'EOF\'\n.env.local 은 무시\nEOF'
t 0 $'mkdir -p .local && cat > .local/a.md <<-\'EOF\'\n\t.env\n\tEOF\nls .local'

echo "## 파일 도구 차단 (exit 2)"
tf 2 Read file_path '.env'
tf 2 Read file_path '/Users/x/project/.env'
tf 2 Read file_path '/abs/path/.env.production'
tf 2 Edit file_path '.env'
tf 2 Write file_path '.env.local'
tf 2 MultiEdit file_path 'src/main/resources/.env'
tf 2 NotebookEdit notebook_path '.env'
tf 2 Grep path '.env'
tf 2 Grep glob '.env*'
tf 2 Glob pattern '**/.env*'

echo "## 파일 도구 통과 (exit 0)"
tf 0 Read file_path '.env.example'
tf 0 Read file_path 'src/main/resources/application.yml'
tf 0 Read file_path '.envrc'
tf 0 Edit file_path '.gitignore'
tf 0 Write file_path '.env.template'
tf 0 Grep glob '*.java'
tf 0 Grep pattern '.env'
tf 0 Glob pattern '**/*.java'

echo "## fail-closed (검사할 수 없으면 exit 2)"
printf 'not json' | bash "$HOOK" >/dev/null 2>&1
check 2 "잘못된 JSON 입력" "$?"

printf '{"tool_name":"Bash","tool_input":{}}' | bash "$HOOK" >/dev/null 2>&1
check 0 "command 필드 없음 (검사할 명령 없음)" "$?"

printf '{"tool_name":"Read","tool_input":{}}' | bash "$HOOK" >/dev/null 2>&1
check 0 "file_path 필드 없음 (검사할 경로 없음)" "$?"

# jq만 빠진 PATH를 만들어서 jq가 없는 PC를 흉내낸다
NOJQ=$(mktemp -d)
for bin in cat grep sed perl printf tr; do
  p=$(command -v "$bin") && [ -x "$p" ] && ln -s "$p" "$NOJQ/$bin"
done
printf '{"tool_name":"Bash","tool_input":{"command":"git status"}}' | PATH="$NOJQ" "$BASH" "$HOOK" >/dev/null 2>&1
check 2 "jq 없음" "$?"
rm -rf "$NOJQ"

echo
echo "PASS=$PASS FAIL=$FAIL"
[ "$FAIL" -eq 0 ]
