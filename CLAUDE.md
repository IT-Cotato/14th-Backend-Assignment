# CLAUDE.md

코테이토 스터디 매칭 플랫폼이다. 회원은 이메일로 로그인하고, 모집글에 댓글을 남겨 스터디에 신청한다.
백엔드는 Java 17, Spring Boot 3.5, MySQL과 Gradle Wrapper를 사용한다. 기본 패키지는 `com.cotato.backend14th`다.
프론트는 React, TypeScript, Vite, Tailwind CSS를 사용한다.

- `backend/`는 Spring 애플리케이션과 JUnit 5 테스트를 담는다.
- `frontend/`는 React 앱과 Playwright 시나리오를 담는다.
- API 변경 전 [API 명세](docs/api-spec.md)와 [백엔드 컨벤션](docs/backend-conventions.md)을 읽는다.
- 실행과 DB 설정은 [로컬 개발 환경 점검](docs/local-verification.md)을 참고하거나 `/verify-local`을 사용한다.

| 위치 | 작업 | 명령 |
| --- | --- | --- |
| `backend/` | 빌드 및 테스트 | `./gradlew build` 또는 `.\gradlew.bat build` |
| `backend/` | 실행 | `./gradlew bootRun` 또는 `.\gradlew.bat bootRun` |
| `frontend/` | 의존성 설치 | `npm ci` |
| `frontend/` | 실행과 빌드 | `npm run dev`, `npm run build` |
| `frontend/` | E2E | 백엔드 실행 후 `npx playwright test` |

- 새 API는 `/api` 아래에 두고 기존 공통 응답을 사용한다. 204는 본문을 비운다.
- 공개 조회, 로그인 사용자 작성, 작성자 수정·삭제 권한을 서버에서 확인한다.
- 실제 `backend/.env`, DB 자격 증명, JWT 시크릿과 토큰을 커밋하거나 출력하지 않는다.
- 공유 DB와 운영 DB에서 로컬 테스트를 실행하지 않는다.
- 공용 `develop` 직접 push와 강제 push를 금지한다.
