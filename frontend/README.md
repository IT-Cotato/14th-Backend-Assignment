# 코테이토 스터디 매칭 - 프론트엔드

React + TypeScript + Vite + Tailwind CSS로 만든 스터디 매칭 플랫폼 화면이에요.
백엔드(`../backend`, Spring Boot) API를 Axios로 호출해요. 기능 설명은 [기능 문서](../docs/features/117-study-platform.md)를 참고해요.

## 실행

```bash
npm install
npm run dev        # http://localhost:3000, /api 요청은 http://localhost:8080으로 프록시돼요
```

백엔드도 실행되어 있어야 해요. (`cd ../backend && ./gradlew bootRun`)

## 빌드 · 정적 검사

```bash
npm run build      # tsc -b(앱 + E2E 타입 검사) 후 vite build
npm run lint       # oxlint
```

## E2E 테스트 (Playwright)

```bash
npx playwright install chromium   # 처음 한 번
npm run test:e2e                  # 전체 E2E (Chromium)
npm run test:e2e:report           # HTML 리포트 열기
```

- 실제 백엔드(8080)와 프론트엔드(3000)를 사용해요. 이미 실행 중이면 재사용하고, 없으면 `playwright.config.ts`가 띄워요.
- 백엔드는 `backend/.env`의 **로컬 개발용 DB**에 연결돼요. 운영·공유 DB를 가리키고 있다면 실행하지 마세요.
- 고정된 테스트 계정 풀(`e2e.pool.*@example.com`)을 재사용하고, 테스트가 만든 모집글·댓글은 테스트가 끝나면 지워요.
- 결과물: `test-results/`(실패 스크린샷·trace), `playwright-report/` (둘 다 Git 제외)
- `e2e/screenshots.spec.ts`는 실행할 때마다 `docs/screenshots/`의 화면 캡처를 다시 만들어요.

## 폴더

| 경로 | 내용 |
| --- | --- |
| `src/api` | Axios 클라이언트(JWT 첨부, 401 처리), API 함수, 오류 변환 |
| `src/auth` | 로그인 상태(AuthProvider), 보호 라우트(RequireAuth), 로그아웃 흐름 |
| `src/pages` | 회원가입, 로그인, 모집글 목록·상세·작성·수정, 마이페이지 |
| `src/components` | 공통 UI, 댓글 영역 |
| `src/types` | API 요청·응답 타입 |
| `e2e` | Playwright 테스트 |
