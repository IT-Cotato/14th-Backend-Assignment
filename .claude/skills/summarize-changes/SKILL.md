
---
name: summarize-changes
description: Git 변경사항을 요약하고 민감 정보 노출, 불필요한 파일 변경, 프로젝트 규칙 위반, 모노레포 구조 문제를 점검하는 읽기 전용 스킬이에요. 사용자가 코드 변경사항 확인, 변경 내용 요약, 커밋 전 검토를 요청할 때 사용해요.
---

# summarize-changes

현재 Git 작업 트리의 변경사항을 읽어서 한국어로 보고해요.

보고 항목:
1. 변경사항 요약
2. 위험 요소
3. 검증 필요 사항

## 0. 절대 규칙: 읽기 전용

이 스킬은 Git 상태와 파일을 읽기만 해요.

허용하는 Git 명령:
- git status
- git diff
- git diff --staged
- git diff --stat
- git ls-files
- git log
- git branch --show-current

금지하는 Git 명령:
- git add
- git commit
- git push
- git reset
- git restore
- git checkout
- git switch
- git stash
- git clean
- git rm
- git mv
- git rebase
- git merge
- git tag
- git branch -d
- git branch -D

이 스킬은 파일을 생성, 수정, 삭제하지 않아요.

문제를 발견하면 보고만 해요.

민감 정보가 들어 있는 파일은 내용을 열거나 출력하지 않아요.

사용자가 별도로 요청하지 않는 한 테스트나 빌드도
직접 실행하지 않고 필요한 명령어만 안내해요.

## 1. 현재 브랜치 확인

다음 명령어를 사용해요.

```bash
git branch --show-current
git status
```

5·6주차 작업 브랜치:
`seungju/monorepo`

PR 대상 브랜치:
`seungju/basic`

확인 사항:
- 현재 브랜치가 seungju/monorepo인지 확인해요.
- main, develop에서 작업 중이면 위험 요소로 보고해요.
- 기존 변경사항을 임의로 되돌리지 않아요.
- 브랜치를 자동으로 변경하지 않아요.

## 2. 변경 파일 확인

다음 명령어를 사용해요.

```bash
git diff --stat
git diff --staged --stat
git status --short
```

변경사항을 다음과 같이 구분해요.

- staged: 커밋 준비가 된 파일
- unstaged: 아직 Stage하지 않은 변경
- untracked: Git이 추적하지 않는 새 파일

필요한 경우 파일별 diff를 확인해요.

```bash
git diff -- <파일경로>
git diff --staged -- <파일경로>
```

민감 정보가 포함된 파일은 내용을 출력하지 않아요.

새로운 파일 목록은 다음 명령어로 확인해요.

```bash
git ls-files --others --exclude-standard
```

파일을 직접 읽지 않았다면 확인한 것으로 간주하지 않아요.

## 3. 모노레포 변경사항 분석

현재 프로젝트 구조:

14th-Backend-Assignment/
- backend/
- frontend/
- docs/
- .claude/
- .github/
- CLAUDE.md

변경 파일을 다음과 같이 분류해요.

### Backend

`backend/`

확인 사항:
- Controller
- Service
- Repository
- Entity
- DTO
- Security
- API 응답 및 예외 처리
- Gradle 의존성
- 환경 설정
- 테스트

### Frontend

`frontend/`

확인 사항:
- React 컴포넌트
- 페이지 및 라우팅
- Axios API 호출
- JWT 토큰 처리
- Tailwind CSS
- TypeScript 오류 가능성
- Playwright E2E 테스트

### Documentation

`docs/`

확인 사항:
- ERD
- API 명세
- 기능 문서
- 실제 구현과 문서의 일치 여부

### Claude Code Harness

`.claude/`
`CLAUDE.md`

확인 사항:
- Rules 변경
- Skills 변경
- Hooks 변경
- settings.json 변경
- 모노레포 경로 반영
- 기존 하네스 기능 유지 여부

## 4. 민감 정보 점검

다음 파일과 정보가 Git에 포함되지 않았는지 확인해요.

- backend/.env
- frontend/.env
- DB 비밀번호
- JWT Secret
- API Key
- Access Token
- 개인 인증 정보
- *.pem
- *.key
- credentials 파일

민감 정보 파일은 내용을 읽거나 출력하지 않아요.

다음 항목을 확인해요.

- .env 파일이 추적되고 있지 않은가?
- 민감 정보가 Stage되어 있지 않은가?
- 환경변수 대신 비밀번호를 하드코딩하지 않았는가?
- JWT Secret이 코드에 직접 작성되어 있지 않은가?
- 실제 DB 접속 정보가 문서에 포함되지 않았는가?

문제가 발견되면 사용자에게 알려요.

이 스킬에서 직접 수정하지 않아요.

이미 Push된 민감 정보는 이력 재작성 없이 사용자에게
문제와 비밀값 교체 필요성을 알려요.

## 5. 불필요한 파일 점검

다음 파일과 폴더가 커밋 대상에 포함되지 않았는지 확인해요.

- backend/build/
- frontend/dist/
- frontend/node_modules/
- .gradle/
- .idea/
- out/
- playwright-report/
- test-results/
- *.log

확인 사항:
- 빌드 결과물이 포함되지 않았는가?
- IDE 설정 파일이 포함되지 않았는가?
- 요청 범위 밖의 파일이 수정되지 않았는가?
- 불필요한 대량 삭제가 발생하지 않았는가?
- 의미 없는 포맷 변경이 섞이지 않았는가?

필요한 경우 파일 목록과 변경 내용을 구분해서 보고해요.

## 6. Backend 규칙 점검

기준:
- CLAUDE.md
- .claude/rules/controller-rules.md

확인 사항:

- 신규 API가 /api로 시작하는가?
- Controller → Service → Repository 구조를 사용하는가?
- Entity를 API 응답으로 직접 반환하지 않는가?
- ApiResponse 공통 응답 형식을 사용하는가?
- ResponseUtils를 올바르게 사용하는가?
- 실패 응답에 Backend14thException과 ErrorCode를 사용하는가?
- HTTP 204 응답에 불필요한 본문이 없는가?
- DTO 입력값 검증이 적용되어 있는가?
- 검증 실패 시 400 응답을 반환하는가?
- JPA 연관관계가 LAZY인가?
- Enum이 문자열로 저장되는가?
- 인증과 작성자 권한을 서버에서 확인하는가?
- Swagger 문서가 작성되어 있는가?

기존 BookController와 HelloController는
요청 없이 수정하지 않았는지도 확인해요.

## 7. Frontend 규칙 점검

확인 사항:

- React와 TypeScript를 사용하는가?
- Vite 기반 프로젝트인가?
- Tailwind CSS를 사용하는가?
- React Router 경로가 올바른가?
- Axios 요청이 API 명세와 일치하는가?
- 프론트엔드 개발 서버 포트가 3000인가?
- 로그인 상태에 따라 헤더가 변경되는가?
- 인증이 필요한 화면을 보호하는가?
- 서버에서 401이 반환되면 로그인 화면으로 이동하는가?
- 서버의 에러 메시지를 사용자에게 표시하는가?
- 로딩 상태를 표시하는가?
- 모바일 화면을 고려했는가?

확인하지 못한 항목은 확인이 필요하다고 보고해요.

## 8. API 명세 일치 여부

기준 문서:
`docs/api-spec.md`

다음 기능을 확인해요.

### Authentication
- 회원가입
- 로그인
- 로그아웃
- 내 정보 조회

### Study Posts
- 작성
- 목록
- 검색 및 필터
- 상세
- 수정
- 삭제

### Comments
- 작성
- 목록
- 수정
- 삭제

총 13개 API를 기준으로 확인해요.

확인 사항:
- HTTP Method
- URL
- Request DTO
- Response DTO
- HTTP Status
- ErrorCode
- 인증 및 권한
- 페이지네이션
- 검색 및 필터

실제 코드와 명세를 모두 확인한 항목만
검증 완료로 표시해요.

## 9. 테스트 점검

### Backend

실행 명령어:

```powershell
cd backend
.\gradlew.bat test
```

확인 사항:
- 서비스 로직 단위 테스트
- 입력 검증
- 인증
- 작성자 권한
- 기존 테스트 통과 여부

### Frontend

실행 명령어:

```powershell
cd frontend
npm run build
```

확인 사항:
- TypeScript 컴파일
- Vite 빌드

Lint 명령어는 package.json에 정의된 경우에만 안내해요.

### E2E

실행 명령어:

```powershell
cd frontend
npx playwright test
```

확인 사항:
- 인증 정상 시나리오
- 인증 에러 시나리오
- 모집글 정상 시나리오
- 모집글 에러 시나리오
- 댓글 정상 시나리오
- 댓글 에러 시나리오

실제로 테스트를 실행하지 않았다면
통과했다고 보고하지 않아요.

사용자가 제공한 테스트 결과가 있다면
해당 결과를 근거로 보고해요.

## 10. 문서 및 하네스 점검

필수 문서:
- docs/erd/ ERD 이미지
- docs/api-spec.md
- docs/features/ 기능 문서 1개

하네스:
- CLAUDE.md
- .claude/settings.json
- .claude/hooks/
- .claude/rules/
- .claude/skills/

확인 사항:
- 경로가 모노레포 구조와 일치하는가?
- 기존 4주차 규칙이 유지되는가?
- 불필요한 설정 삭제가 없는가?
- 새 기능에 필요한 규칙이 추가되었는가?
- 문서와 실제 구현 내용이 일치하는가?

## 11. 변경사항이 없을 때

staged, unstaged, untracked 항목이 모두 없다면
다음 형식으로 보고해요.

현재 브랜치 `<브랜치명>`에는 변경사항이 없어요.

- staged: 0개
- unstaged: 0개
- untracked: 0개

## 12. 보고 형식

한국어로 다음 세 섹션을 구분해서 작성해요.

### 변경사항 요약

- 현재 브랜치
- staged 파일 개수
- unstaged 파일 개수
- untracked 파일 개수

변경 파일을 표로 정리해요.

| 파일 | 구분 | 상태 | 주요 변경 | 주의할 점 |
|---|---|---|---|---|
| 파일 경로 | Backend/Frontend/Docs/Harness | 수정 | 변경 내용 | 확인 사항 |

새 파일은 직접 내용을 확인한 파일과
목록만 확인한 파일을 구분해요.

### 위험 요소

다음 범주로 정리해요.

- 민감 정보
- 불필요한 파일
- Git 브랜치
- Backend 규칙
- Frontend 규칙
- API 명세 불일치
- 하네스 설정

문제가 없다면 다음과 같이 작성해요.

`확인한 범위에서 발견된 위험 요소 없음`

### 검증 필요 사항

다음 내용을 포함해요.

- 실행이 필요한 테스트
- 확인하지 못한 파일
- API 명세 검증 필요 항목
- ERD와 Entity 일치 여부
- E2E 테스트 필요 항목

## 13. 스킬 종료 규칙

- 모든 보고는 확인한 사실에 근거해요.
- 확인하지 않은 사항은 검증 완료로 표시하지 않아요.
- 민감 정보를 출력하지 않아요.
- 코드를 임의로 수정하지 않아요.
- Git 상태를 변경하지 않아요.
- 커밋, Push, PR을 자동으로 생성하지 않아요.
- 사용자 요청 없이 위험한 Git 명령을 실행하지 않아요.
