# CLAUDE.md

## 프로젝트 개요

코테이토 스터디 매칭 플랫폼을 만드는 모노레포 프로젝트다. (5,6주차 과제, 이슈 #127)
백엔드는 `backend/`, 프론트엔드는 `frontend/`에서 관리한다.

### 도메인

- 인증: 이메일·비밀번호 회원가입(기수, 파트 포함), 로그인(JWT), 로그아웃, 내 정보
- 스터디 모집글: 작성, 목록(검색·모집 상태·파트 필터), 상세, 수정, 삭제
- 댓글: 모집글에 댓글을 남기는 것이 곧 스터디 신청이다. 작성, 목록, 수정, 삭제
- 요구사항 원문은 로컬 전용 `docs/assignments/week-05-06.md`에 있다. Git에는 올리지 않는다.

### 백엔드 스택

Java 21과 Spring Boot 기반이다.
Gradle, Spring Data JPA, MySQL, Spring Security, SpringDoc, Lombok을 사용한다.
기본 패키지는 `com.cotato.backend14th`다.
현재 구현 상태는 문서가 아니라 실제 코드(`backend/src/main/java`)를 기준으로 확인한다.
설명과 문서는 한국어로 작성하고, 코드 식별자와 명령어는 원문을 유지한다.

### 프론트엔드 스택

React, TypeScript, Vite, Tailwind CSS, React Router, axios를 사용한다. Next.js는 사용하지 않는다.
버전은 `frontend/package.json`을 기준으로 확인한다.
개발 서버는 3000번 포트로 고정하며, 백엔드 CORS 설정이 이 포트를 허용한다.
입력창에는 `label`을 연결하고, 동작은 `button`, 이동은 링크(`a`)로 만든다. E2E 테스트가 역할과 라벨로 요소를 찾는다.

## 어디서 무엇을 읽을지

작업에 필요한 문서를 먼저 확인한다.

| 상황 | 참고 파일 |
|---|---|
| 현재 구조와 구성요소의 역할 확인 | `docs/project-structure.md` |
| 개발·환경설정·검증·협업 기준 확인 | `docs/conventions.md` |
| 브랜치·커밋·PR 작업 | `.claude/rules/git-workflow.md` |
| Controller 개발·수정 | `.claude/rules/controller.md` |
| E2E 테스트(`frontend/e2e/`) 작성·수정 | `.claude/rules/e2e.md` |

Git Rule은 항상 적용하고, Controller Rule은 해당 파일의 `paths` 조건에 따라 적용한다.
문서와 코드가 다르면 차이를 알리고 현재 상태와 변경 목표를 구분한다.

## 주요 명령어

백엔드 명령어는 `backend/` 디렉터리의 Windows PowerShell에서 Gradle Wrapper로 실행한다.

```powershell
cd backend
.\gradlew.bat build     # 빌드 및 테스트
.\gradlew.bat test      # 테스트만 실행
.\gradlew.bat bootRun   # 애플리케이션 실행
```

- `build`에 테스트가 포함되므로, 같은 변경에 대해 이유 없이 `test`를 반복하지 않는다.
- Spring 컨텍스트를 불러오는 테스트와 로컬 실행에는 DB 연결 및 환경설정이 필요할 수 있다.
- macOS·Linux에서는 `.\gradlew.bat` 대신 `./gradlew`를 사용한다.

프론트엔드 명령어는 `frontend/` 디렉터리에서 실행한다.

```powershell
cd frontend
npm run dev     # 개발 서버 (3000번 포트)
npm run build   # 타입 검사와 빌드
npm run lint    # 린트
npm run test:e2e  # Playwright E2E 테스트 (백엔드 실행 필요)
```

- API를 호출하는 화면을 확인하려면 백엔드(`bootRun`)를 먼저 실행한다.
- E2E는 백엔드(8080)를 먼저 실행한 뒤 돌린다. 프론트 서버는 Playwright가 띄운다. 작성 규칙은 `.claude/rules/e2e.md`에 있다.

## 로컬 실행 환경

- 기본 프로필은 `local`이며 `SPRING_PROFILES_ACTIVE`로 변경할 수 있다.
- `application.yml`에서 실행 위치의 `.env`를 선택적으로 불러오므로, `backend/`에서 실행하고 `.env`는 `backend/` 안에 둔다.
- `backend/.env.example`에는 필요한 환경변수 이름만 적고 값은 비워 둔다.
- 로컬 프로필은 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`를 참조한다.
- `DB_URL`은 기본값이 없고, 사용자명·비밀번호의 기본값은 각각 `root`와 빈 문자열이다.
- 인증에는 `JWT_SECRET`(32자 이상)이 필요하다. `backend/.env`에 두며, 없으면 서버가 시작되지 않는다.
- 프론트는 백엔드 주소를 `VITE_API_BASE_URL`로 바꿀 수 있고, 기본값은 `http://localhost:8080`이다. (`frontend/.env.example`)
- 실제 접속 환경에 맞는 값은 사용자가 로컬에서 관리한다.
- 로컬의 `ddl-auto: update` 설정을 운영 환경에 그대로 적용하지 않는다.

## 작업 방식

- 요청한 목표와 완료 조건을 확인하고 필요한 파일부터 읽는다.
- 여러 파일이나 구조를 바꾸는 작업은 변경 범위와 검증 방법을 먼저 설명한다.
- 사용자 승인이 필요한 PowerShell·Bash 명령은 실행 전에 목적, 실행 대상, 파일·DB 변경 여부, 주의사항을 한국어로 간결하게 설명한다. 단순 조회나 컴파일처럼 반복되는 저위험 작업은 한 문장으로 충분하다.
- 결과에 영향을 주는 요구사항이 모호하면 질문한다.
- 기존 패턴을 따르고, 작업과 관계없는 리팩터링이나 의존성 변경은 피한다.
- 현재 Book API에는 Service가 없다. 새로운 업무 로직의 복잡도에 맞게 분리를 검토한다.
- 변경 후에는 적절한 검증을 수행하고 변경 내용·이유·검증 결과를 설명한다.
- 빌드 성공, 서버 시작, DB 연결, Swagger 접속, 개별 API 동작을 구분한다.
- 확인하지 못한 항목은 미확인으로 기록하고 이유를 설명한다.
- 구조나 개발 기준이 바뀌면 관련 docs와 Rules도 함께 검토한다.

## 영구 금지

- `.env`와 실제 비밀번호·토큰을 커밋하거나 문서·응답·로그에 노출하지 않는다.
- 문제 해결을 위해 `.env` 전체를 출력하거나 사용자에게 공유하도록 요구하지 않는다.
- `main`과 `develop`에 직접 push하지 않는다.
- 강제 push를 사용하지 않는다.
- 사용자의 기존 변경사항을 임의로 삭제하거나 되돌리지 않는다.
- 요청받지 않은 커밋·push·PR 생성·머지를 수행하지 않는다.
- 실행하지 않은 검증을 통과했다고 보고하지 않는다.