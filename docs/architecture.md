# 프로젝트 아키텍처

## 1. 패키지 구조

베이스 패키지: `com.cotato.backend14th`

```text
com.cotato.backend14th
├── Backend14thApplication.java
├── HelloController.java
├── domain/
│   └── book/
│       ├── controller/
│       ├── dto/
│       ├── entity/
│       └── repository/
└── global/
    ├── common/
    │   ├── entity/
    │   └── response/
    ├── config/
    └── exception/
```

## 2. 패키지별 역할

### domain

비즈니스 도메인별 코드를 관리한다.

- `controller`: HTTP 요청 처리
- `dto`: 요청 및 응답 데이터 전달
- `entity`: JPA 엔티티
- `repository`: 데이터베이스 접근

현재 `book` 도메인이 있으며 별도의 `service` 패키지는 존재하지 않는다.

### global

여러 도메인에서 공통으로 사용하는 코드를 관리한다.

- `common/entity`: 공통 엔티티
- `common/response`: 공통 API 응답
- `config`: CORS, Security, Swagger 설정
- `exception`: 공통 예외 처리

## 3. 코드 배치 원칙

- 도메인별 코드는 `domain/{도메인명}/`에 배치한다.
- 공통 기능은 기존 `global/` 구조를 참고한다.
- 새로운 패키지나 계층을 추가할 때 기존 구조와의 일관성을 고려한다.
- API 응답 및 예외 처리 규칙은 `docs/api-conventions.md`를 참고한다.