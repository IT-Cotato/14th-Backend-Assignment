---
paths:
  - "frontend/e2e/**"
  - "frontend/playwright.config.ts"
---

# E2E 테스트 규칙 (Playwright)

## 실행 전
- 백엔드가 `http://localhost:8081`에 떠 있어야 합니다. (다른 주소면 `E2E_API_URL`)
- 백엔드 코드를 바꿨다면 서버 재시작을 사용자에게 요청합니다. Claude가 직접 `bootRun`을 실행하지 않습니다.
- 브라우저가 없다는 에러(`Executable doesn't exist`)는 앱 문제가 아닙니다. `npx playwright install chromium`으로 해결합니다.
- 프론트 서버는 `playwright.config.ts`의 `webServer`가 띄웁니다.

## 작성
- 요소는 `getByRole`, `getByLabel`, `getByText`로 찾습니다. CSS 셀렉터, `data-testid`에 의존하지 않습니다.
- 이름이 겹치면 `{ exact: true }`나 상위 영역(`getByRole('list', { name })`, `getByRole('article')`)으로 범위를 좁힙니다.
- `waitForTimeout` 같은 고정 대기를 쓰지 않습니다. `await expect(...).toBeVisible()`처럼 조건을 기다립니다.
- 헬퍼는 `e2e/helpers.ts`에 둡니다. import할 때는 `'./helpers.ts'`처럼 **확장자를 붙입니다.** (`tsconfig.node.json`이 `nodenext`라서 빼면 `npm run build`가 실패합니다)
- `response.json()` 결과는 타입을 지정해서 씁니다. (`npm run build`가 `e2e/`도 타입 검사합니다)

## 데이터와 독립성
- 테스트마다 계정, 닉네임, 제목을 새로 만듭니다. (`newUser()`, `uniqueTag()`) 앞선 테스트의 데이터에 기대지 않습니다.
- 테스트 이메일은 `e2e-`로 시작합니다.
- 비밀번호를 코드에 적지 않습니다. 실행할 때 `randomUUID()`로 만듭니다.
- 사전 데이터(다른 사용자의 글, 마감된 글 등)는 API 헬퍼로 만들고, 검증하려는 동작만 화면으로 합니다.
- 목록과 검색 검증은 고유 태그로 범위를 좁혀서 다른 데이터의 영향을 받지 않게 합니다.

## 권한과 에러
- 화면에서 버튼을 숨긴 동작(남의 글·댓글 수정 등)은 버튼이 없음을 확인하고, API를 직접 호출해서 403/409와 `code`도 확인합니다. (`expectError`)

## 실패했을 때
- 원인이 앱인지 테스트인지 먼저 가립니다. 통과시키려고 테스트를 지우거나 검사를 약하게 바꾸지 않습니다.
- 앱 버그를 고쳤다면 별도 커밋으로 남깁니다.
- 통과한 뒤 `--repeat-each=3`으로 불안정한 테스트가 없는지 확인합니다.
- `playwright-report/`, `test-results/`는 Git에 올리지 않습니다. (루트 `.gitignore`)
