---
name: run-e2e
description: 백엔드를 띄우고 Playwright E2E 테스트를 실행한 뒤 결과를 정리한다. "E2E 돌려줘", "playwright 테스트 실행" 같은 요청에 사용.
---

# E2E 실행 절차

1. MySQL이 떠 있는지 확인한다 (`lsof -iTCP:3306 -sTCP:LISTEN`).
2. 백엔드가 떠 있지 않으면 `backend/`에서 백그라운드로 실행한다.
   `./gradlew bootRun --args='--spring.profiles.active=local'` → 로그에 `Started Backend14thApplication`이 나올 때까지 기다린다.
   - 환경 변수 파일은 읽거나 출력하지 않는다. 값이 필요하면 사용자에게 요청한다.
3. `frontend/`에서 `npx playwright test`를 실행한다. 프론트 서버는 `webServer` 설정이 띄운다.
   - 백엔드를 8080이 아닌 포트로 띄웠다면 `BACKEND_URL=http://localhost:포트 npx playwright test`
4. 실패하면 `test-results/*/error-context.md`와 스크린샷을 읽고 **앱 문제인지 테스트 문제인지 먼저** 가린다.
   - 통과시키려고 테스트를 지우거나 검사를 약하게 바꾸지 않는다 (`.claude/rules/e2e-conventions.md`).
5. 결과를 `N passed / M failed`와 실패 원인 요약으로 보고한다.
