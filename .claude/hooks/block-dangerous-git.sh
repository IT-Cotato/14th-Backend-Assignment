#!/bin/bash
# PreToolUse(Bash) 훅: 되돌리기 어려운 git 명령(force push, reset --hard 등)을 차단한다.
# - 명령은 JSON 맨 위가 아니라 .tool_input.command 에 들어 있다.
# - 따옴표 안의 문자열(커밋 메시지, grep 패턴 등)은 명령이 아니므로 빼고 검사한다.
#   단, bash -c / sh -c / eval 은 따옴표 안이 실제 명령이라 원문 그대로 검사한다.
# - 검사할 수 없는 상황(jq/perl 없음, JSON 파싱 실패)에서는 통과시키지 않고 막는다. (fail-closed)
# - 테스트: bash .claude/hooks/test-block-dangerous-git.sh

block() {
  echo "Blocked by .claude/hooks/block-dangerous-git.sh: $1" >&2
  echo "되돌리기 어려운 명령이에요. 다른 방법을 찾거나, 꼭 필요하면 사용자에게 이유를 설명하고 직접 실행하도록 안내하세요." >&2
  exit 2
}

fail_closed() {
  echo "Blocked by .claude/hooks/block-dangerous-git.sh: $1" >&2
  echo "훅이 명령을 검사하지 못해서 안전을 위해 막았어요. 사용자에게 원인을 알리세요." >&2
  exit 2
}

command -v jq >/dev/null 2>&1 || fail_closed "jq가 설치되어 있지 않아요. (Mac: brew install jq)"
command -v perl >/dev/null 2>&1 || fail_closed "perl을 찾을 수 없어요."

INPUT=$(cat)
COMMAND=$(printf '%s' "$INPUT" | jq -r '.tool_input.command // empty' 2>/dev/null) \
  || fail_closed "훅 입력 JSON을 파싱하지 못했어요."

[ -z "$COMMAND" ] && exit 0

if echo "$COMMAND" | grep -Eq '(^|[[:space:];&|(])((ba|z)?sh[[:space:]]+-c|eval)[[:space:]]'; then
  SCAN="$COMMAND"
else
  SCAN=$(printf '%s' "$COMMAND" | perl -0pe "s/'[^']*'//g; s/\"(\\\\.|[^\"\\\\])*\"//g")
fi

# 명령 구분자(&& || ; | 줄바꿈)로 쪼개서 각 명령을 검사한다.
while IFS= read -r PART; do
  PART="${PART#"${PART%%[![:space:]]*}"}"   # 앞 공백 제거
  # sh -c "git ..." / eval 'git ...' 은 안쪽 명령만 남긴다
  PART=$(printf '%s' "$PART" | sed -E "s/^((ba|z)?sh[[:space:]]+-c|eval)[[:space:]]+//; s/^[\"'(]+//")

  # 원격 이력을 덮어쓰는 force push
  echo "$PART" | grep -Eq '^git([[:space:]]+-C[[:space:]]+[^[:space:]]+)?[[:space:]]+push([[:space:]].*)?[[:space:]](-[a-zA-Z]*f[a-zA-Z]*|--force(-with-lease)?)([^[:alnum:]_-]|$)' \
    && block "git push --force 는 원격 이력을 덮어써요."

  # 되돌리기 어려운 로컬 git 명령
  echo "$PART" | grep -Eq '^git[[:space:]].*reset[[:space:]]+(.*[[:space:]])?--hard' \
    && block "git reset --hard 는 커밋하지 않은 변경을 지워요."
  echo "$PART" | grep -Eq '^git[[:space:]].*clean[[:space:]]+(.*[[:space:]])?(-[a-zA-Z]*f|--force)' \
    && block "git clean -f 는 추적하지 않는 파일을 지워요."
  echo "$PART" | grep -Eq '^git[[:space:]].*branch[[:space:]]+(.*[[:space:]])?(-D|-d[[:space:]]+-f|--delete[[:space:]]+--force)([[:space:]]|$)' \
    && block "git branch -D 는 머지되지 않은 브랜치를 지워요."
  echo "$PART" | grep -Eq '^git[[:space:]].*checkout[[:space:]]+--[[:space:]]+\.([[:space:]]|$)' \
    && block "작업 트리 전체를 되돌리는 명령이에요."
  # restore . 는 막되, --staged 만 쓴 경우(스테이징 취소, 작업 트리는 그대로)는 통과시킨다
  if echo "$PART" | grep -Eq '^git[[:space:]].*restore[[:space:]]+(.*[[:space:]])?\.([[:space:]]|$)'; then
    if echo "$PART" | grep -Eq '[[:space:]](--staged|-S)([[:space:]]|$)' \
      && ! echo "$PART" | grep -Eq '[[:space:]](--worktree|-W|-SW|-WS)([[:space:]]|$)'; then
      :
    else
      block "작업 트리 전체를 되돌리는 명령이에요."
    fi
  fi
done <<< "$(printf '%s\n' "$SCAN" | perl -pe 's/(&&|\|\||;|\|)/\n/g')"

exit 0
