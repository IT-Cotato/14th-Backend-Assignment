# 프로젝트 구조

## 패키지 구조

```
com.cotato.backend14th
├── Backend14thApplication.java   # 스프링 부트 엔트리 포인트
├── HelloController.java          # 헬스체크/샘플 컨트롤러
├── domain/
│   └── book/
│       ├── controller/   # BookController - API 엔드포인트
│       ├── dto/          # BookRequest, BookResponse
│       ├── entity/       # Book
│       └── repository/   # BookRepository (Spring Data JPA)
└── global/
    ├── common/
    │   ├── entity/    # BaseEntity - 생성/수정 시각 등 공통 컬럼
    │   └── response/  # ApiResponse, ErrorResponse, ResponseUtils
    ├── config/        # CorsConfig, SecurityConfig, SwaggerConfig
    └── exception/     # Backend14thException, ErrorCode, GlobalExceptionHandler
```

## 레이어 설명

- **controller**: HTTP 요청을 받아 DTO로 변환하고, `ApiResponse`로 감싸서 응답한다.
- **dto**: 요청/응답 스펙을 정의한다. 엔티티를 그대로 노출하지 않는다.
- **entity**: JPA 엔티티. `BaseEntity`를 상속해 생성/수정 시각 등 공통 필드를 공유한다.
- **repository**: Spring Data JPA 인터페이스.
- **global/common/response**: 모든 API가 공통으로 쓰는 응답 포맷. 성공은 `ApiResponse`, 실패는 `ErrorResponse`로 통일한다.
- **global/exception**: `Backend14thException` + `ErrorCode` 조합으로 비즈니스 예외를 표현하고, `GlobalExceptionHandler`가 이를 받아 `ErrorResponse`로 변환한다.
- **global/config**: CORS, Security, Swagger 등 애플리케이션 전역 설정.

## 환경 설정

- `application.yml`에서 `spring.config.import`로 `.env` 파일을 선택적으로 읽어온다.
- 프로필은 `SPRING_PROFILES_ACTIVE` 환경 변수로 제어하며, 기본값은 `local`이다.
