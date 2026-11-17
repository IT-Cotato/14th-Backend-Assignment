# 프로젝트 구조

## 프로젝트 개요

코테이토 스터디 매칭 플랫폼의 모노레포다. 백엔드는 `backend/`, 프론트엔드는 `frontend/`에 둔다.

Java 21과 Spring Boot 기반의 백엔드 프로젝트다.
Gradle, Spring Data JPA, MySQL을 사용하며 SpringDoc으로 API 문서를 제공한다.

기본 패키지는 `com.cotato.backend14th`다.
기능별 코드는 `domain`, 여러 기능이 공유하는 코드는 `global`에 배치한다.
구현 범위는 아래 구성요소 표와 실제 코드(`backend/src/main/java`)로 확인한다.

프론트엔드는 React, TypeScript, Vite, Tailwind CSS 기반이며 `frontend/`에 둔다.

## 주요 디렉터리

```text
14th-Backend-Assignment/
├── CLAUDE.md                       # AI 작업 지침과 문서 안내
├── .claude/
│   ├── rules/                      # 공통·조건부 규칙
│   ├── skills/                     # 반복 작업 절차
│   ├── hooks/                      # 실행 전 안전장치
│   └── settings.json               # Hook 연결 설정
├── .github/                        # 이슈·PR 템플릿
├── docs/                           # 구조·개발·협업 문서
├── backend/                        # Spring Boot 백엔드 (Gradle 프로젝트 루트)
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/cotato/backend14th/
│   │   │   │   ├── domain/             # 도메인별 controller, dto, entity, repository, service
│   │   │   │   │   ├── auth/           # 회원가입·로그인·로그아웃, 로그아웃한 토큰
│   │   │   │   │   ├── book/
│   │   │   │   │   ├── comment/        # 댓글(스터디 신청)
│   │   │   │   │   ├── study/          # 스터디 모집글
│   │   │   │   │   └── user/           # 사용자, 내 정보
│   │   │   │   └── global/
│   │   │   │       ├── common/entity/
│   │   │   │       ├── common/response/
│   │   │   │       ├── config/
│   │   │   │       ├── exception/
│   │   │   │       └── security/       # JWT 발급·검증, 인증 필터
│   │   │   └── resources/          # 애플리케이션·프로필 설정
│   │   └── test/                   # 테스트 코드
│   ├── build.gradle                # 의존성·Java 버전·빌드 설정
│   ├── settings.gradle
│   ├── gradlew, gradlew.bat, gradle/  # Gradle Wrapper
│   └── .env.example                # 필요한 환경변수 이름 (값은 비움)
├── frontend/                       # React 프론트엔드 (Vite, 개발 서버 3000번 포트)
│   ├── src/
│   │   ├── api/                    # axios 클라이언트, API 호출 함수, 응답 타입
│   │   ├── auth/                   # 로그인 상태, 보호 라우트, 토큰 저장
│   │   ├── components/             # 헤더, 레이아웃, 폼 입력, 댓글 영역 등 공통 컴포넌트
│   │   ├── pages/                  # 화면 (로그인, 회원가입, 마이페이지, 스터디 목록·상세·작성/수정)
│   │   └── utils/                  # 메시지 처리 등 보조 함수
│   ├── package.json                # 의존성·스크립트 (dev, build, lint)
│   ├── vite.config.ts              # 3000번 포트 고정, Tailwind 플러그인
│   └── .env.example                # 선택 환경변수 이름 (VITE_API_BASE_URL)
├── .gitattributes                  # 줄바꿈 등 Git 속성
└── .gitignore                      # Git 추적 제외 규칙
```

주요 디렉터리와 설정만 표시했다.
`backend/.env`는 로컬 환경설정 파일이며 Git 추적 대상에서 제외한다.
`backend/build/`와 `backend/.gradle/`은 각각 빌드 산출물과 캐시를 담는다.
`frontend/node_modules/`와 `frontend/dist/`는 설치된 패키지와 빌드 산출물이며 Git 추적 대상에서 제외한다.

## 주요 구성요소

### 시작점과 기본 API

- `Backend14thApplication`: 애플리케이션을 실행하고 `@EnableJpaAuditing`으로 생성·수정 시각 기록을 활성화한다.
- `HelloController`: `GET /hello`에 문자열을 직접 반환한다.

### Book 도메인

| 클래스 | 역할 |
|---|---|
| BookController | 책 등록·단건 조회 요청 처리와 응답 구성 |
| BookRequest | 등록 요청의 제목과 저자 표현 |
| BookResponse | 응답할 책의 ID, 제목, 저자 표현 |
| Book | DB에 저장되는 책 데이터. BaseEntity 상속 |
| BookRepository | `JpaRepository<Book, Long>`을 통한 조회·저장 |

### 인증·사용자 도메인

| 클래스 | 역할 |
|---|---|
| AuthController | 회원가입·로그인·로그아웃 요청 처리 (`/api/auth`) |
| AuthService | 중복 이메일 확인, 비밀번호 암호화, 로그인 검증과 토큰 발급, 로그아웃 시 토큰 무효 처리 |
| RevokedToken, RevokedTokenRepository | 로그아웃한 토큰의 `jti`와 만료 시각 저장·조회 (`revoked_token`) |
| User, Part, UserRepository | 사용자 데이터와 파트(`PM`, `DE`, `BE`, `FE`) |
| UserController, UserService | 내 정보 조회 (`/api/users/me`) |

### 스터디 모집글 도메인

| 클래스 | 역할 |
|---|---|
| StudyPostController | 모집글 작성·목록·상세·수정·삭제 요청 처리 (`/api/study-posts`) |
| StudyPostService | 작성자 확인, 부분 수정, 키워드 이스케이프와 페이징 값 검증, 목록의 댓글 수 집계, 삭제 시 댓글 벌크 삭제 |
| StudyPost, StudyStatus | 모집글 데이터와 모집 상태(`RECRUITING`, `CLOSED`). 작성자는 LAZY 연관 |
| StudyPostRepository | `search`의 JPQL로 `keyword`·`status`·`part` 조건을 한 번에 처리하고(값이 없는 조건은 무시), 작성자를 fetch join으로 함께 조회 |
| AuthorResponse | 모집글·댓글에 담는 작성자 정보(닉네임, 기수, 파트) |

### 댓글(스터디 신청) 도메인

댓글을 남기는 것이 곧 스터디 신청이다.

| 클래스 | 역할 |
|---|---|
| CommentController | 댓글 작성·목록(`/api/study-posts/{postId}/comments`), 수정·삭제(`/api/comments/{commentId}`) 요청 처리 |
| CommentService | 마감된 글의 작성 제한(`STUDY_POST_CLOSED`), 작성자 확인, 페이징 값 검증 |
| StudyPostComment | 댓글 데이터. 모집글과 작성자를 LAZY로 연관 (`study_post_comment`) |
| StudyPostCommentRepository | 댓글 목록(작성자 fetch join), 글 여러 개의 댓글 수를 한 번에 집계, 글 삭제 시 댓글 벌크 삭제 |
| CommentResponse | 댓글 하나의 응답. 본인 댓글 여부를 `isAuthor`로 내려줌 |

- 모집글 목록의 `commentCount`는 현재 페이지 글들의 댓글 수를 `group by` 쿼리 한 번으로 세어 붙인다. 글마다 따로 세지 않아 N+1이 생기지 않는다.
- 모집글을 삭제할 때는 댓글을 벌크 삭제 쿼리로 먼저 지운 뒤 글을 삭제한다. 댓글이 글을 참조(FK)하므로 이 순서를 바꾸면 삭제가 실패한다.

### 공통 영역

| 주요 클래스 | 역할 |
|---|---|
| BaseEntity | 생성·수정 시각 관리 |
| ApiResponse, ResponseUtils | 공통 성공 응답 구성 |
| ErrorResponse | 오류 응답 데이터 표현 |
| PageResponse | 목록 응답 형식 (`content`, `page`, `size`, `totalElements`, `totalPages`) |
| SwaggerConfig | OpenAPI 문서의 기본 정보와 Bearer 인증 설정 |
| CorsConfig | CORS 정책 정의 |
| SecurityConfig | 경로별 접근 규칙(공개·로그인), JWT 필터 연결, BCrypt 인코더, 세션 정책 설정 |
| JwtProvider | JWT 발급과 검증 (토큰마다 고유한 `jti` 포함) |
| JwtAuthenticationFilter | 요청마다 토큰을 확인하고 로그아웃한 토큰이면 인증하지 않음 |
| JwtAuthenticationEntryPoint | 인증이 필요한 경로의 실패를 401 `ErrorResponse`로 응답 |
| ErrorCode, Backend14thException | 오류 코드와 사용자 정의 예외 정의 |
| GlobalExceptionHandler | 예외를 HTTP 오류 응답으로 변환 (검증 실패와 형식이 맞지 않는 요청 값은 400) |

## Book API 처리 흐름

현재는 Service 계층 없이 Controller가 Repository를 직접 호출한다.

```text
HTTP 요청 → BookController → BookRepository ↔ MySQL
                 ↓
          BookResponse 구성
                 ↓
       ResponseUtils로 응답 반환
```

- `POST /books`: 요청 데이터를 저장하고 공통 성공 응답과 HTTP 201을 반환한다.
- `GET /books/{id}`: 책을 조회하고 공통 성공 응답과 HTTP 200을 반환한다.
- 조회 대상이 없으면 `ErrorCode.BOOK_NOT_FOUND`를 사용하는 `Backend14thException`을 던진다. 전역 예외 처리기가 `ErrorResponse`와 HTTP 404를 반환한다.

Entity와 요청·응답 DTO는 분리되어 있다.
책 목록 조회·수정·삭제 API는 아직 없다.

## 인증 처리 흐름

```text
로그인  POST /api/auth/login → AuthController → AuthService
        (UserRepository 조회, 비밀번호 검증) → JwtProvider가 토큰 발급

요청    Authorization: Bearer 토큰 → JwtAuthenticationFilter(서명·만료 확인, revoked_token 조회)
        → SecurityConfig 접근 규칙 → Controller (@AuthenticationPrincipal로 사용자 ID 사용)

실패    인증이 필요한 경로에 유효한 토큰이 없으면 JwtAuthenticationEntryPoint가 401(COMMON-004) 응답
```

`/api` 아래에서 회원가입·로그인과 모집글 조회(`GET /api/study-posts/**`)는 공개이고, 그 외는 로그인이 필요하다.
`/api`가 아닌 기존 경로(`/hello`, `/books`, Swagger)는 그대로 공개다.
공개 API에서도 유효한 토큰이 있으면 로그인한 사용자로 인식한다.

## 실행 설정과 테스트

| 항목 | 현재 상태 |
|---|---|
| application.yml | 애플리케이션 이름, 기본 프로필, `.env` 불러오기, JWT 설정(`JWT_SECRET`, 유효 시간) |
| application-local.yml | 로컬 MySQL 연결과 JPA 설정 |
| backend/src/test/ | `contextLoads()`와 서비스·JWT 단위 테스트 (Spring·DB 없이 실행) |
| frontend/ | `npm run build`, `npm run lint`로 검증. E2E 테스트는 아직 없음 |

## 문서 관리

구조나 주요 구성요소의 역할이 바뀌면 이 문서를 갱신한다.
개발·협업 기준은 `docs/conventions.md`에서 관리한다.