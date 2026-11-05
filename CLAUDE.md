# 14th-Backend-Assignment

코테이토 14기 백엔드 파트 과제 프로젝트예요. 5~6주차에 이 위에 지원자관리 시스템을 개발해요.
(도메인 설명은 5주차에 요구사항이 공개되면 추가할 예정)

## 기술 스택
- Java 17, Spring Boot 4.1.1, Gradle
- Spring Data JPA + MySQL (로컬 DB 이름: `backend14th`)
- Spring Security (현재는 전체 `permitAll`)
- SpringDoc OpenAPI(Swagger) 2.8.5, Lombok

## 주요 명령어
- 빌드: `./gradlew build` (Windows PowerShell에서는 `.\gradlew build`)
- 테스트: `./gradlew test`
- 실행: `./gradlew bootRun`
- Swagger UI: http://localhost:8080/swagger-ui/index.html

## 프로젝트 구조 (도메인형)
```
src/main/java/com/cotato/backend14th/
├── domain/{도메인}/          # controller, dto, entity, repository
└── global/
    ├── common/entity/        # BaseEntity
    ├── common/response/      # ApiResponse, ErrorResponse, ResponseUtils
    ├── config/               # Swagger, CORS, Security 설정
    └── exception/            # ErrorCode, Backend14thException, GlobalExceptionHandler
```
- 특정 도메인에 속하지 않는 공통 코드는 `global/`에 둬요.

## 핵심 컨벤션
- 성공 응답은 `ResponseUtils.ok()` / `ResponseUtils.created()`로 감싸요.
- 예외는 `ErrorCode`에 상수를 추가하고 `Backend14thException`으로 던져요. 코드 형식은 `도메인-001` (예: `BOOK-001`).
- 엔티티는 `BaseEntity`를 상속해요.
- 커밋 메시지는 `타입: 요약` 형식이에요. (feat, fix, docs, style, refactor, chore)

## 상황별로 읽을 문서
- 브랜치, 커밋, 이슈, PR 작업 → `docs/git-convention.md`
- 패키지 위치나 응답·예외 구조가 헷갈릴 때 → `docs/project-structure.md`
- 새 도메인을 만들 때 → `new-domain` 스킬 사용

## 절대 하지 않는 것
- `main`, `develop` 브랜치에 직접 push하지 않아요. 작업은 항상 작업 브랜치에서 해요.
- `git push --force`를 쓰지 않아요.
- `.env` 파일을 읽거나, 출력하거나, 커밋하지 않아요. DB 비밀번호 같은 민감 정보는 `.env`에만 둬요.
- `application*.yml`에 비밀번호를 직접 적지 않아요. `${DB_PASSWORD}`처럼 참조만 해요.
- 요청받지 않은 설정 파일(`global/config/`, `build.gradle`)은 고치지 않아요. 고쳐야 하면 먼저 물어봐요.

## 작업 방식
- 큰 작업은 바로 코드를 쓰지 말고, 계획을 먼저 보여주고 승인을 받아요.
- 작업이 끝나면 `./gradlew build`가 성공하는지 확인해요.
