---
paths:
  - "frontend/e2e/**"
  - "frontend/playwright.config.ts"
---

# E2E 테스트 규칙 (Playwright)

- 요소는 `getByRole`, `getByLabel`, `getByText`로 찾는다. CSS 선택자나 `data-testid`에 기대지 않는다.
- `waitForTimeout` 같은 고정 대기를 쓰지 않는다. `expect(...)`가 조건이 맞을 때까지 기다린다.
- 테스트마다 독립이다. 이메일, 닉네임, 글 제목은 매번 새로 만들고(UUID 등), 앞선 테스트의 데이터에 기대지 않는다.
- 테스트 계정의 비밀번호를 코드에 적지 않는다. 실행할 때 랜덤으로 만들거나 환경 변수로 넣는다.
- 테스트 이메일은 `e2e-`로 시작하게 해서 직접 만든 데이터와 구분한다.
- 같은 DB를 쓰므로 `workers: 1`, `fullyParallel: false`를 유지한다.
- 화면에서 버튼을 숨긴 동작(남의 글/댓글 수정, 마감 글에 댓글)은 API를 직접 호출해서 403, 409가 오는지도 확인한다.
- 실패하면 원인이 앱인지 테스트인지 먼저 가린다. 통과시키려고 테스트를 지우거나 검사를 약하게 바꾸지 않는다.
- 화면의 라벨이나 버튼 이름을 바꾸면 같은 커밋에서 E2E의 로케이터도 함께 고친다.
- 실행 순서: 백엔드와 DB를 먼저 켠다 (`cd backend && ./gradlew bootRun`) -> `cd frontend && npx playwright test`. 프론트 서버는 `webServer` 설정이 띄운다.
