# 프로젝트 구조

COTATO 14기 백엔드 스터디 과제 프로젝트의 실제 폴더·패키지 구조와 각 구성 요소의 책임, 현재 구현 상태를 정리한 문서예요.
이 문서는 작성 시점의 `seungju/harness` 브랜치 코드를 기준으로 해요. 코드가 바뀌면 이 문서도 함께 갱신해야 해요.

> 표기 규칙
> - **현재 구현**: 코드에 실제로 존재하는 것
> - **미구현**: 아직 코드에 없는 것 (구현된 것처럼 가정하지 않아요)

---

## 1. 기술 스택 (`build.gradle` 기준)

| 구분 | 내용 |
| --- | --- |
| 언어 / 런타임 | Java 21 (Gradle toolchain) |
| 프레임워크 | Spring Boot `4.1.1` (`io.spring.dependency-management` `1.1.7`) |
| 웹 | `spring-boot-starter-webmvc` |
| 영속성 | `spring-boot-starter-data-jpa`, `mysql-connector-j` (runtime) |
| 검증 | `spring-boot-starter-validation` (의존성만 있고 아직 사용하지 않아요) |
| 보안 | `spring-boot-starter-security` |
| API 문서 | `springdoc-openapi-starter-webmvc-ui` `2.8.5` |
| 기타 | Lombok |
| 테스트 | JUnit Platform, `spring-boot-starter-*-test` (data-jpa / validation / webmvc) |

- 빌드 도구: Gradle Wrapper (`gradlew`, `gradlew.bat`)
- `group = 'com.cotato'`, `rootProject.name = '14th-backend-assignment'`
- 린터/포매터 설정은 없어요.

---

## 2. 최상위 폴더 구조

```
14th-Backend-Assignment/
├── .github/
│   ├── ISSUE_TEMPLATE/issue_template.md
│   └── PULL_REQUEST_TEMPLATE.md
├── docs/                       # 프로젝트 문서 (이 문서 포함)
├── gradle/wrapper/             # Gradle Wrapper
├── src/
│   ├── main/
│   │   ├── java/com/cotato/backend14th/
│   │   └── resources/
│   │       ├── application.yml
│   │       └── application-local.yml
│   └── test/java/com/cotato/backend14th/
│       └── Backend14thApplicationTests.java
├── .env                        # 로컬 환경 변수 (gitignore 대상, 커밋 금지)
├── .gitignore
├── build.gradle
├── settings.gradle
├── CLAUDE.md
├── gradlew / gradlew.bat
```

---

## 3. 패키지 구조

기본 패키지는 `com.cotato.backend14th`이고, **기능별 `domain`** 과 **공통 `global`** 로 나뉘어 있어요.

```
com.cotato.backend14th
├── Backend14thApplication.java      # 애플리케이션 진입점 (@EnableJpaAuditing)
├── HelloController.java             # 1주차 실습용 컨트롤러 (domain 밖에 위치)
├── domain/
│   └── book/
│       ├── controller/BookController.java
│       ├── dto/BookRequest.java
│       ├── dto/BookResponse.java
│       ├── entity/Book.java
│       └── repository/BookRepository.java
└── global/
    ├── common/
    │   ├── entity/BaseEntity.java
    │   └── response/
    │       ├── ApiResponse.java
    │       ├── ErrorResponse.java
    │       └── ResponseUtils.java
    ├── config/
    │   ├── CorsConfig.java
    │   ├── SecurityConfig.java
    │   └── SwaggerConfig.java
    └── exception/
        ├── Backend14thException.java
        ├── ErrorCode.java
        └── GlobalExceptionHandler.java
```

현재 도메인은 `book` 하나뿐이에요.

---

## 4. 구성 요소별 책임

### 4.1 진입점 / 루트 패키지

| 파일 | 책임 |
| --- | --- |
| `Backend14thApplication` | `@SpringBootApplication` 진입점. `@EnableJpaAuditing`으로 `BaseEntity`의 생성/수정 시각 자동 기록을 켜요. |
| `HelloController` | `GET /hello` → 평문 `String` 반환. 1주차 실습에서 남은 코드로, `domain` 패키지 밖에 있고 `ApiResponse` 규칙을 따르지 않아요. |

### 4.2 `domain/book`

| 레이어 | 파일 | 책임 |
| --- | --- | --- |
| controller | `BookController` | `/books` 하위 엔드포인트 제공. 리포지토리를 **직접** 호출하고, 엔티티 → `BookResponse` 변환도 컨트롤러에서 해요. |
| dto | `BookRequest` | 책 등록 요청 바디 (`title`, `author`). |
| dto | `BookResponse` | 책 응답 데이터 (`id`, `title`, `author`). |
| entity | `Book` | `book` 테이블에 매핑되는 JPA 엔티티. `BaseEntity`를 상속해요. |
| repository | `BookRepository` | `JpaRepository<Book, Long>`. 커스텀 쿼리 메서드는 없어요. |

**현재 제공 API**

| 메서드 | 경로 | 설명 | 성공 응답 | 실패 응답 |
| --- | --- | --- | --- | --- |
| `GET` | `/books/{id}` | 책 단건 조회 | 200, `ApiResponse<BookResponse>` | 없으면 404 `BOOK-001` |
| `POST` | `/books` | 책 등록 | 201, `ApiResponse<BookResponse>` | (입력 검증 없음) |
| `GET` | `/hello` | 1주차 실습용 | 200, 평문 문자열 | - |

### 4.3 `global/common`

| 파일 | 책임 |
| --- | --- |
| `entity/BaseEntity` | `@MappedSuperclass` + `AuditingEntityListener`. `created_at`(수정 불가), `updated_at` 컬럼 제공. |
| `response/ApiResponse<T>` | 성공 응답 래퍼. `success`, `status`, `data`, `timestamp`(Asia/Seoul, `yyyy-MM-dd HH:mm:ss`). `data`가 `null`이면 필드를 생략해요(`@JsonInclude(NON_NULL)`). 정적 팩토리 `success(data)`(200), `created(data)`(201). |
| `response/ErrorResponse` | 실패 응답. `success`(항상 `false`), `status`, `code`, `message`, `method`, `path`, `timestamp`. `ErrorResponse.of(ErrorCode, HttpServletRequest)`로 생성해요. |
| `response/ResponseUtils` | 컨트롤러에서 쓰는 헬퍼. `ok(data)` → 200, `created(data)` → 201 `ResponseEntity<ApiResponse<T>>`. Lombok `@UtilityClass`. |

### 4.4 `global/config`

| 파일 | 책임 |
| --- | --- |
| `CorsConfig` | 허용 Origin 목록 `ALLOWED_ORIGINS` (`http://localhost:3000`, `http://localhost:8080`). `WebMvcConfigurer.addCorsMappings`와 `CorsConfigurationSource` 빈 **두 곳**에 같은 설정을 등록해요. 허용 메서드: GET, POST, PUT, DELETE, OPTIONS, PATCH / credentials 허용 / maxAge 3600. |
| `SecurityConfig` | `SecurityFilterChain` 정의. CORS는 `CorsConfigurationSource` 빈 사용, CSRF·formLogin·httpBasic 비활성화, 세션 `STATELESS`, `anyRequest().permitAll()`. |
| `SwaggerConfig` | `OpenAPI` 빈. title `"My API"`, description `"스터디 실습용 API 문서"`, version `v1.0.0`. |

### 4.5 `global/exception`

| 파일 | 책임 |
| --- | --- |
| `ErrorCode` | `HttpStatus`, `code`, `message`를 가진 enum. 도메인별 주석으로 그룹을 나눠요. |
| `Backend14thException` | 프로젝트 공통 커스텀 예외(`RuntimeException`). `ErrorCode`를 담고, 선택적으로 커스텀 메시지를 받을 수 있어요. |
| `GlobalExceptionHandler` | `@RestControllerAdvice`. `Backend14thException` → 해당 `ErrorCode`의 상태/응답(warn 로그), 그 외 모든 `Exception` → 500 `COMMON-002`(error 로그). |

**현재 정의된 ErrorCode**

| 상수 | HTTP | code | message |
| --- | --- | --- | --- |
| `COMMON_BAD_REQUEST` | 400 | `COMMON-001` | 잘못된 요청입니다. (정의만 되어 있고 사용처 없음) |
| `COMMON_INTERNAL_SERVER_ERROR` | 500 | `COMMON-002` | 서버 내부 오류가 발생했습니다. |
| `BOOK_NOT_FOUND` | 404 | `BOOK-001` | 책을 찾을 수 없습니다. |

`ErrorCode`에는 다른 도메인 그룹을 추가하는 방법을 보여주는 **주석 처리된 예시**만 있고, 실제로 존재하는 도메인은 `book`뿐이에요.

### 4.6 설정 파일 (`src/main/resources`)

| 파일 | 내용 |
| --- | --- |
| `application.yml` | `spring.config.import: optional:file:.env[.properties]`로 루트의 `.env`를 읽어요. 애플리케이션 이름 `backend14th`, 활성 프로필은 `SPRING_PROFILES_ACTIVE` (기본값 `local`). |
| `application-local.yml` | `local` 프로필 전용. MySQL datasource(`DB_URL` 필수, `DB_USERNAME` 기본 `root`, `DB_PASSWORD` 기본 빈 값), `ddl-auto: update`, `show-sql: true`. |

`local` 외의 프로필(dev, prod 등) 설정 파일은 없어요.

### 4.7 테스트 (`src/test`)

| 파일 | 내용 |
| --- | --- |
| `Backend14thApplicationTests` | `@SpringBootTest` 컨텍스트 로딩 테스트 1개. 실제 MySQL 연결과 유효한 `.env`가 있어야 통과해요. |

---

## 5. 요청 처리 흐름 (현재)

```
Client
  │
  ▼
Spring Security FilterChain (CORS 적용, 인증 없이 permitAll)
  │
  ▼
BookController ──(직접 호출)──▶ BookRepository ──▶ MySQL
  │
  ├─ 성공: ResponseUtils.ok / created → ApiResponse<T>
  └─ 실패: throw Backend14thException(ErrorCode)
             └─▶ GlobalExceptionHandler → ErrorResponse
```

서비스 레이어가 없기 때문에 비즈니스 로직, 트랜잭션 경계, DTO 변환이 모두 컨트롤러에 있어요.

---

## 6. 현재 구현 상태 요약

### 구현됨

- [x] 패키지 구조: `domain/<도메인>/{controller,dto,entity,repository}` + `global/{common,config,exception}`
- [x] Book 단건 조회(`GET /books/{id}`), 등록(`POST /books`)
- [x] 공통 성공 응답 `ApiResponse` + `ResponseUtils`
- [x] 공통 실패 응답 `ErrorResponse` + `ErrorCode` + `Backend14thException` + `GlobalExceptionHandler`
- [x] JPA Auditing 기반 `BaseEntity` (`createdAt`, `updatedAt`)
- [x] CORS 설정 (로컬 Origin만 허용)
- [x] Spring Security 기본 설정 (stateless, 전체 허용)
- [x] Swagger(springdoc) 설정 및 Book API 문서화
- [x] `.env` + `local` 프로필 기반 DB 설정

### 미구현 / 현재 한계

- [ ] **서비스 레이어 없음**: 컨트롤러가 리포지토리를 직접 호출해요. `@Transactional`도 사용하지 않아요.
- [ ] **입력 검증 없음**: `@Valid`, Bean Validation 어노테이션을 쓰는 DTO가 없어요. `COMMON_BAD_REQUEST`는 정의만 되어 있어요.
- [ ] **Spring 기본 예외 핸들러 없음**: `MethodArgumentNotValidException`, `HttpMessageNotReadableException`, `MethodArgumentTypeMismatchException`, 존재하지 않는 경로 등은 `Exception` 핸들러에 걸려 **500**으로 응답돼요.
- [ ] **인증/인가 없음**: 모든 요청이 `permitAll`이에요.
- [ ] Book 목록 조회, 수정, 삭제 API 없음
- [ ] `local` 외 프로필 설정 없음
- [ ] 컨텍스트 로딩 외의 단위/통합 테스트 없음 (테스트 전용 DB 설정도 없음)
- [ ] `HelloController`는 공통 응답 규칙 밖에 남아 있어요.
