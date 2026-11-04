# Git 작업 규칙

> **적용 범위**: 항상 적용돼요 (`paths` 없음). 이 저장소에서 Git·GitHub 작업(브랜치, 커밋, push, PR)을 할 때 모두 해당돼요.
> **목적**: 4주차부터 바뀐 개인 누적 브랜치 흐름을 지키고, 민감 정보 유출과 되돌리기 어려운 Git 작업을 막는 거예요.
>
> 브랜치 이름, 주차별 흐름, 커밋 type prefix, 영구 금지 사항은 `CLAUDE.md`의 *Git workflow*, *Permanent prohibitions* 섹션이 기준이에요. 이 파일은 그 내용을 **실제로 실행할 때의 절차와 점검 항목**을 다뤄요. 두 문서가 다르면 `CLAUDE.md`를 따르고, 사용자에게 차이를 알려요.

---

## 1. 작업 전 확인

**목적**: 엉뚱한 브랜치에서 작업하거나 PR 대상을 잘못 고르는 실수를 막아요.

- 작업 전에 `git branch --show-current`, `git status`로 현재 브랜치와 작업 트리 상태를 확인해요.
- 4주차 하네스 작업은 `seungju/harness`에서 해요. 다른 브랜치에 있으면 바로 작업하지 말고 사용자에게 알려요.
- 작업 트리에 내가 만들지 않은 변경이 있으면 건드리지 않고(stash, restore, 삭제 금지) 사용자에게 먼저 알려요.
- 새 브랜치는 사용자가 요청했을 때만 만들어요. 기준 브랜치는 `CLAUDE.md`에 적힌 대로예요. (4주차: `seungju/harness`, 5~6주차: `seungju/basic`에서 만든 `seungju/monorepo`)

## 2. PR 규칙 (4주차)

**목적**: 4주차 결과물이 개인 누적 브랜치에 정확히 한 경로로 쌓이도록 해요.

- 4주차 PR은 **head `seungju/harness` → base `seungju/basic`** 하나예요.
  - 저장소 기본 브랜치가 `develop`이라서 `gh pr create`에서 base를 생략하면 `develop`으로 PR이 열려요. 항상 `--base seungju/basic --head seungju/harness`를 명시해요.
- `develop`, `main`을 base로 하는 PR은 만들지 않아요. (1~3주차 방식이에요.)
- PR 본문은 `.github/PULL_REQUEST_TEMPLATE.md`의 섹션 순서를 지키고 **작업 내용**, **관련 이슈(`closes #<번호>`)**, **리뷰 포인트**를 꼭 채워요. 이슈 번호를 모르면 지어내지 말고 사용자에게 물어봐요.
- PR 생성, merge, close, 리뷰 요청은 외부에 공개되는 작업이라 사용자가 요청했을 때만 해요. merge는 사용자가 직접 하도록 남겨둬요.

## 3. 안전한 커밋

**목적**: 커밋 하나에 의도한 변경만 담고, 요청 범위 밖의 파일이 섞이지 않도록 해요.

- 커밋과 push는 사용자가 요청했을 때만 해요. 요청은 그때 한 번에만 유효해요.
- 커밋 전에 `git status`와 `git diff --staged`로 들어갈 파일과 내용을 확인해요.
- `git add .`, `git add -A`는 쓰지 않고 **파일 경로를 하나씩 지정**해서 stage해요.
- 한 커밋에는 하나의 목적만 담아요. 문서 작업과 코드 변경이 섞이면 커밋을 나눌지 사용자에게 물어봐요.
- 커밋 메시지는 `CLAUDE.md`의 형식(`<type>: <한국어 요약>`)을 따르고, type은 실제 변경 내용과 맞춰요. (문서만 바꿨다면 `docs:`)
- 빌드 결과물과 IDE·로컬 파일(`build/`, `.gradle/`, `.idea/`, `out/`)은 커밋하지 않아요. `.gitignore`에 있어도 강제로 추가(`git add -f`)하지 않아요.
- pre-commit 등 hook이 실패하면 `--no-verify`로 넘어가지 말고 원인을 고치거나 사용자에게 알려요.

## 4. 민감 정보 보호

**목적**: DB 접속 정보 같은 비밀값이 Git 이력, PR, 외부 서비스에 남지 않도록 해요. 한 번 push되면 이력에서 지우기 어려워요.

- `.env`는 `.gitignore` 대상이에요. stage, 커밋, `git add -f` 모두 하지 않아요.
- `.env`의 값을 출력하거나(cat 등) 커밋 메시지, PR 본문, 이슈, 문서, 로그에 옮기지 않아요. 필요하면 변수 **이름**(`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`)만 써요.
- `application*.yml`에는 실제 값이 아니라 `${...}` 플레이스홀더만 둬요. 비밀값 기본값을 yml에 하드코딩하지 않아요.
- 커밋 전에 staged diff에서 비밀번호, 토큰, API 키, 실제 DB URL처럼 보이는 문자열이 있는지 확인해요.
- 민감 정보가 stage된 걸 발견하면 `git restore --staged <파일>`로 내리고 사용자에게 알려요.
- 이미 커밋됐거나 push됐다면 이력을 직접 고치지 말고, 즉시 사용자에게 알리고 비밀값 교체(rotation)가 필요하다고 안내해요.

## 5. 위험한 Git 명령 제한

**목적**: 원격 이력을 덮어쓰거나 작업 내용을 되돌릴 수 없게 지우는 일을 막아요.

다음 명령은 **사용자의 명시적인 승인 없이 실행하지 않아요.** 승인을 받을 때는 어떤 커밋·파일이 영향을 받는지 먼저 설명해요.

| 분류 | 명령 |
| --- | --- |
| 원격 이력 덮어쓰기 | `git push --force`, `git push --force-with-lease`, `git push -f` |
| 이력 재작성 | `git rebase`, `git commit --amend`(push된 커밋), `git filter-branch`, `git filter-repo` |
| 작업 내용 삭제 | `git reset --hard`, `git clean -f`/`-fd`, `git checkout -- .`, `git restore .` |
| 브랜치·태그 삭제 | `git branch -D`, `git push origin --delete <브랜치>`, `git tag -d` |
| 보호 브랜치 반영 | `main`, `develop`으로의 push와 merge (승인과 상관없이 금지예요. `CLAUDE.md` 참고) |

- push는 현재 작업 브랜치(`seungju/harness`)를 같은 이름의 원격 브랜치로만 해요. (`git push origin seungju/harness`) 다른 브랜치로 push하지 않아요.
- 충돌이 나면 임의로 한쪽을 선택하지 말고 충돌 파일과 내용을 사용자에게 보여주고 결정을 맡겨요.
