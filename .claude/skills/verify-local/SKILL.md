---
name: verify-local
description: "백엔드 빌드, 로컬 MySQL과 Swagger, 프론트 빌드 및 E2E를 점검한다. 환경이나 API를 바꾼 후 제출 전에 사용한다."
---

1. 루트의 [로컬 점검 문서](../../../docs/local-verification.md)를 읽는다.
2. 루트에서 `git check-ignore backend/.env`로 설정 파일 제외 여부를 확인한다.
3. `backend/`에서 Gradle Wrapper로 빌드한 뒤 로컬 MySQL로 앱을 실행하고 Swagger를 확인한다.
4. `frontend/`에서 프론트 빌드와 Playwright를 실행한다.
5. 이번 점검에서 시작한 프로세스를 종료하고 각 명령의 결과를 기록한다. 비밀값은 출력하지 않는다.
