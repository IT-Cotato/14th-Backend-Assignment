# 로컬 개발 환경 점검

Java 17, 로컬 MySQL, Node.js 22.12 이상을 준비한다. 검증 전용 MySQL 스키마를 만든다. local 프로필은 Hibernate의 `ddl-auto: update`로 스키마를 갱신한다.

1. `backend/.env.example`을 `backend/.env`로 복사해 네 항목을 채운다. DB_URL 형식은 `jdbc:mysql://localhost:3306/<스키마>`다. JWT_SECRET은 무작위 32바이트 이상을 Base64로 인코딩한 값이다.
2. `backend/`에서 `./gradlew build`, `./gradlew bootRun`을 실행한다. Windows에서는 `./gradlew.bat`를 사용한다.
3. `http://localhost:8080/swagger-ui/index.html`에서 API를 열고 인증이 필요한 호출에는 로그인한 토큰을 입력한다.
4. `frontend/`에서 `npm ci`, `npm run build`, `npm run dev`를 실행한다. 앱은 `http://localhost:3000`에서 열린다.
5. Chromium을 처음 준비할 때 `npx playwright install chromium`을 실행한다. 백엔드와 DB를 켜고 `npx playwright test`를 실행한다. Playwright가 프론트 서버를 띄운다. 테스트 데이터는 `e2e-` 이메일로 식별한다.

## Claude Code 훅 점검

루트에서 `/hooks`를 열어 `PreToolUse`의 `Edit|Write`를 확인한다. 루트의 `.claude/hooks/protect-env.cjs`는 파일 경로의 마지막 이름을 검사하므로 `backend/.env`에도 적용된다. 개인 설정은 `.claude/settings.local.json`에 두고, 팀 설정은 `.claude/settings.json`에서 관리한다.
