---
paths:
  - "frontend/src/**"
---

# 프론트엔드 규칙 (React, TypeScript, Vite, Tailwind CSS)

- Next.js를 쓰지 않는다. Vite 기반 React SPA다. 개발 서버 포트는 3000 고정 (`strictPort: true`).
- API 호출은 `src/api/`의 axios 인스턴스를 거친다. 컴포넌트에서 axios를 직접 만들지 않는다.
- 서버 응답은 `{ success, status, data, timestamp }`로 감싸져 온다. `data` 언래핑은 `src/api/client.ts` 한 곳에서 한다.
- 서버가 내려준 에러 `message`를 화면에 그대로 보여준다. 로딩 중 상태도 표시한다.
- 401이 오면 저장한 토큰을 지우고 `/login`으로 보낸다. 로그인 요청 자체의 401(틀린 비밀번호)은 제외한다.
- 로그인이 필요한 화면은 `ProtectedRoute`로 감싼다.
- 입력창에는 `label`을 연결하고(`htmlFor` + `id`), 동작은 `button`, 이동은 `Link`(a)로 만든다. E2E가 역할과 라벨로 요소를 찾는다.
- 스타일은 Tailwind CSS 클래스로 쓰고, 모바일 화면에서도 깨지지 않게 한다.
- 요청/응답 타입의 필드 이름은 `docs/api-spec.md`와 똑같이 둔다 (`postId`, `studyStatus`, `isAuthor` 등).
- 새 라이브러리를 추가하면 PR에 이유를 적는다.
- 불필요한 `console.log`와 주석 처리한 코드를 남기지 않는다.
