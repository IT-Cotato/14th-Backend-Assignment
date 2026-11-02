---
name: pre-pr-check
description: PR을 만들기 전에 브랜치, 변경 파일, 민감 정보, 빌드 결과를 점검할 때 사용합니다.
---

# PR 전 점검

다음 항목을 확인하고 결과를 표로 보고합니다. 문제가 있어도 사용자 승인 없이 고치지 않습니다.

1. **브랜치:** `git branch --show-current`가 `minho/{작업명}` 형식인지 확인합니다.
2. **기준 브랜치:** `git log --oneline minho/basic..HEAD`에 이번 작업의 커밋만 있는지 확인합니다.
3. **변경 파일:** `git diff --stat minho/basic...HEAD`와 `git status --short`에 의도하지 않은 파일이 없는지 확인합니다.
4. **민감 정보:**
   - `git check-ignore .env`로 `.env`가 제외되는지 확인합니다.
   - `git diff minho/basic...HEAD`에 비밀번호나 토큰처럼 보이는 값이 없는지 확인합니다. 발견하면 값은 출력하지 않고 위치만 알립니다.
5. **커밋 메시지:** `feat:`, `fix:`, `chore:`, `docs:` 중 하나로 시작하는지 확인합니다.
6. **빌드:** 테스트가 로컬 DB에 영향을 줄 수 있음을 사용자에게 알린 뒤 `.\gradlew.bat build`를 실행합니다.
7. **PR 안내:** base가 `minho/basic`인지 확인하고 `.github/PULL_REQUEST_TEMPLATE.md` 형식의 초안을 보여 줍니다.

PR 생성과 푸시는 사용자가 요청할 때만 합니다.
