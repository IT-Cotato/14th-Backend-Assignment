# Git 규칙

## 브랜치

- 4주차: 누적 브랜치 `이름/basic`, 작업 브랜치 `이름/harness`. 이름은 영문 소문자 (예: `hyunvin/basic`)
- 이전 주차 방식: `feat/이슈번호-설명` (예: `feat/38-swagger`)
- PR은 `이름/harness` → `이름/basic`으로 올린다. base를 `develop`으로 두지 않는다

## 커밋

- 형식: `타입: 설명` (예: `feat: .env 및 프로필 설정, Book Swagger 문서화`)
- 타입: `feat`, `fix`, `chore`, `docs` 등 작업 성격에 맞게
- 커밋 전에 `git status`로 `.env` 같은 민감한 파일이 포함되지 않았는지 확인

## 이슈

- 제목: `타입: [이름] 설명` (예: `chore: [박현빈] 4주차 하네스 구축`)
- 레포의 이슈 템플릿(Description, To-do, 기타)을 그대로 사용

## PR

- 제목: `타입: [이름] 설명 (closes #이슈번호)`
- 본문: 개요 / 작업 내용 / 관련 이슈(`closes #N`) / 리뷰 포인트
- 올리기 전에 base 브랜치가 맞는지 확인
