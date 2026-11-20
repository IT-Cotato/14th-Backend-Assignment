# CLAUDE.md

## 프로젝트 개요
- COTATO 14기 백엔드 과제 레포. **코테이토 스터디 매칭 플랫폼**을 백엔드와 프론트엔드 모노레포로 만든다.
- 도메인: 회원(기수, 파트) / 스터디 모집글 / 댓글. 별도의 가입 신청 기능은 없고, **모집글에 댓글을 남기는 것이 곧 스터디 신청**이다.
- 권한: 조회는 누구나, 작성은 로그인한 사용자, 수정과 삭제는 작성자 본인만.

### 모노레포 구조
```
backend/    Spring Boot 3.5 / Java 17 / Gradle (여기서 ./gradlew 를 실행한다)
frontend/   React / TypeScript / Vite / Tailwind CSS (Next.js 는 쓰지 않는다)
  └── e2e/  Playwright E2E 테스트
docs/       architecture, conventions, api-spec, erd, features
```

### Backend
- 주요 의존성: Spring Web MVC, Spring Data JPA + MySQL, Spring Security + JWT(jjwt), Validation, springdoc-openapi, Lombok
- 베이스 패키지: `com.cotato.backend14th` (`global/` 공통, `domain/<도메인>/` 기능별)
- 도메인: `auth`(회원가입, 로그인, 로그아웃), `user`, `studypost`, `comment`
- 인증: `Authorization: Bearer <accessToken>`. 로그아웃한 토큰은 `logout_token` 테이블에 저장해 만료 시각까지 막는다.
- 환경 변수는 `backend/.env`에서 읽는다 (`application.yml`의 `spring.config.import`). 필요한 이름은 `backend/.env.example` 참고 (`DB_*`, `JWT_SECRET`, `JWT_ACCESS_EXPIRATION_MS`)

### Frontend
- React Router, axios, TanStack Query. 개발 서버는 3000번 포트 고정 (`strictPort`). 백엔드 CORS가 3000번을 허용한다.
- API 주소는 `VITE_API_BASE_URL` (기본 `http://localhost:8080`)

## 어디서 무엇을 읽을지
- 패키지 구조, 요청 -> 응답/예외 흐름, 인증 흐름이 궁금할 때 -> `docs/architecture.md`
- API 13개의 요청/응답/에러 -> `docs/api-spec.md`
- 테이블과 컬럼 -> `docs/erd/README.md`
- 코드 스타일, 커밋/브랜치/PR 규칙 -> `docs/conventions.md`
- controller, dto 작업 -> `.claude/rules/api-layer.md` (자동 적용)
- exception, entity 작업 -> `.claude/rules/exception-entity.md` (자동 적용)
- 프론트엔드 작업 -> `.claude/rules/frontend.md` (자동 적용)
- E2E 테스트 작업 -> `.claude/rules/e2e.md` (자동 적용)
- git 작업 전 -> `.claude/rules/git-workflow.md`
- PR 단위 작업을 문서로 정리할 때 -> `feature-doc` 스킬 (형식은 `docs/features/_template.md`)
- 변경사항을 객관적으로 검토받고 싶을 때 -> `reviewer` 서브에이전트
- AI가 과거에 틀렸던 것 -> `.dev/learnings/`

## 주요 명령어
```bash
# backend/ 에서 실행 (.env 를 실행 위치 기준으로 읽는다)
cd backend
./gradlew build      # 컴파일 + 테스트 (contextLoads는 DB 연결과 JWT_SECRET이 필요)
./gradlew test       # 테스트만
./gradlew test --tests '*ServiceTest'   # DB 없이 서비스 단위 테스트만
./gradlew bootRun    # 로컬 실행 (profile: local, 8080 포트)

# frontend/ 에서 실행
cd frontend
npm install
npm run dev          # 개발 서버 (http://localhost:3000)
npm run build        # 타입 검사 + 빌드
npm run lint
npx playwright test  # E2E. 백엔드와 DB를 먼저 켠다. 프론트 서버는 Playwright가 띄운다
```
- Swagger UI: http://localhost:8080/swagger-ui/index.html

## 작업 방식
- 작업 전에 이해한 내용을 먼저 요약하고, 모호한 점은 질문으로 해소한 뒤 진행한다.
- 큰 작업은 계획 -> 승인 -> 실행 -> 검증 순서로 한다. 시작 전에 측정 가능한 완료 조건을 적는다.
- 새 기능은 기존 패턴을 재사용한다: 응답은 `ResponseUtils`, 예외는 `Backend14thException(ErrorCode.X)`, 엔티티는 `BaseEntity` 상속, 페이지 응답은 `PageResponse`.
- API를 바꾸면 `docs/api-spec.md`를, 테이블을 바꾸면 `docs/erd/`를 같이 고친다.
- 요청 범위 밖의 파일은 수정하지 않는다. 필요해 보이면 먼저 제안한다.
- 완료라고 말하기 전에 백엔드는 `./gradlew build`, 프론트는 `npm run build`와 `npm run lint`로 확인한다. 실행하지 못했다면 그렇다고 말한다.
- E2E가 실패하면 원인이 앱인지 테스트인지 먼저 가린다. 통과시키려고 테스트를 지우거나 검사를 약하게 바꾸지 않는다.
- 훅을 수정하면 차단돼야 할 명령과 통과돼야 할 명령을 모두 테스트하고, 기대 exit code와 비교한 결과를 보고한다.
- 같은 실수를 반복하면 `.dev/learnings/`에 기록하고 rule로 승격을 제안한다.
- 작업 메모와 handoff는 `.dev/scratchpad/`에 둔다 (Git에 올리지 않음).

## 영구 금지
- `.env` 파일을 읽거나, 수정하거나, 출력하지 않는다. (settings.json deny + `.claude/hooks/block-env-access.sh` 훅으로 차단)
- `git push --force`, `git reset --hard`, `git clean -f`, `git branch -D` 등 되돌리기 어려운 git 명령을 쓰지 않는다. (훅으로 차단)
- `develop`, `seungmin/basic`에 직접 커밋하지 않는다. 작업 브랜치에서만 커밋한다.
- 비밀번호, 토큰, JWT 시크릿 같은 민감 정보를 코드나 yml에 하드코딩하지 않는다. 비밀번호는 BCrypt로 암호화해서만 저장한다.
