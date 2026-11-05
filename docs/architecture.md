# 아키텍처

## 패키지 구조

```
src/main/java/com/cotato/backend14th/
├── domain/                        # 도메인별 비즈니스 로직
│   └── {도메인}/
│       ├── controller/            # @RestController
│       ├── service/               # 인터페이스 + Impl (필요 시)
│       ├── dto/                   # Request / Response
│       ├── entity/                # JPA 엔티티
│       └── repository/            # JpaRepository
└── global/                        # 프로젝트 공통
    ├── config/                    # SwaggerConfig, CorsConfig, SecurityConfig
    ├── common/
    │   ├── entity/                # BaseEntity (createdAt, updatedAt)
    │   └── response/              # ApiResponse, ErrorResponse, ResponseUtils
    └── exception/                 # ErrorCode, Backend14thException, GlobalExceptionHandler
```

## 레이어

```
Controller → Service → Repository → Entity
```

- Controller: REST 엔드포인트. 비즈니스 로직 없이 Service 호출만
- Service: 비즈니스 로직 담당
- Repository: Spring Data JPA
- Entity: JPA 엔티티, `BaseEntity` 상속 (createdAt, updatedAt)

## 응답 형식

### 성공 응답 — `ApiResponse<T>`

```json
{
  "success": true,
  "status": 200,
  "data": { ... },
  "timestamp": "2025-01-01 12:00:00"
}
```

Controller에서는 `ResponseUtils.ok(data)` 또는 `ResponseUtils.created(data)` 사용.

### 에러 응답 — `ErrorResponse`

```json
{
  "success": false,
  "status": 404,
  "code": "BOOK-001",
  "message": "책을 찾을 수 없습니다.",
  "method": "GET",
  "path": "/books/999",
  "timestamp": "2025-01-01 12:00:00"
}
```

## 예외 처리

- 커스텀 예외: `throw new Backend14thException(ErrorCode.BOOK_NOT_FOUND)`
- 에러 코드: `ErrorCode` enum (httpStatus, code, message)
- `GlobalExceptionHandler`(@RestControllerAdvice)에서 일괄 처리 → Controller에서 try-catch 금지
