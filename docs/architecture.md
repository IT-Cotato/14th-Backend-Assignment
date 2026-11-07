# 아키텍처

## 패키지 구조

```
com.cotato.backend14th
├── Backend14thApplication.java     # @EnableJpaAuditing
├── global/                         # 도메인과 무관한 공통 코드
│   ├── config/                     # SecurityConfig, CorsConfig, SwaggerConfig
│   ├── exception/                  # ErrorCode, Backend14thException, GlobalExceptionHandler
│   └── common/
│       ├── response/               # ApiResponse, ErrorResponse, ResponseUtils
│       └── entity/                 # BaseEntity (createdAt, updatedAt)
└── domain/                         # 기능(도메인)별 패키지
    └── <도메인>/
        ├── controller/             # HTTP 진입점, Swagger 문서화
        ├── service/                # 비즈니스 로직 (5주차부터 추가)
        ├── repository/             # Spring Data JPA
        ├── dto/                    # 요청/응답 DTO
        └── entity/                 # JPA 엔티티
```

- 새 도메인은 `domain/<도메인>/` 아래에 같은 하위 구조로 만든다. 예제는 `domain/book/`.
- `global/`에는 두 개 이상의 도메인이 같이 쓰는 코드만 둔다.

## 요청 처리 흐름

### 성공
```
Client -> SecurityFilterChain (CORS, permitAll)
       -> Controller -> (Service) -> Repository
       -> ResponseUtils.ok(dto) / ResponseUtils.created(dto)
       -> ApiResponse<T> { success, status, data, timestamp }
```

### 실패
```
어디서든 throw new Backend14thException(ErrorCode.X)
  -> GlobalExceptionHandler.handleBackend14thException (log.warn)
  -> ErrorResponse { success=false, status, code, message, method, path, timestamp }

예상하지 못한 Exception
  -> GlobalExceptionHandler.handleException (log.error)
  -> ErrorCode.COMMON_INTERNAL_SERVER_ERROR (500)
```

## 응답 예시

성공
```json
{ "success": true, "status": 200, "data": { "id": 1, "title": "...", "author": "..." }, "timestamp": "2026-11-05 14:00:00" }
```

실패
```json
{ "success": false, "status": 404, "code": "BOOK-001", "message": "책을 찾을 수 없습니다.", "method": "GET", "path": "/books/99", "timestamp": "2026-11-05 14:00:00" }
```

## 설정
- `application.yml`: `.env`를 `optional:file:.env[.properties]`로 import, 기본 profile `local`
- `application-local.yml`: MySQL 접속(`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`), `ddl-auto: update`, `show-sql: true`
- Security: CSRF/formLogin/httpBasic 비활성, STATELESS, 현재 모든 요청 허용
