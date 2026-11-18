# 프로젝트 구조

## 패키지 구조

레포는 모노레포이고 백엔드 코드는 `backend/` 아래에 있다. (`backend/src/main/java/...`)

베이스 패키지: `com.cotato.backend14th`

```
com.cotato.backend14th
├── Backend14thApplication.java    (@EnableJpaAuditing)
├── HelloController.java           (GET /hello, 1주차 실습 코드)
├── domain/
│   └── book/
│       ├── controller/BookController.java
│       ├── dto/BookRequest.java, BookResponse.java
│       ├── entity/Book.java
│       └── repository/BookRepository.java
└── global/
    ├── config/                    SwaggerConfig, CorsConfig, SecurityConfig
    ├── common/
    │   ├── entity/BaseEntity.java
    │   └── response/              ApiResponse, ErrorResponse, ResponseUtils
    └── exception/                 ErrorCode, Backend14thException, GlobalExceptionHandler
```

- `domain/`: 기능(도메인) 단위. 도메인 하나가 폴더 하나
- `global/`: 특정 도메인에 속하지 않는 공통 코드 (설정, 공통 응답·엔티티, 예외)

## 도메인 폴더 구성

도메인 폴더 안에 `controller`, `service`, `dto`, `entity`, `repository`를 둔다.

- `controller`: 요청을 받고 응답을 반환. 경로와 Swagger 어노테이션을 둔다. 로직은 Service에 맡긴다
- `service`: 비즈니스 로직과 트랜잭션(`@Transactional`)을 담당. Repository를 호출하고 엔티티를 DTO로 변환한다
- `dto`: 요청(`BookRequest`)과 응답(`BookResponse`) 객체
- `entity`: DB 테이블과 매핑되는 JPA 엔티티
- `repository`: `JpaRepository`를 상속해 DB에 접근

## 계층 규칙

- 호출 순서는 Controller → Service → Repository. Controller는 Repository를 직접 호출하지 않는다
- 기존 `book` 도메인은 Service 없이 Controller가 Repository를 직접 호출하는 상태 그대로 둔다 (5,6주차 이전 코드)
- 엔티티는 Controller 응답으로 직접 반환하지 않고, DTO로 변환해서 반환
- 도메인 공통 코드는 `global/`, 도메인 전용 코드는 `domain/<도메인>/`에 둔다
