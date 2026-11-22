# 코테이토 스터디 매칭 플랫폼

COTATO 14기 회원이 스터디 모집글을 올리고, 관심 있는 모집글에 댓글을 남겨 참여를 신청하는 웹 서비스입니다. React 프론트엔드와 Spring Boot REST API를 하나의 저장소에서 관리합니다.

## 주요 기능

- 회원가입, 로그인, 내 정보 조회, 로그아웃
- 모집글 목록·상세 조회, 제목·내용 검색, 모집 상태·작성자 파트 필터, 페이지네이션
- 로그인 사용자의 모집글 작성과 작성자 본인의 수정·삭제
- 공개 댓글 목록과 로그인 사용자의 댓글 작성, 댓글 작성자 본인의 수정·삭제
- 모집 마감 시 새 댓글 작성 제한, 모집글 삭제 시 연결 댓글 삭제

## 기술 스택

| 구분 | 기술 |
| --- | --- |
| 프론트엔드 | React 19, TypeScript 6, Vite 8, React Router 7, ESLint |
| 백엔드 | Java 17, Spring Boot 3.5.16, Gradle Wrapper 8.14.5, Spring Data JPA, Spring Security |
| 인증 및 저장소 | Bearer JWT, BCrypt, MySQL |
| 테스트 및 API 문서 | JUnit, Spring Boot Test, H2, Springdoc OpenAPI |

## 디렉터리 구조

```text
.
├── backend/
│   ├── src/main/    # REST API, 도메인 코드, 애플리케이션 설정
│   ├── src/test/    # 백엔드 테스트
│   └── gradle/      # Gradle Wrapper 설정
├── frontend/
│   ├── src/api/     # 공통 HTTP 클라이언트와 API 호출
│   ├── src/pages/   # 인증 및 모집글 화면
│   ├── src/components/ # 댓글 UI
│   └── src/hooks/   # 조회 요청 상태 관리
└── docs/           # API 명세, ERD, 개발 문서
```

## 로컬 실행

Java 17, MySQL, Node.js와 npm이 필요합니다. 프론트엔드 빌드와 린트는 Node.js 24 및 npm 11 환경에서 검증했습니다.

### 1. 백엔드

MySQL에 개발용 데이터베이스를 준비합니다. 기본 `local` 프로필은 `ddl-auto: update`를 사용하므로 개발 전용 데이터베이스에 연결합니다.

| 설정 | 설명 |
| --- | --- |
| `DB_URL` | 개발용 MySQL 데이터베이스의 JDBC 주소 |
| `DB_USERNAME` | 개발용 MySQL 사용자 이름 |
| `DB_PASSWORD` | 개발용 MySQL 사용자 비밀번호 |
| `JWT_SIGNING_KEY` | Base64 디코딩 결과가 최소 32바이트인 JWT 서명 키 |
| `SPRING_PROFILES_ACTIVE` | 선택 설정. 기본값 `local` |

DB 연결값은 셸 환경변수로 지정하거나, [backend/.env.example](backend/.env.example)을 참고해 로컬 `backend/.env`에 설정합니다. 예제 파일에는 DB 연결 항목만 있으므로 JWT 키는 별도로 설정합니다. 아래 명령은 실행 세션에 사용할 키를 생성합니다.

```sh
cd backend
export JWT_SIGNING_KEY="$(openssl rand -base64 32)"
./gradlew bootRun
```

백엔드 기본 주소는 `http://localhost:8080`입니다. 실제 DB 비밀번호와 서명 키를 담은 로컬 설정 파일은 Git에 추가하지 않습니다.

### 2. 프론트엔드

저장소 루트에서 별도 터미널을 열고 실행합니다.

```sh
cd frontend
npm ci
npm run dev
```

터미널에 표시된 Vite 주소를 브라우저에서 엽니다. 기본 개발 주소는 `http://localhost:5173`이며, Vite는 `/api` 요청을 `http://127.0.0.1:8080`으로 프록시합니다. 이 구성에서 프론트엔드 환경변수는 필요하지 않습니다. 별도 API 서버를 직접 호출할 때는 `VITE_API_BASE_URL`에 `/api` 앞부분의 서버 주소를 지정합니다.

## API와 설계 문서

- [API 명세서](docs/api-spec.md) · 인증 4개, 모집글 5개, 댓글 4개 API
- [ERD](docs/erd.png)
- [공통 응답 및 예외 처리 규칙](docs/api-conventions.md)
- [아키텍처](docs/architecture.md) · [개발 환경 가이드](docs/development-guide.md)

## 테스트 및 빌드

저장소 루트에서 실행합니다.

```sh
(cd backend && ./gradlew test && ./gradlew build)
(cd frontend && npm ci && npm run build && npm run lint)
```

백엔드 통합 테스트는 H2와 테스트용 JWT 키를 사용합니다. MySQL과 브라우저를 함께 확인하려면 개발용 MySQL에 연결한 백엔드와 Vite를 실행한 뒤, 테스트 계정으로 회원가입·로그인·모집글·댓글·로그아웃 흐름을 확인합니다.
