---
name: check-assignment
description: 과제 PR을 제출하기 전에 브랜치, 변경 파일, 필수 산출물과 검증 결과를 점검한다. 제출 점검을 요청하거나 PR 본문을 작성할 때 사용한다.
---

# 과제 제출 점검

1. 연결할 이슈와 과제 안내를 읽고 이번 제출의 요구사항을 정리한다. 주차별 브랜치와 산출물을 이전 과제에서 그대로 추정하지 않는다.
2. `git status --short`, `git branch --show-current`, `git diff <base>...HEAD`, 커밋 전 변경분을 확인한다. base가 개인 누적 브랜치(`seonjae/basic`)인지 확인한다.
3. 모노레포 과제(5·6주차)에서는 아래를 확인한다.
   - `backend/`, `frontend/` 구조, `backend/.env.example` 존재, 하네스 경로가 `backend/`, `frontend/` 기준인지
   - `docs/api-spec.md`의 13개 API와 실제 Controller의 URL·메서드가 같은지
   - `docs/erd/`에 이미지와 ERDCloud 링크가 있고 엔티티와 컬럼이 같은지
   - `docs/features/이슈번호-기능명.md` 1개
   - 프론트 6개 화면 경로, `vite.config.ts`의 3000 포트
   - `frontend/e2e/`에 인증·모집글·댓글의 정상/에러 시나리오
4. stage 예정 파일에 비밀 파일(`.env`), `node_modules/`, `dist/`, `playwright-report/`, `test-results/`, 개인 설정이 없는지 파일명으로 확인한다. 비밀 값을 읽어서 검사하지 않는다.
5. `cd backend && ./gradlew test`, `cd frontend && npm run build && npm run lint`, 훅 테스트를 실행한다. E2E는 `run-e2e` 스킬로 실행한다. DB 연결, Swagger, E2E는 실행 결과가 있을 때만 완료로 표시한다.
6. 결과를 `통과 / 미확인 / 수정 필요`로 나누고 근거를 적는다. PR 본문은 과제 안내 6-2 형식(개요, 작업 내용 Backend/Frontend, API 변경, ERD/문서 변경, E2E 결과, 테스트, 하네스 활용, 관련 이슈, 리뷰 포인트, 제출 전 체크)으로 작성한다.

이 스킬은 점검과 본문 작성까지 수행한다. push와 PR 생성은 사용자가 요청한 범위에서 진행하고, 병합은 하지 않는다.
