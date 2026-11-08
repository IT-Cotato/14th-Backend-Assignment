
# Git 작업 규칙

> 적용 범위: 항상 적용돼요 (paths 없음).
> 목적: 코테이토 5·6주차 모노레포 과제에서
> 브랜치 관리, 커밋, Push, PR 규칙을 일관되게 유지해요.

## 1. 작업 브랜치

현재 5·6주차 작업 브랜치:

`seungju/monorepo`

PR 대상 브랜치:

`seungju/basic`

브랜치 흐름:

`seungju/basic` → `seungju/monorepo`

PR:

`seungju/monorepo` → `seungju/basic`

### 작업 규칙

- 모든 5·6주차 작업은 seungju/monorepo에서 진행해요.
- 기능별 브랜치를 따로 만들지 않아요.
- 작업 시작 전에 현재 브랜치를 확인해요.
- 현재 브랜치가 다르면 사용자에게 알려요.
- main, develop 브랜치에서 직접 작업하지 않아요.
- 기존 변경사항을 임의로 삭제하거나 되돌리지 않아요.

작업 전 확인 명령어:

```bash
git branch --show-current
git status
```

4주차 작업 브랜치인 seungju/harness는
이전 과제 기록으로 유지해요.

## 2. 모노레포 규칙

백엔드와 프론트엔드는 하나의 Git 저장소에서 관리해요.

프로젝트 구성:

- backend/: Spring Boot
- frontend/: React
- docs/: ERD, API 명세, 기능 문서
- .claude/: Claude Code 하네스
- CLAUDE.md: 프로젝트 전역 지침

### 규칙

- 백엔드와 프론트엔드의 Git 저장소를 분리하지 않아요.
- 모든 커밋은 동일한 저장소에서 관리해요.
- 기존 하네스 파일을 유지해요.
- 프로젝트 경로 변경 시 관련 설정을 확인해요.
- backend와 frontend의 변경사항을 구분해서 기록해요.

## 3. 커밋 규칙

커밋과 Push는 사용자가 요청했을 때만 실행해요.

### 커밋 전 확인

```bash
git status
git diff
git diff --staged
```

- 커밋할 파일을 직접 확인해요.
- git add . 또는 git add -A를 사용하지 않아요.
- 필요한 파일만 경로를 지정해 Stage해요.
- 불필요한 파일이 포함되지 않았는지 확인해요.
- 다른 사람이 작성한 변경사항을 임의로 삭제하지 않아요.

### 커밋 단위

작업 내용을 논리적으로 구분해요.

권장 순서:

0. 모노레포 구조 및 환경 설정
1. 인증 기능
2. 스터디 모집글 기능
3. 댓글 기능
4. 테스트
5. 문서 및 최종 수정

- 한 커밋에는 하나의 목적을 담아요.
- 프로덕션 코드와 테스트 코드는 별도 커밋으로 구분해요.
- 관련 없는 변경사항을 한 커밋에 섞지 않아요.

### 커밋 메시지

type prefix와 한국어 요약을 사용해요.

예시:

- chore: 모노레포 프로젝트 구조 변경
- feat: JWT 회원가입 및 로그인 구현
- feat: 스터디 모집글 API 구현
- feat: 스터디 댓글 기능 구현
- fix: 작성자 권한 검증 수정
- refactor: 서비스 계층 분리
- test: Playwright E2E 테스트 추가
- docs: API 명세서 및 ERD 작성

## 4. 민감 정보 보호

다음 정보는 Git에 커밋하지 않아요.

- .env
- DB 비밀번호
- JWT Secret
- API Key
- 인증 토큰
- 기타 개인정보 및 비밀값

다음 파일과 폴더도 커밋하지 않아요.

- node_modules/
- backend/build/
- frontend/dist/
- .gradle/
- .idea/
- out/
- playwright-report/
- test-results/
- *.log

### 환경변수

- backend/.env는 Git에 올리지 않아요.
- backend/.env.example에는 변수 이름만 작성해요.
- 실제 비밀번호와 인증 정보를 예제 파일에 넣지 않아요.
- application.yml에는 환경변수 플레이스홀더를 사용해요.
- .gitignore를 확인해요.

### 민감 정보 발견 시

- 민감 정보의 내용을 출력하지 않아요.
- Stage된 민감 정보는 사용자에게 알려요.
- 이미 Push된 민감 정보는 즉시 알려요.
- 사용자 승인 없이 Git 이력을 재작성하지 않아요.

## 5. 위험한 Git 명령 제한

다음 명령어는 사용자의 명시적인 승인 없이 실행하지 않아요.

### 강제 Push

- git push --force
- git push --force-with-lease
- git push -f
- +로 시작하는 강제 Push refspec

### 이력 변경

- git rebase
- Push된 커밋에 대한 git commit --amend
- git filter-branch
- git filter-repo

### 파일 및 변경사항 삭제

- git reset --hard
- git clean -f
- git clean -fd
- git checkout -- .
- git restore .

### 브랜치 삭제

- git branch -D
- git push origin --delete
- git tag -d

### 보호 브랜치

- main에 직접 Push하지 않아요.
- develop에 직접 Push하지 않아요.
- main 또는 develop으로 직접 Merge하지 않아요.

### 안전 규칙

- 위험한 명령은 사용자의 승인을 먼저 받아요.
- 명령 실행 전에 영향을 받는 파일과 커밋을 설명해요.
- 충돌이 발생하면 사용자에게 내용을 알려요.
- Git Hook을 우회하지 않아요.
- --no-verify를 사용해서 검사를 건너뛰지 않아요.

## 6. Push 규칙

현재 작업 브랜치:

`seungju/monorepo`

Push 대상:

`origin seungju/monorepo`

사용자가 Push를 요청한 경우에만 다음 명령어를 사용해요.

```bash
git push origin seungju/monorepo
```

- main 또는 develop으로 Push하지 않아요.
- seungju/basic에 직접 Push하지 않아요.
- 강제 Push하지 않아요.
- Push 전에 브랜치와 변경사항을 확인해요.

## 7. Issue 규칙

5·6주차 과제에서는 Issue를 하나만 만들어요.

제목 예시:

`feat: [이승주] 코테이토 스터디 매칭 플랫폼`

Issue에는 다음 내용을 포함해요.

- 모노레포 구조 변경
- ERD 작성
- API 명세 작성
- 인증 기능 구현
- 스터디 모집글 구현
- 댓글 기능 구현
- 백엔드 단위 테스트
- 프론트엔드 빌드
- Playwright E2E 테스트
- 기능 문서 작성
- E2E 결과 캡처

Issue 생성은 사용자가 요청했을 때만 해요.

## 8. Pull Request 규칙

5·6주차 과제에서는 PR을 하나만 만들어요.

Head:

`seungju/monorepo`

Base:

`seungju/basic`

PR 제목 예시:

`feat: [이승주] 코테이토 스터디 매칭 플랫폼 (closes #이슈번호)`

### 필수 내용

PR 본문에는 다음 항목을 포함해요.

1. 개요
2. Backend 작업 내용
3. Frontend 작업 내용
4. API 변경 사항
5. ERD 및 문서 변경
6. E2E 테스트 결과
7. 백엔드 및 프론트엔드 테스트 결과
8. Claude Code 하네스 활용
9. 관련 Issue
10. 리뷰 포인트
11. 제출 전 체크리스트

### E2E 결과

- 정상 및 에러 시나리오를 테스트해요.
- 인증, 모집글, 댓글 기능을 모두 검증해요.
- npx playwright test 통과 결과를 첨부해요.
- 터미널 캡처에 테스트 결과가 보여야 해요.
- Playwright 테스트 코드도 PR에 포함해요.

### PR 생성 규칙

- PR은 사용자가 요청한 경우에만 생성해요.
- Base 브랜치를 반드시 확인해요.
- 기본 브랜치인 develop을 Base로 사용하지 않아요.
- Issue 번호를 임의로 생성하지 않아요.
- closes #이슈번호 형식으로 연결해요.
- Merge는 사용자가 직접 수행하도록 남겨둬요.

## 9. 변경사항 점검

커밋 또는 PR 전에 다음 항목을 확인해요.

- [ ] 현재 브랜치가 seungju/monorepo인가요?
- [ ] 민감 정보가 포함되지 않았나요?
- [ ] backend와 frontend 변경사항을 확인했나요?
- [ ] 불필요한 빌드 파일이 제외되어 있나요?
- [ ] CLAUDE.md가 최신 구조를 반영하나요?
- [ ] ERD와 API 명세가 실제 구현과 일치하나요?
- [ ] 백엔드 테스트가 통과하나요?
- [ ] 프론트엔드 빌드가 성공하나요?
- [ ] Playwright E2E가 통과하나요?
- [ ] PR Base가 seungju/basic인가요?
- [ ] 관련 Issue가 연결되어 있나요?

## 10. 기존 프로젝트 보호

- 1~4주차 코드를 임의로 삭제하지 않아요.
- 기존 BookController와 HelloController를
  요청 없이 수정하지 않아요.
- 기존 Git 이력을 변경하지 않아요.
- 기존 Claude Code 하네스를 유지해요.
- 요청 범위에 없는 파일은 수정하지 않아요.
- 문제가 발견되면 사용자에게 먼저 알려요.
