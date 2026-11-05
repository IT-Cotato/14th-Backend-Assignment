# Git 컨벤션

브랜치, 커밋, 이슈, PR 규칙을 정리한 문서예요.

## 브랜치

### 1~3주차 (develop 기준)
| 브랜치 | 용도 |
|---|---|
| `main` | 최종 배포용. 직접 push 금지 |
| `develop` | 개발 통합 브랜치. 직접 push 금지 |
| `feat/이슈번호-기능명` | 기능 작업 (예: `feat/80-response-security`) |
| `fix/이슈번호-버그명` | 버그 수정 (예: `fix/12-login-error`) |

### 4주차부터 (내 누적 브랜치 기준)
```
develop
  └─ dahye/basic          # 내 누적 브랜치. PR이 머지되는 곳
       └─ dahye/harness   # 4주차 작업 브랜치
       └─ dahye/monorepo  # 5~6주차 작업 브랜치
```
- 작업 브랜치는 항상 `dahye/basic`에서 만들어요.
- PR의 base는 `develop`이 아니라 `dahye/basic`이에요. GitHub 기본값이 `develop`이라 꼭 바꿔야 해요.
- 머지 후에는 `git checkout dahye/basic` → `git pull origin dahye/basic`으로 최신 상태를 받아요.

## 커밋 메시지

형식: `타입: 작업 내용 요약`

| 타입 | 의미 |
|---|---|
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `docs` | 문서 수정 |
| `style` | 코드 포맷 등 기능 변경 없음 |
| `refactor` | 기능 변경 없이 코드 구조 개선 |
| `chore` | 빌드 설정, 의존성 추가 등 기타 작업 |

- 좋은 예: `feat: 책 삭제 API 추가`, `chore: .gitignore에 .env 추가`
- 나쁜 예: `수정함`, `fix`, `asdf`
- 의미 있는 단위로 나눠서 커밋해요.

## 이슈

- 레포에 등록된 이슈 템플릿을 그대로 써요. (Description / To-do / 기타)
- 제목 예: `chore: [다혜] 4주차 하네스 구축`
- 생성된 이슈 번호를 브랜치 이름과 PR에 사용해요.

## PR

- 제목 형식: `타입: [이름] 작업 요약 (closes #이슈번호)`
  - 예: `chore: [다혜] 4주차 하네스 구축 (closes #90)`
- 본문은 레포 PR 템플릿(개요 / 작업 내용 / 관련 이슈 / 참고 자료 / 리뷰 포인트)을 채워요.
- `closes #이슈번호`를 넣으면 머지될 때 이슈가 자동으로 닫혀요.
- 머지는 파트장이 해요.

## 기능 문서 (5주차부터)

- PR마다 `docs/features/이슈번호-작업명.md` 문서를 1개 작성해요. (예: `docs/features/7-example-feature.md`)
- 무엇을, 왜, 어떻게 만들었는지 적어요. PR이 닫힌 뒤에도 남는 기록이에요.

## 올리기 전 확인

- `git status`로 `.env` 같은 민감한 파일이 포함되지 않았는지 확인해요.
- 지금 브랜치가 맞는지 확인해요. (`git branch`에서 `*` 표시)
