#!/bin/bash
# PreToolUse 훅: .env 비밀 파일을 읽거나 수정하거나 출력하는 도구 호출을 차단한다.
# - Bash: 명령에 .env 토큰이 있거나, .env에 걸리는 와일드카드(cat .* 등)나
#   재귀 검색(grep -r, rg -uu 등)이 있으면 막는다.
# - 파일 도구(Read/Edit/Write/MultiEdit/NotebookEdit/Grep/Glob): 대상 경로가 .env 계열이면 막는다.
# - .env.example / .env.sample / .env.template 은 비밀값이 없는 템플릿이라 허용한다.
# - 글자로만 쓰인 .env 는 명령이 아니므로 검사에서 뺀다. (오탐 방지, .dev/learnings/002)
#   - git commit -m/--message, gh pr|issue create|edit|comment 의 --title/--body 인자
#   - cat/tee 로 넘기는 heredoc 본문 (cat > draft.md <<'EOF' ... EOF)
#   단 그 안에 $( 나 ` 가 있으면 실제로 실행되므로 지우지 않고 검사한다. (git commit -m "$(cat .env)" 차단)
#   heredoc 줄에 | 가 있으면(cat <<'EOF' | bash) 본문이 실행될 수 있어 지우지 않는다.
# - 검사할 수 없는 상황(jq/perl 없음, JSON 파싱 실패)에서는 통과시키지 않고 막는다. (fail-closed)
# - 1차 방어는 settings.json 의 permissions.deny, 이 훅은 2차 방어다.
# - 알려진 한계: find -exec cat, xargs, 변수/명령 치환(f=.e; cat ${f}nv), base64 같은 난독화는 못 막는다.
#   실수 방지용 장치이지 보안 경계가 아니다.
# - 테스트: bash .claude/hooks/test-block-env-access.sh

block() {
  echo "Blocked by .claude/hooks/block-env-access.sh: $1" >&2
  echo ".env 에는 비밀값이 있어서 읽거나 수정하거나 출력할 수 없어요. 값이 필요하면 사용자에게 직접 요청하세요." >&2
  [ -n "$2" ] && echo "$2" >&2
  exit 2
}

fail_closed() {
  echo "Blocked by .claude/hooks/block-env-access.sh: $1" >&2
  echo "훅이 명령을 검사하지 못해서 안전을 위해 막았어요. 사용자에게 원인을 알리세요." >&2
  exit 2
}

command -v jq >/dev/null 2>&1 || fail_closed "jq가 설치되어 있지 않아요. (Mac: brew install jq)"
command -v perl >/dev/null 2>&1 || fail_closed "perl을 찾을 수 없어요."

# 경로의 마지막 이름이 비밀 .env 파일이면 0
is_secret_env() {
  local name="${1##*/}"
  case "$name" in
    .env.example|.env.sample|.env.template) return 1 ;;
    .env|.env.*) return 0 ;;
  esac
  return 1
}

# 와일드카드 단어가 .env 계열 파일에 걸리면 0
# 셸 기본 동작처럼 점으로 시작하는 패턴만 숨김 파일에 걸린다고 본다. (cat * 은 .env 를 안 읽음)
glob_hits_env() {
  local base="${1##*/}"
  [[ "$base" == .* ]] || return 1
  # shellcheck disable=SC2053
  [[ .env == $base || .env.local == $base || .env.production == $base ]]
}

INPUT=$(cat)
TOOL=$(printf '%s' "$INPUT" | jq -r '.tool_name // empty' 2>/dev/null) \
  || fail_closed "훅 입력 JSON을 파싱하지 못했어요."

# ---------- 파일 도구 ----------
if [ "$TOOL" != "Bash" ]; then
  PATHS=$(printf '%s' "$INPUT" | jq -r --arg t "$TOOL" '
    .tool_input // {} |
    [.file_path, .notebook_path, .path, .glob, (if $t == "Glob" then .pattern else null end)]
    | map(select(type == "string" and . != "")) | .[]' 2>/dev/null) \
    || fail_closed "훅 입력 JSON을 파싱하지 못했어요."

  while IFS= read -r P; do
    [ -z "$P" ] && continue
    is_secret_env "$P" && block "$TOOL 대상이 .env 파일이에요: $P"
    case "$P" in
      *[\*\?\[]*) glob_hits_env "$P" && block "$TOOL 패턴이 .env 파일에 걸려요: $P" ;;
    esac
  done <<< "$PATHS"
  exit 0
fi

# ---------- Bash ----------
COMMAND=$(printf '%s' "$INPUT" | jq -r '.tool_input.command // empty' 2>/dev/null) \
  || fail_closed "훅 입력 JSON을 파싱하지 못했어요."
[ -z "$COMMAND" ] && exit 0

# 글자로만 쓰인 부분(데이터)을 지운다. $( 나 ` 가 있으면 실행되므로 남긴다.
read -r -d '' STRIP_DATA <<'PERL'
sub runs_code { return $_[0] =~ /\$\(|`/ }

# (a) cat/tee 로 넘기는 heredoc 본문
#     구분자를 따옴표로 감쌌으면('EOF') 본문이 펼쳐지지 않으므로 데이터.
#     안 감쌌어도 본문에 $( 나 ` 가 없으면 데이터. heredoc 줄에 | 가 있으면 남긴다.
s{((?:^|[\n;&|(])[ \t]*(?:cat|tee)\b[^\n]*?<<-?[ \t]*(['"\\]?)(\w+)['"]?[^\n]*)(.*?)(\n[ \t]*\3[ \t]*(?=\n|\)|$))}{
  my ($head, $q, $body, $tail) = ($1, $2, $4, $5);
  ($head !~ /\|/ && ($q ne '' || !runs_code($body))) ? "$head$tail" : "$head$body$tail"
}gse;

# (b) 메시지 인자. 실행되는 내용이 있으면 플래그만 _KEPT_ 로 바꿔 남긴다. (같은 인자를 다시 잡지 않게)
1 while s{((?:^|[;&|\n(])[ \t]*git\b[^;&|\n]*?\bcommit\b[^;&|\n]*?)\s(?:-m|--message)(?:=|\s+)("(?:\\.|[^"\\])*"|'[^']*'|[^\s;&|]+)}{
  runs_code($2) ? "$1 _KEPT_ $2" : $1
}e;
1 while s{((?:^|[;&|\n(])[ \t]*gh\s+(?:pr|issue)\s+(?:create|edit|comment)\b[^;&|\n]*?)\s(?:-t|--title|-b|--body)(?:=|\s+)("(?:\\.|[^"\\])*"|'[^']*'|[^\s;&|]+)}{
  runs_code($2) ? "$1 _KEPT_ $2" : $1
}e;

# (c) .env 를 "제외"하는 인자(grep --exclude=.env*, rg -g '!.env*')는 읽는 게 아니다
s{--exclude(?:=|\s+)["']?\.env[^\s;&|]*}{}g;
s{(?:-g|--glob)(?:=|\s+)["']?!\.env[^\s;&|]*}{}g;
PERL
SCAN=$(printf '%s' "$COMMAND" | perl -0pe "$STRIP_DATA") \
  || fail_closed "명령에서 데이터 부분을 분리하지 못했어요."

# 1. .env 토큰 (따옴표 안도 검사: cat ".env", sh -c 'cat .env')
while IFS= read -r TOKEN; do
  [ -z "$TOKEN" ] && continue
  is_secret_env "$TOKEN" && block "명령에 .env 파일이 들어 있어요: $TOKEN" \
    "문서나 초안에 .env 라는 글자를 쓰려는 거라면 Bash(echo, heredoc) 대신 Write/Edit 도구를 쓰세요. (내용은 검사하지 않음)"
done <<< "$(printf '%s' "$SCAN" | perl -ne 'print "$1\n" while /(?:^|[^A-Za-z0-9_.\-])(\.env(?:\.[A-Za-z0-9_.\-]+)?)(?=[^A-Za-z0-9_.\-]|$)/g')"

# 따옴표 안 문자열은 셸이 펼치지 않으므로 와일드카드/플래그 검사에서는 뺀다
NOQUOTE=$(printf '%s' "$SCAN" | perl -0pe "s/'[^']*'//g; s/\"(\\\\.|[^\"\\\\])*\"//g")

# 2. .env 에 걸리는 와일드카드 (cat .*, cat .e*, less .en?)
# set -f: 단어 분리할 때 훅 자신이 와일드카드를 실제 파일로 펼치지 않게 한다
set -f
for WORD in $(printf '%s' "$NOQUOTE" | tr ';&|()<>' ' '); do
  case "$WORD" in
    *[\*\?\[]*) glob_hits_env "$WORD" && block "와일드카드가 .env 파일에 걸려요: $WORD" \
      "숨김 파일 전체가 아니라 필요한 파일 이름을 직접 적으세요." ;;
  esac
done
set +f

# 3. .env 까지 훑는 재귀 검색
HAS_GREP_EXCLUDE=0
echo "$COMMAND" | grep -Eq -- "--exclude(=|[[:space:]]+)[\"']?\\.env" && HAS_GREP_EXCLUDE=1
HAS_RG_EXCLUDE=0
echo "$COMMAND" | grep -Eq -- "(-g|--glob)(=|[[:space:]]+)[\"']?!\\.env" && HAS_RG_EXCLUDE=1

while IFS= read -r PART; do
  PART="${PART#"${PART%%[![:space:]]*}"}"   # 앞 공백 제거
  PART=$(printf '%s' "$PART" | sed -E 's/^[(]+[[:space:]]*//; s/^sudo[[:space:]]+//')

  if echo "$PART" | grep -Eq '^(e|f)?grep[[:space:]]' \
    && echo " $PART " | grep -Eq '[[:space:]](-[a-zA-Z]*[rR][a-zA-Z]*|--recursive|--dereference-recursive)[[:space:]]' \
    && [ "$HAS_GREP_EXCLUDE" -eq 0 ]; then
    block "재귀 grep 은 .env 내용까지 출력할 수 있어요." \
      "--exclude='.env*' 를 붙이거나, .gitignore 와 숨김 파일을 건너뛰는 rg 를 쓰세요."
  fi

  if echo "$PART" | grep -Eq '^rg[[:space:]]' \
    && echo " $PART " | grep -Eq '[[:space:]](--hidden|--no-ignore[a-z-]*|-uu+|-[a-zA-Z]*u[a-zA-Z]*u[a-zA-Z]*|-\.)[[:space:]]' \
    && [ "$HAS_RG_EXCLUDE" -eq 0 ]; then
    block "숨김/무시 파일까지 보는 rg 는 .env 내용을 출력할 수 있어요." \
      "-g '!.env*' 를 붙이거나 옵션 없이 rg 를 쓰세요."
  fi
done <<< "$(printf '%s\n' "$NOQUOTE" | perl -pe 's/(&&|\|\||;|\|)/\n/g')"

exit 0
