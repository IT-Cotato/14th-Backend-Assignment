# 프로젝트 구조

## 프로젝트 개요

Java 21과 Spring Boot 기반의 백엔드 프로젝트다.
Gradle로 빌드하며, Spring Data JPA와 MySQL을 사용한다.
SpringDoc을 통해 API 문서와 Swagger UI를 제공한다.

기본 패키지는 `com.cotato.backend14th`다.
기능별 코드는 `domain`, 공통 코드는 `global`에 배치한다.
현재 책 등록·단건 조회 API와 인사 응답 API가 구현되어 있다.

## 주요 디렉터리 구조

```text
14th-Backend-Assignment/
├── CLAUDE.md                       # AI 작업 지침과 문서 안내
├── .claude/                        # AI 작업 설정
│   ├── rules/
│   │   ├── git-workflow.md         # 항상 적용하는 Git 협업 규칙
│   │   └── controller.md           # Controller 경로에 적용하는 규칙
│   ├── skills/
│   │   └── pr-check/
│   │       └── SKILL.md            # PR 제출 전 점검 절차
│   ├── hooks/
│   │   └── block-force-push.sh     # 강제 push 패턴 차단
│   └── settings.json               # Hook 연결 설정
├── .github/                        # 이슈·PR 템플릿
├── docs/
│   ├── project-structure.md        # 현재 구조와 구성요소의 역할
│   └── conventions.md              # 개발·환경설정·검증·협업 기준
├── gradle/
│   └── wrapper/                    # Gradle Wrapper
├── src/
│   ├── main/
│   │   ├── java/com/cotato/backend14th/
│   │   │   ├── Backend14thApplication.java
│   │   │   ├── HelloController.java
│   │   │   ├── domain/book/
│   │   │   │   ├── controller/     # HTTP 요청 처리
│   │   │   │   ├── dto/            # 요청·응답 데이터
│   │   │   │   ├── entity/         # 영속 데이터 모델
│   │   │   │   └── repository/     # DB 접근
│   │   │   └── global/
│   │   │       ├── common/
│   │   │       │   ├── entity/     # 공통 Entity 속성
│   │   │       │   └── response/   # 공통 응답
│   │   │       ├── config/         # 공통 설정
│   │   │       └── exception/      # 예외 정의·처리
│   │   └── resources/
│   │       ├── application.yml
│   │       └── application-local.yml
│   └── test/                       # 테스트 코드
├── build.gradle                    # 의존성·빌드 설정
├── settings.gradle                 # Gradle 프로젝트 설정
├── gradlew
├── gradlew.bat
├── .gitattributes                  # 파일 줄바꿈 등 Git 속성
└── .gitignore                      # Git 추적 제외 규칙
```

위 트리는 주요 소스와 설정을 중심으로 정리한 것이다.
`.env`는 로컬 환경설정 파일이며 Git 추적 대상에서 제외한다.
`build/`와 `.gradle/`은 각각 빌드 산출물과 캐시를 담는다.

## 주요 구성요소와 역할

### 애플리케이션 시작점

`Backend14thApplication`에서 Spring Boot 애플리케이션을 실행한다.
`@EnableJpaAuditing`으로 Entity의 생성·수정 시각 기록을 활성화한다.

`HelloController`는 `GET /hello` 요청에 인사 문자열을 직접 반환한다.

### Book 도메인

| 클래스 | 역할 |
|---|---|
| BookController | 책 등록·단건 조회 요청을 처리하고 응답을 구성한다. |
| BookRequest | 책 등록 요청의 제목과 저자를 담는다. |
| BookResponse | 응답할 책의 ID, 제목, 저자를 담는다. |
| Book | DB에 저장되는 책 데이터를 표현한다. |
| BookRepository | `JpaRepository<Book, Long>`을 통해 책을 조회·저장한다. |

### 공통 영역

| 패키지 | 주요 클래스 | 역할 |
|---|---|---|
| global/common/entity | BaseEntity | 생성 시각과 수정 시각을 관리한다. |
| global/common/response | ApiResponse, ResponseUtils | 공통 성공 응답을 구성한다. |
| global/common/response | ErrorResponse | 오류 응답 데이터를 표현한다. |
| global/config | SwaggerConfig | OpenAPI 문서의 기본 정보를 설정한다. |
| global/config | CorsConfig | CORS 정책을 정의한다. |
| global/config | SecurityConfig | 요청 접근과 인증·세션 정책을 설정한다. |
| global/exception | ErrorCode, Backend14thException | 오류 코드와 사용자 정의 예외를 정의한다. |
| global/exception | GlobalExceptionHandler | 예외를 HTTP 오류 응답으로 변환한다. |

## Book API 처리 흐름

현재 Book API는 별도의 Service 계층 없이
Controller가 Repository를 직접 호출한다.

```text
HTTP 요청
    ↓
BookController
    ↓
BookRepository ↔ MySQL
    ↓
BookController에서 BookResponse 구성
    ↓
ResponseUtils로 공통 성공 응답 반환
```

- `POST /books`: 요청 데이터를 Entity로 만들어 저장하고 201 응답을 반환한다.
- `GET /books/{id}`: ID로 책을 조회하고 200 응답을 반환한다.
- 조회 대상이 없으면 `BOOK_NOT_FOUND` 예외를 발생시키고,
  `GlobalExceptionHandler`가 `ErrorResponse`와 HTTP 404를 반환한다.

## 현재 구조의 특징

- Book의 Entity와 API 요청·응답 DTO가 분리되어 있다.
- Book은 BaseEntity를 상속해 생성·수정 시각을 기록한다.
- 공통 성공 응답은 Book API에 적용되어 있으며, `/hello`는 문자열을 직접 반환한다.
- Book 목록 조회·수정·삭제 API와 Service 계층은 아직 없다.
- Security는 현재 모든 요청을 허용하는 초기 설정이다.
- `application.yml`은 공통 설정을, `application-local.yml`은 로컬 DB·JPA 설정을 담는다.
- 테스트는 `src/test/`에 있으며, 현재 기본 `contextLoads()` 테스트가 있다.

## 문서 관리

패키지 구조나 주요 구성요소의 역할이 변경되면 이 문서도 함께 갱신한다.
개발·협업 규칙은 `docs/conventions.md`에서 관리한다.