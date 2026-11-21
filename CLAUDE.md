# 14th-Backend-Assignment: 코테이토 스터디 매칭 플랫폼

코테이토 14기 백엔드 파트 과제 프로젝트예요. 5~6주차에는 네이버 카페에서 하던 스터디 신청을 대신할 **스터디 매칭 플랫폼**을 백엔드와 프론트엔드로 함께 만들어요.

## 도메인
- **User**: 이메일·비밀번호로 가입하고, 기수(`generation`, 1 이상 정수)와 파트(`part`: PM, DE, BE, FE)를 입력해요.
- **StudyPost**: 스터디 모집글. 상태(`studyStatus`)는 `RECRUITING` 또는 `CLOSED`예요.
- **StudyPostComment**: 모집글에 단 댓글. **댓글이 곧 스터디 신청**이에요. 별도 가입 신청 기능은 없어요.
- 권한: 조회는 누구나, 작성은 로그인 사용자, 수정·삭제는 작성자 본인만 해요.
- 자세한 API는 `docs/api-spec.md`, 테이블은 `docs/erd/`를 봐요.

## 모노레포 구조
```
├── backend/     # Spring Boot (Java 17, Spring Boot 4.1.1, Gradle)
├── frontend/    # React + TypeScript + Vite + Tailwind CSS (Next.js 사용 안 함)
├── docs/        # api-spec.md, erd/, features/
└── .claude/     # 하네스 (rules, skills, hooks)
```

## 주요 명령어
### 백엔드 (`backend/`에서 실행)
- 빌드: `./gradlew build` (Windows PowerShell에서는 `.\gradlew build`)
- 테스트: `./gradlew test`
- 실행: `./gradlew bootRun` → http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- `.env`는 `backend/.env`에 있어요. 필요한 변수 이름은 `backend/.env.example`을 봐요.

### 프론트엔드 (`frontend/`에서 실행)
- 개발 서버: `npm run dev` → http://localhost:3000 (포트 3000 고정, CORS가 3000을 허용해요)
- 빌드: `npm run build`
- 린트: `npm run lint`
- E2E: `npx playwright test` (백엔드 서버와 DB를 먼저 켜요)

## 백엔드 구조 (도메인형)
```
backend/src/main/java/com/cotato/backend14th/
├── domain/{도메인}/          # controller, service, dto, entity, repository
└── global/
    ├── common/entity/        # BaseEntity
    ├── common/response/      # ApiResponse, ErrorResponse, ResponseUtils
    ├── config/               # Swagger, CORS, Security 설정
    └── exception/            # ErrorCode, Backend14thException, GlobalExceptionHandler
```

## 핵심 컨벤션
- 모든 API 주소는 `/api`로 시작해요.
- 성공 응답은 `ResponseUtils.ok()` / `ResponseUtils.created()`로 감싸요. 본문 없는 204는 감싸지 않아요.
- 예외는 `ErrorCode`에 상수를 추가하고 `Backend14thException`으로 던져요. 코드 형식은 `도메인-001` (예: `STUDY-001`).
- 엔티티는 `BaseEntity`를 상속하고, 연관관계는 모두 `LAZY`, Enum은 `@Enumerated(EnumType.STRING)`으로 저장해요.
- 비밀번호는 BCrypt로 암호화해서 저장하고, 응답에 절대 넣지 않아요.
- 커밋 메시지는 `타입: 요약` 형식이에요. (feat, fix, docs, style, refactor, chore, test) 프로덕션 코드와 테스트 코드는 커밋을 나눠요.

## 상황별로 읽을 문서
- API를 만들거나 고칠 때 → `docs/api-spec.md`
- 브랜치, 커밋, 이슈, PR 작업 → `docs/git-convention.md`
- 패키지 위치나 응답·예외 구조가 헷갈릴 때 → `docs/project-structure.md`
- 새 도메인을 만들 때 → `new-domain` 스킬 사용

## 절대 하지 않는 것
- `main`, `develop` 브랜치에 직접 push하지 않아요. 작업은 항상 작업 브랜치에서 해요.
- `git push --force`를 쓰지 않아요.
- `.env` 파일을 읽거나, 출력하거나, 커밋하지 않아요. DB 비밀번호, JWT 시크릿 같은 민감 정보는 `.env`에만 둬요.
- `application*.yml`에 비밀번호나 시크릿을 직접 적지 않아요. `${DB_PASSWORD}`처럼 참조만 해요.
- 요청받지 않은 설정 파일(`global/config/`, `build.gradle`, `package.json`)은 고치지 않아요. 고쳐야 하면 먼저 물어봐요.
- 테스트를 통과시키려고 테스트를 지우거나 검사를 약하게 바꾸지 않아요.

## 작업 방식
- 큰 작업은 바로 코드를 쓰지 말고, 계획을 먼저 보여주고 승인을 받아요.
- 백엔드를 고치면 `./gradlew build`, 프론트를 고치면 `npm run build`가 성공하는지 확인해요.
