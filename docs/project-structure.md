# 프로젝트 구조

## 프로젝트 개요

Java 21과 Spring Boot 기반의 백엔드 프로젝트다.
Gradle, Spring Data JPA, MySQL을 사용하며 SpringDoc으로 API 문서를 제공한다.

기본 패키지는 `com.cotato.backend14th`다.
기능별 코드는 `domain`, 여러 기능이 공유하는 코드는 `global`에 배치한다.
현재 책 등록·단건 조회 API와 `GET /hello`가 구현되어 있다.

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
├── src/
│   ├── main/
│   │   ├── java/com/cotato/backend14th/
│   │   │   ├── domain/book/
│   │   │   │   ├── controller/
│   │   │   │   ├── dto/
│   │   │   │   ├── entity/
│   │   │   │   └── repository/
│   │   │   └── global/
│   │   │       ├── common/entity/
│   │   │       ├── common/response/
│   │   │       ├── config/
│   │   │       └── exception/
│   │   └── resources/              # 애플리케이션·프로필 설정
│   └── test/                       # 테스트 코드
├── build.gradle                    # 의존성·Java 버전·빌드 설정
├── .gitattributes                  # 줄바꿈 등 Git 속성
└── .gitignore                      # Git 추적 제외 규칙
```

주요 디렉터리와 설정만 표시했다.
`.env`는 로컬 환경설정 파일이며 Git 추적 대상에서 제외한다.
`build/`와 `.gradle/`은 각각 빌드 산출물과 캐시를 담는다.

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

### 공통 영역

| 주요 클래스 | 역할 |
|---|---|
| BaseEntity | 생성·수정 시각 관리 |
| ApiResponse, ResponseUtils | 공통 성공 응답 구성 |
| ErrorResponse | 오류 응답 데이터 표현 |
| SwaggerConfig | OpenAPI 문서의 기본 정보 설정 |
| CorsConfig | CORS 정책 정의 |
| SecurityConfig | 요청 접근과 인증·세션 정책 설정 |
| ErrorCode, Backend14thException | 오류 코드와 사용자 정의 예외 정의 |
| GlobalExceptionHandler | 예외를 HTTP 오류 응답으로 변환 |

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

## 실행 설정과 테스트

| 항목 | 현재 상태 |
|---|---|
| application.yml | 애플리케이션 이름, 기본 프로필, `.env` 불러오기 설정 |
| application-local.yml | 로컬 MySQL 연결과 JPA 설정 |
| SecurityConfig | 모든 요청을 허용하는 초기 설정 |
| src/test/ | Spring 컨텍스트를 불러오는 기본 `contextLoads()` 테스트 |

## 문서 관리

구조나 주요 구성요소의 역할이 바뀌면 이 문서를 갱신한다.
개발·협업 기준은 `docs/conventions.md`에서 관리한다.