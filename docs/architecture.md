# 아키텍처

실제 코드의 구조를 정리한 문서입니다. 백엔드 코드는 `backend/src/main/java/com/cotato/backend14th/` 아래에 있습니다.

## 패키지 구조

```text
com.cotato.backend14th
├─ Backend14thApplication   앱 시작점, JPA Auditing 활성화
├─ HelloController          GET /hello
├─ domain/
│  └─ book/
│     ├─ controller/        BookController
│     ├─ dto/               BookRequest, BookResponse
│     ├─ entity/            Book
│     └─ repository/        BookRepository
└─ global/
   ├─ common/entity/        BaseEntity
   ├─ common/response/      ApiResponse, ErrorResponse, ResponseUtils
   ├─ config/               CorsConfig, SecurityConfig, SwaggerConfig
   └─ exception/            ErrorCode, Backend14thException, GlobalExceptionHandler
```

## 패키지 역할

- **`domain/`:** 도메인별 코드를 둡니다. 현재 도메인은 `book` 하나입니다.
- **`global/common/`:** 공통 엔티티와 응답 형식을 둡니다.
- **`global/config/`:** CORS, Security, Swagger 설정을 둡니다.
- **`global/exception/`:** 에러 코드, 예외 클래스, 전역 예외 처리를 둡니다.
- **패키지 루트:** `Backend14thApplication`과 `HelloController`가 있습니다.

## 레이어

| 레이어 | 상태 | 역할 |
|---|---|---|
| Controller | 있음 | 요청을 받고, repository를 호출하고, 엔티티를 응답 DTO로 변환해 반환 |
| Service | **현재 없음** | `@Service`와 service 패키지가 없음 |
| Repository | 있음 | `JpaRepository<Book, Long>`을 상속해 DB에 접근 |

`BookController`는 생성자로 `BookRepository`를 주입받아 직접 호출합니다.

### 5,6주차부터 적용할 구조

새 도메인(User, StudyPost, StudyPostComment)은 Controller → Service → Repository로 작성합니다.

| 레이어 | 역할 |
|---|---|
| Controller | 요청 검증(`@Valid`), service 호출, 응답 반환 |
| Service | 권한 검사(작성자 확인), 비즈니스 규칙(예: 마감된 글에는 댓글 불가), 트랜잭션 |
| Repository | DB 접근 |

3주차 Book 예제는 지금 구조 그대로 둡니다.

## 요청 흐름

### `GET /hello`

```text
HelloController → 문자열 반환
```

### `GET /books/{id}`

```text
BookController → BookRepository.findById
  ├─ 있음: BookResponse 변환 → ResponseUtils.ok (200)
  └─ 없음: Backend14thException(BOOK_NOT_FOUND)
           → GlobalExceptionHandler → ErrorResponse (404)
```

### `POST /books`

```text
BookController → BookRepository.save
  → BookResponse 변환 → ResponseUtils.created (201)
```

## 현재 설정

- **Security:** 모든 요청을 허용합니다(`permitAll`). 세션은 `STATELESS`이고 CSRF, 폼 로그인, HTTP Basic은 꺼져 있습니다.
  - 5,6주차에서는 [API 명세](api-spec.md) 3장의 권한(공개, 로그인, 작성자)에 맞게 토큰 인증으로 바꿔야 합니다.
  - 로그아웃한 토큰은 인메모리(토큰 → 만료 시각)로 관리합니다. 서버를 재시작하면 목록이 사라집니다.
- **주소:** 5,6주차 API는 `/api`로 시작합니다. 3주차 예제(`/hello`, `/books`)는 예외로 그대로 둡니다.
- **CORS:** `http://localhost:3000`, `http://localhost:8080`을 허용합니다.
- **Swagger:** `SwaggerConfig`에서 API 문서 정보를 정의합니다.

## 프로필과 설정 파일

- `application.yml`: 앱 이름, `.env` 불러오기, 활성 프로필(기본값 `local`)
- `application-local.yml`: 포트, DataSource, JPA 설정
