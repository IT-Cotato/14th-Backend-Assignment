---
paths:
  - "frontend/src/**/*.{ts,tsx,css}"
  - "frontend/vite.config.ts"
---

# 프론트엔드 규칙

- React + TypeScript + Vite SPA다. Next.js, SSR 기능을 쓰지 않는다. 스타일은 Tailwind CSS 클래스로만 한다.
- 개발 서버는 3000 포트(`strictPort: true`)다. 백엔드 CORS가 3000만 허용한다.
- API 호출은 `src/api/`의 함수로만 한다. 컴포넌트에서 axios를 직접 부르지 않는다.
- 성공 응답은 `ApiResponse.data`를 꺼내서 쓰고, 실패는 `getErrorMessage(error)`로 서버의 `message`를 그대로 보여준다.
- 응답 타입은 `src/api/types.ts`에 두고 `docs/api-spec.md`의 필드 이름과 맞춘다.
- 서버 상태는 TanStack Query로 다룬다. 쓰기 후에는 관련 query key를 invalidate한다.
- 토큰은 `src/auth/token.ts`로만 읽고 쓴다. 401을 받으면 토큰을 지우고 `/login`으로 보낸다.
- 로그인이 필요한 화면은 `RequireAuth`로 감싼다.
- 접근성: 입력창에는 `<label htmlFor>`를 붙이고, 동작은 `<button>`, 이동은 `<Link>`(a)로 만든다. 에러는 `role="alert"`, 로딩은 `role="status"`로 표시한다. E2E가 역할과 라벨로 요소를 찾는다.
- 모바일 화면(360px)에서 가로 스크롤이 생기지 않게 `flex-wrap`, `min-w-0`, `break-words`를 쓴다.
- `console.log`와 주석 처리한 코드를 남기지 않는다. 바꾼 뒤 `npm run build`와 `npm run lint`를 확인한다.
