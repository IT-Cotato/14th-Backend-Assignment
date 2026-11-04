---
name: summarize-changes
description: Git 변경사항을 요약하고 민감 정보 노출, 불필요한 파일 변경, 프로젝트 규칙 위반 같은 위험 요소를 점검하는 읽기 전용 스킬이에요. 사용자가 코드 변경사항 확인, 변경 내용 요약, 커밋 전 검토를 요청할 때 사용해요.
---

# summarize-changes

현재 작업 트리의 Git 변경사항을 읽어서 **변경사항 요약**, **위험 요소**, **검증 필요 사항**으로 나눠 한국어로 보고해요.

## 0. 절대 규칙: 읽기 전용

이 스킬은 Git 상태와 파일을 **읽기만** 해요.

- 실행해도 되는 명령: `git status`, `git diff`, `git diff --staged`, `git ls-files`, `git log`, `git branch --show-current`, 그리고 파일 읽기
- 실행하면 안 되는 명령: `git add`, `git commit`, `git push`, `git reset`, `git restore`, `git checkout`, `git switch`, `git stash`, `git clean`, `git rm`, `git mv`, `git rebase`, `git merge`, `git tag`, `git branch -d/-D` 등 Git 상태를 바꾸는 모든 명령
- 파일을 만들거나 고치거나 지우지 않아요. 문제를 발견해도 직접 고치지 않고 보고만 해요. 수정은 사용자가 따로 요청했을 때 이 스킬 밖에서 해요.
- `.env`처럼 비밀값이 들어 있는 파일은 내용을 열어 출력하지 않아요. 보고서에도 변수 **이름**만 쓰고 값은 쓰지 않아요.

## 1. 변경 파일 확인

```bash
git branch --show-current
git status
```

- 현재 브랜치를 기록해요. 4주차 작업인데 `seungju/harness`가 아니면 위험 요소에 적어요.
- staged, unstaged, untracked 파일을 구분해서 목록을 만들어요.

## 2. 추적 중인 파일의 변경 내용 확인

```bash
git diff
git diff --staged
```

- `git diff`는 아직 stage하지 않은 변경, `git diff --staged`는 stage된 변경이에요. 둘 다 봐요.
- diff가 너무 길면 `git diff --stat`, `git diff --staged --stat`으로 전체 규모를 먼저 보고, 파일별로 `git diff -- <파일>`로 나눠서 읽어요.

## 3. 추적되지 않은 새 파일 확인

```bash
git ls-files --others --exclude-standard
```

- 새 파일은 `git diff`에 나오지 않아요. 내용을 알고 싶으면 파일을 직접 읽어야 해요.
- 새 파일이 많으면 주요 파일만 읽고, **읽은 파일과 읽지 않은 파일을 보고서에 구분해서 적어요.**
- 직접 읽지 않은 파일은 "목록만 확인했고 내용은 확인하지 않았어요"라고 적어요. 파일 이름만 보고 내용을 추측해서 확인한 것처럼 쓰지 않아요.
- 비밀값이 들어 있을 수 있는 파일(`.env`, `*.pem`, `*.key`, `credentials*` 등)이 목록에 있으면 내용을 열지 않고, 파일이 존재한다는 사실만 위험 요소로 보고해요.

## 4. 변경사항이 없을 때

1~3단계 결과가 모두 비어 있으면(staged·unstaged 변경도, 새 파일도 없으면) 다음처럼 안내하고 끝내요.

> 현재 브랜치 `<브랜치명>`에는 변경사항이 없어요. (staged, unstaged, untracked 모두 없음)

## 5. 파일별 주요 수정사항과 주의할 점 요약

변경 파일마다 다음을 정리해요.

- 무엇이 바뀌었는지 (추가 / 수정 / 삭제 / 이름 변경)
- 변경의 의도로 보이는 것 (diff나 직접 읽은 내용에 근거해서만 써요)
- 주의할 점 (동작 변경, 다른 파일에 미치는 영향, 누락돼 보이는 짝 변경 등)

## 6. 위험 요소 점검

아래 항목을 하나씩 점검해요. 기준은 `CLAUDE.md`와 `.claude/rules/git-workflow.md`예요.

### 민감 정보 노출

- `.env`가 staged 상태이거나 추적되고 있는지
- diff나 읽은 파일에 비밀번호, 토큰, API 키, 실제 DB URL·계정처럼 보이는 문자열이 있는지
- `application*.yml`에 `${...}` 플레이스홀더 대신 실제 값이나 비밀값 기본값이 하드코딩됐는지

### 불필요한 파일 변경

- 빌드 결과물·IDE·로컬 파일(`build/`, `.gradle/`, `.idea/`, `out/`, `*.log` 등)이 변경 목록에 있는지
- 요청 범위와 상관없어 보이는 파일 수정, 의미 없는 공백·줄바꿈·포맷 변경이 섞였는지
- 대량 삭제나 파일 전체 재작성처럼 보이는 변경이 있는지

### 프로젝트 규칙 위반 가능성

- 현재 브랜치가 주차별 작업 브랜치와 맞는지 (`main`, `develop`에서 직접 작업 중이면 반드시 보고)
- 성격이 다른 변경(문서와 코드 등)이 한 커밋에 섞일 상황인지
- 코드 컨벤션: 성공 응답은 `ResponseUtils.ok/created`와 `ApiResponse` 사용, 에러는 `Backend14thException(ErrorCode.X)` 사용, 새 `ErrorCode`는 도메인별 그룹과 `BOOK-001` 같은 코드 형식, 엔티티는 `BaseEntity` 상속, 컨트롤러에 `@Tag`/`@Operation` 적용
- CORS 설정을 바꿨다면 `CorsConfig`의 두 설정(`WebMvcConfigurer`, `CorsConfigurationSource`)이 함께 바뀌었는지
- 주석·API 설명은 `~해요` 체, `ErrorCode` 메시지는 `~습니다` 체인지

해당 사항이 없으면 "발견된 문제 없음"이라고 적어요. 근거 없이 문제를 만들어내지 않아요.

## 7. 보고 형식

한국어로, 아래 세 섹션을 구분해서 보고해요.

```markdown
## 변경사항 요약
- 브랜치: `<브랜치명>`
- staged: N개 / unstaged: N개 / untracked: N개

| 파일 | 상태 | 주요 수정사항 | 주의할 점 |
| --- | --- | --- | --- |
| `path/to/file` | 수정(unstaged) | ... | ... |

- 내용을 직접 확인한 새 파일: ...
- 목록만 확인하고 내용은 확인하지 않은 새 파일: ...

## 위험 요소
- [민감 정보] ...
- [불필요한 변경] ...
- [규칙 위반 가능성] ...
(없으면 "발견된 위험 요소 없음")

## 검증 필요 사항
- 사용자가 직접 확인하거나 실행해 봐야 하는 것 (예: `./gradlew build`, 특정 API 수동 호출, 내용을 확인하지 못한 파일 검토)
```

- 보고는 확인한 사실에 근거해서만 해요. 확인하지 못한 부분은 확인하지 못했다고 분명히 적어요.
- 보고가 끝나도 커밋, push, stage 같은 후속 작업은 하지 않아요. 사용자가 원하면 따로 요청하도록 안내만 해요.
