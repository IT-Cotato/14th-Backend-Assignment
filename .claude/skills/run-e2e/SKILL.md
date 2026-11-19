---
name: run-e2e
description: 백엔드와 프론트를 함께 띄워 Playwright E2E를 실행하고 실패 원인을 가린다. E2E 실행, 테스트 실패 분석, PR용 E2E 결과 준비를 요청할 때 사용한다.
---

# E2E 실행

1. 백엔드가 떠 있는지 확인한다. `curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/study-posts`가 200이어야 한다.
   - 아니면 `backend/`에서 `./gradlew bootRun`을 백그라운드로 실행하고 200이 될 때까지 기다린다. `.env` 내용은 읽지 않는다.
2. `frontend/`에서 의존성과 브라우저를 확인한다. `node_modules`가 없으면 `npm install`, 브라우저가 없으면 `npx playwright install chromium`.
3. `npx playwright test`를 실행한다. 프론트 서버는 설정의 `webServer`가 띄운다.
4. 실패하면 `test-results/`의 에러 문맥과 스크린샷을 보고 원인을 나눈다.
   - 앱 문제: 응답 필드·상태 코드가 `docs/api-spec.md`와 다르다, 화면에 에러가 안 보인다 → 앱을 고친다.
   - 테스트 문제: 라벨·버튼 이름이 화면과 다르다, 데이터가 겹친다 → 테스트를 고친다. 검사를 지우거나 약하게 바꾸지 않는다.
5. 모두 통과하면 `N passed` 줄과 테스트 목록을 보고한다. PR에는 사용자가 터미널을 캡처해 붙인다. 캡처에 토큰이나 비밀번호가 없는지 확인한다.

이 스킬은 실행과 원인 분석까지 한다. 커밋과 push는 사용자가 요청한 범위에서만 한다.
