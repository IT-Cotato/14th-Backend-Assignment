# 협업과 제출

- 개인 누적 브랜치는 `seonjae/basic`, 5·6주차 작업 브랜치는 `seonjae/monorepo` 하나다. 기능별 브랜치는 만들지 않는다.
- PR의 base는 `seonjae/basic`이다. `develop`으로 제출하지 않는다. PR은 모든 작업을 마친 뒤 한 번만 올린다.
- 중간 백업은 `git push origin seonjae/monorepo`로만 한다.
- 작업 순서와 커밋: 0 프로젝트 세팅 → 1 인증 → 2 모집글 → 3 댓글 → 4 E2E. 단계마다 커밋을 나눈다.
- 프로덕션 코드와 테스트 코드(단위 테스트, E2E)는 커밋을 분리한다.
- 커밋은 `feat: 요약`, `test: 요약`, `docs: 요약`, `chore: 요약`, `refactor: 요약`처럼 변경 성격에 맞는 타입을 붙인다.
- 이슈 제목은 `feat: [윤선재] 코테이토 스터디 매칭 플랫폼`, PR 제목은 `feat: [윤선재] 코테이토 스터디 매칭 플랫폼 (closes #이슈번호)` 형식이다.
- 기능 문서는 `docs/features/이슈번호-기능명.md` 1개에 인증, 모집글, 댓글 순으로 정리한다.
- stage 전에 변경 목록을 확인한다. `.env`, `node_modules/`, `dist/`, `playwright-report/`, `test-results/`, 빌드 결과, 개인 설정은 포함하지 않는다.
- 테스트와 실제 실행 결과를 구분하고, 실행하지 않은 검증을 완료했다고 적지 않는다.
- PR 병합은 파트장이 진행한다.
