# 아키텍처

실제 코드의 구조를 정리한 문서입니다.

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
- **CORS:** `http://localhost:3000`, `http://localhost:8080`을 허용합니다.
- **Swagger:** `SwaggerConfig`에서 API 문서 정보를 정의합니다.

## 프로필과 설정 파일

- `application.yml`: 앱 이름, `.env` 불러오기, 활성 프로필(기본값 `local`)
- `application-local.yml`: 포트, DataSource, JPA 설정
