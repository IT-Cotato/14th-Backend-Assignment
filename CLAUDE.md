# CLAUDE.md

## 프로젝트 개요
- Cotato 14기 백엔드+프론트엔드 과제 레포 (`com.cotato.backend14th`). 명세는 `docs/PRD.md` (5~6주차 과제: 스터디 매칭 플랫폼).
- 모노레포 구조: `backend/` (Spring Boot), `frontend/` (React), `docs/` (ERD/API 명세/기능 문서), `.claude/` (하네스)
- 백엔드: Spring Boot 4.1.1 / Java 21 / Gradle. 주요 의존성: Spring Web MVC, Spring Data JPA, MySQL, Spring Security, springdoc-openapi(Swagger), Validation, Lombok
- 프론트엔드: React + TypeScript + Vite + Tailwind CSS + React Router + axios. **Next.js 사용 금지** (Vite 기반 SPA 유지). 개발 서버 포트는 **3000 고정** (`vite.config.ts`의 `server.port`/`strictPort`)
- 도메인: `book`(3주차 샘플 CRUD, `/books`, 유지만 함) / `user`(인증: 회원가입·로그인·로그아웃·내 정보, JWT) / `studypost`(스터디 모집글: 작성·목록(키워드/상태/파트 필터)·상세·수정·삭제) / `comment`(댓글=스터디 신청: 작성·목록·수정·삭제). 각 도메인은 `controller/dto/entity/repository/service`로 구성. API 명세는 `docs/api-spec.md`, 구현 결정사항은 `docs/features/129-인증-모집글-댓글.md` 참고
- 공통 응답 포맷(`ApiResponse`/`ErrorResponse`), 전역 예외 처리(`GlobalExceptionHandler`), CORS/Security 설정이 `backend/.../global/` 패키지에 적용되어 있음
- DB 접속 정보 등 민감 값은 `backend/.env`를 통해 주입(`application.yml`에서 `optional:file:.env[.properties]` import). 키 이름만 적힌 예시는 `backend/.env.example` 참고

## 주요 명령어
- 백엔드 빌드: `cd backend && ./gradlew build`
- 백엔드 테스트: `cd backend && ./gradlew test`
- 백엔드 로컬 실행: `cd backend && ./gradlew bootRun`
- Swagger 접속: `http://localhost:8080/swagger-ui/index.html`
- 프론트엔드 설치: `cd frontend && npm install`
- 프론트엔드 개발 서버: `cd frontend && npm run dev` (포트 3000 고정)
- 프론트엔드 빌드: `cd frontend && npm run build`
- E2E 테스트(백엔드 서버를 먼저 켠 뒤): `cd frontend && npx playwright test`

## 영구 금지 규칙
- `backend/.env`, `application-local.yml` 등 민감 정보가 담긴 파일은 절대 커밋/푸시하지 않는다.
- `git push --force`, `git reset --hard` 등 파괴적 git 명령은 사용자 승인 없이 실행하지 않는다.
- 공통 응답 포맷(`ApiResponse`/`ErrorResponse`)과 전역 예외 처리 구조를 임의로 변경하지 않는다.
- DB 스키마나 마이그레이션을 임의로 변경하지 않는다.
- 사용자가 명시적으로 요청하지 않은 리팩토링/구조 변경은 하지 않는다.
- 프론트엔드는 Next.js를 쓰지 않는다 (Vite 기반 React SPA 유지). 개발 서버 포트는 3000으로 고정한다.
