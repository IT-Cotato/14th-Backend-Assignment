---
paths:
  - "frontend/src/**"
---

# 프론트엔드 규칙

- Next.js 금지. Vite + React Router SPA로 만든다.
- 스타일은 Tailwind 유틸리티 클래스만 쓴다. 모바일(375px)에서 가로 스크롤이 생기지 않게 한다.
- API 호출은 `src/api/` 의 axios 인스턴스(`client`)만 쓴다. 컴포넌트에서 `fetch`/`axios`를 직접 부르지 않는다.
- 응답은 `ApiResponse`로 감싸져 있으므로 `res.data.data`를 꺼낸다. 204는 본문이 없다.
- 에러는 서버 `ErrorResponse.message`를 그대로 화면에 보여준다 (`getErrorMessage`).
- 401이 오면 토큰을 지우고 `/login`으로 보낸다 (axios 인터셉터에서 일괄 처리).
- 입력창은 반드시 `<label htmlFor>`로 연결한다. 동작은 `<button>`, 화면 이동은 `<Link>`(a 태그)로 만든다. E2E가 역할·라벨로 요소를 찾는다.
- 로딩 중에는 "불러오는 중..." 같은 상태를 표시한다.
- 로그인이 필요한 화면은 `RequireAuth`로 감싼다.
