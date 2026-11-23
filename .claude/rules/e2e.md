---
paths:
  - "frontend/e2e/**"
  - "frontend/playwright.config.ts"
---

# E2E 테스트 규칙 (Playwright)

`frontend/e2e/`의 테스트나 `playwright.config.ts`를 읽거나 고칠 때 지켜요.

## 실행
- 백엔드 서버와 DB를 먼저 켜요. (`backend/`에서 `./gradlew bootRun`) 꺼져 있으면 `e2e/global-setup.ts`가 바로 실패시켜요.
- 프론트는 `webServer`가 띄워요. 이미 3000번에 떠 있으면 그대로 써요(`reuseExistingServer`).
- `frontend/`에서 `npx playwright test` 또는 `npm run test:e2e`
- 모든 테스트가 같은 로컬 DB를 써서 `workers: 1`로 한 번에 하나씩 실행해요. 이 값을 올리지 않아요.

## 파일 구조
- 기능별 파일로 나눠요: `auth.spec.ts`, `study-post.spec.ts`, `comment.spec.ts`
- 공용 코드는 `e2e/support/`에 둬요 (`data.ts` 테스트 데이터, `api.ts` API 호출, `fixtures.ts` 픽스처)
- 상대 import에는 `.ts` 확장자를 붙여요. (`tsconfig.node.json`이 `module: nodenext`라서)

## 요소 찾기
- `getByRole`, `getByLabel`, `getByText`만 써요.
- CSS·XPath 선택자, `locator('.class')`, `locator('#id')`, `data-testid`는 쓰지 않아요. 찾기 어려우면 테스트 대신 화면의 접근성(label, role, 이름)을 고쳐요.
- 이름은 계획에 정리한 이름표(label·button·link·heading 이름)를 써요.
- Playwright의 `name`은 기본이 부분 일치예요. 글 제목처럼 사용자가 정한 이름이나, 다른 요소와 겹칠 수 있는 이름(`로그인`, `수정`, `삭제` 등)은 `{ exact: true }`로 찾아요.
- 같은 이름이 여러 개면 `getByRole('region', { name: '댓글' })`, `getByRole('listitem').filter({ hasText })`처럼 범위를 좁혀요.

## 기다리기
- `waitForTimeout` 같은 고정 대기는 쓰지 않아요.
- `expect(...)`의 자동 대기, `toHaveURL`, `waitForResponse`처럼 조건을 기다려요.
- `window.confirm`은 클릭 전에 `page.once('dialog', (d) => d.accept())`로 처리해요.

## 테스트 독립성
- 테스트마다 이메일·닉네임·글 제목을 새로 만들어요. 이메일은 `e2e-`로 시작해요.
- 다른 테스트가 만든 데이터나 실행 순서에 기대지 않아요.
- 시나리오의 주제가 아닌 준비 단계(가입, 글 작성 등)는 API로 빠르게 만들어도 돼요. 시나리오 자체는 화면으로 해요.
- 테스트가 만든 모집글은 `trackPost`로 등록해서 테스트가 끝나면 지워요. (댓글도 함께 지워져요)

## 비밀 값
- 테스트 계정 비밀번호를 코드에 고정값으로 적지 않아요. 실행할 때 `crypto.randomUUID()`로 만들어요.
- `.env`나 실제 계정 정보를 테스트에 넣지 않아요.

## 권한 검사
- 화면에서 숨긴 동작(남의 글·댓글 수정·삭제, 마감된 글에 댓글)은 두 가지를 모두 확인해요.
  1. 화면에 버튼·폼이 보이지 않는지
  2. API를 직접 불러서 서버가 막는지 (`status`와 응답 `code`까지. 예: 403 `STUDY-002`, 409 `STUDY-003`)

## 실패했을 때
- 통과시키려고 테스트를 지우거나, `skip`하거나, 검사를 약하게 바꾸지 않아요.
- trace·스크린샷·에러 메시지를 보고 원인이 **앱**인지 **테스트**인지 먼저 판단해서 사용자에게 알려요.
- 앱 버그면 앱을 고치는 방향을 제안하고, 테스트 문제면 무엇을 왜 고치는지 설명한 뒤 고쳐요.
