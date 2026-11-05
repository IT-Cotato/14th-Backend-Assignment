# CLAUDE.md

## 프로젝트 개요

Cotato 14기 백엔드 스터디 과제 프로젝트. Spring Boot 기반 REST API.

- **스택:** Java 21 · Spring Boot 4.1.1 · MySQL · Spring Data JPA · Spring Security · SpringDoc OpenAPI · Lombok
- **패키지 루트:** `com.cotato.backend14th`
- **포트:** 8080
- **프로필:** `local` (application-local.yml에 DB 접속 정보)

## 주요 명령어

```bash
./gradlew bootRun --args='--spring.profiles.active=local'   # 로컬 실행
./gradlew build                                              # 빌드
./gradlew test                                               # 테스트
```

## 항상 적용

@docs/conventions.md
@docs/architecture.md

## 문서 라우팅

| 이런 작업을 할 때 | 읽을 문서 |
|---|---|
| 코딩 스타일·네이밍·Lombok·커밋 메시지 | `docs/conventions.md` (항상 로드) |
| 패키지 구조·레이어·응답/예외 패턴 | `docs/architecture.md` (항상 로드) |

## 작업 방식

- 코드 작성 전에 **계획을 먼저 세우고 승인받은 후 실행**한다.
- 모호한 점이 있으면 추측하지 말고 **질문으로 명확하게** 한다.
- 작업 단위는 **API 1개씩**. 하나가 끝나면 다음 작업을 안내하고 승인을 기다린다.

## 영구 금지

- `.env` 파일을 git에 커밋하거나 내용을 출력하지 않는다.
- `git push --force` / `git push -f`를 실행하지 않는다.
- `main`, `develop` 브랜치에 직접 push하지 않는다.
- DB를 직접 조작하는 쿼리(DROP, TRUNCATE, DELETE without WHERE)를 실행하지 않는다.
- 사용자의 승인 없이 파일을 삭제하지 않는다.
