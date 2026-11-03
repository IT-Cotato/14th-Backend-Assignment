# 개발 환경 가이드

## 1. 개발 환경

본 프로젝트는 Spring Boot 기반 백엔드 애플리케이션이다.

| 항목 | 기술 |
|---|---|
| 언어 | Java 21 |
| 프레임워크 | Spring Boot 4.1.1 |
| 빌드 도구 | Gradle Wrapper |
| 데이터베이스 | MySQL |
| ORM | Spring Data JPA |
| 보안 | Spring Security |
| API 문서화 | Springdoc OpenAPI |

## 2. 프로젝트 빌드

프로젝트 루트 디렉터리에서 다음 명령어를 실행한다.

```bash
./gradlew build
```

빌드 과정에서 컴파일, 테스트 및 패키징 작업이 수행된다.

정상적으로 완료되면 다음 메시지가 출력된다.

```text
BUILD SUCCESSFUL
```

## 3. 테스트 실행

전체 테스트를 실행하려면 다음 명령어를 사용한다.

```bash
./gradlew test
```

테스트가 실패하면 오류 로그를 확인하고 원인을 분석한다.

테스트 실패를 무시하거나 임의로 테스트를 비활성화하지 않는다.

## 4. 애플리케이션 실행

다음 명령어로 Spring Boot 애플리케이션을 실행한다.

```bash
./gradlew bootRun
```

현재 확인된 서버 포트는 `8080`이다.

서버가 정상적으로 시작되면 다음과 유사한 로그가 출력된다.

```text
Tomcat started on port 8080
Started Backend14thApplication
```

실행 중인 서버를 종료하려면 터미널에서 `Ctrl + C`를 누른다.

## 5. 애플리케이션 설정

설정 파일은 `src/main/resources/`에 위치한다.

- `application.yml`: 애플리케이션 공통 설정
- `application-local.yml`: 로컬 환경 설정

실제 데이터베이스 연결 정보, 환경 변수 및 활성 프로필은 설정 파일을 확인한다.

민감한 설정값은 Git에 커밋하지 않는다.

## 6. Swagger UI

프로젝트에는 Springdoc OpenAPI 의존성이 포함되어 있다.

서버 실행 후 다음 주소에서 Swagger UI 접근을 시도할 수 있다.

http://localhost:8080/swagger-ui/index.html

Swagger UI 접근이 제한되면 Spring Security 및 Swagger 설정을 확인한다.

## 7. 개발 시 주의 사항

- 프로젝트에서 지정한 Java 버전을 사용한다.
- Gradle Wrapper를 통해 빌드 및 테스트를 실행한다.
- 기존 설정 파일과 환경별 설정을 확인한 후 변경한다.
- 데이터베이스 연결 정보를 코드에 직접 작성하지 않는다.
- 기능 변경 후 관련 테스트를 실행한다.
- 빌드 또는 테스트 실패 시 원인을 확인하고 결과를 보고한다.