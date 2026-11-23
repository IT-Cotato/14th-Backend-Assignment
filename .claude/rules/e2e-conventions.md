---
paths:
  - "frontend/e2e/**"
  - "frontend/playwright.config.ts"
---

# E2E (Playwright) 규칙

- 요소는 `getByRole`, `getByLabel`, `getByText`로만 찾는다. CSS 선택자·`locator('.class')` 금지.
- `page.waitForTimeout` 등 고정 대기 금지. `expect(...)`의 자동 대기를 쓴다.
- 테스트끼리 데이터를 공유하지 않는다. 이메일·닉네임·제목은 매 테스트 새로 만든다 (`e2e/helpers.ts`의 `uniqueUser()` 등).
- 테스트 이메일은 반드시 `e2e-`로 시작한다.
- 비밀번호를 코드에 리터럴로 적지 않는다. `process.env.E2E_PASSWORD` 또는 실행 시 랜덤 생성값을 쓴다.
- 화면에서 숨긴 동작(남의 글 수정 등)은 `request` 픽스처로 API를 직접 호출해 403/409를 함께 확인한다.
- 실패하면 원인이 앱인지 테스트인지 먼저 가린다. 통과시키려고 테스트를 지우거나 검사를 약하게 바꾸지 않는다.
- 테스트 코드는 프로덕션 코드와 커밋을 분리한다 (`test:` 타입).
