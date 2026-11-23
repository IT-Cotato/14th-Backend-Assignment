# 코테이토 스터디 매칭 플랫폼

이슈: #121  
브랜치: `daeun/monorepo` → `daeun/basic`

## 개요

코테이토 멤버가 스터디 모집글을 올리고, 검색하고, 댓글로 참여 의사를 남기는 서비스입니다.  
백엔드는 Spring Boot 3, 프론트는 React + TypeScript + Vite입니다.

## 한 일

- 루트를 `backend/` + `frontend/` 모노레포로 나눴습니다.
- 회원가입, 로그인, JWT, 로그아웃, 내 정보를 만들었습니다.
- 스터디 모집글 CRUD, 검색/필터, 모집 마감, 댓글 CRUD를 만들었습니다.
- 프론트 화면과 Playwright E2E 5개를 통과시켰습니다.
- ERD는 `docs/erd/erd.png`에 있습니다.

## API

| 메서드 | 경로 | 설명 | 인증 |
| --- | --- | --- | --- |
| POST | `/api/auth/signup` | 회원가입 | X |
| POST | `/api/auth/login` | 로그인 | X |
| POST | `/api/auth/logout` | 로그아웃 | O |
| GET | `/api/users/me` | 내 정보 | O |
| GET | `/api/study-posts` | 모집글 목록/검색 | X |
| POST | `/api/study-posts` | 모집글 작성 | O |
| GET | `/api/study-posts/{id}` | 모집글 상세 | X |
| PATCH | `/api/study-posts/{id}` | 모집글 수정 | O |
| DELETE | `/api/study-posts/{id}` | 모집글 삭제 | O |
| GET | `/api/study-posts/{id}/comments` | 댓글 목록 | X |
| POST | `/api/study-posts/{id}/comments` | 댓글 작성 | O |
| PATCH | `/api/comments/{id}` | 댓글 수정 | O |
| DELETE | `/api/comments/{id}` | 댓글 삭제 | O |

## 화면

- `/login` 로그인
- `/signup` 회원가입
- `/mypage` 내 정보
- `/studies` 모집글 목록, 검색, 필터
- `/studies/new` 모집글 작성
- `/studies/:id` 상세, 댓글, 마감, 삭제
- `/studies/:id/edit` 모집글 수정

## 실행

백엔드:

```bash
cd backend
export JAVA_HOME="/opt/homebrew/opt/openjdk@17"
./gradlew bootRun