---
name: submit-pr
description: 주간 과제 작업을 커밋하고, 이슈와 연결한 뒤 daeun/monorepo에서 daeun/basic으로 PR을 올릴 때 사용한다.
---

# 주간 과제 PR 올리기

## 절차
1. 프로젝트 루트인지 확인한다. `.git` 폴더 안에 있으면 `cd ..` 한다.
2. 현재 브랜치가 `daeun/monorepo`인지 확인한다.
3. `git add` 후 `git status`로 올릴 파일을 확인한다.
4. `git commit -m "feat: [한다은] 코테이토 스터디 매칭 플랫폼"` 으로 커밋한다.
5. `git push -u origin daeun/monorepo` 로 푸시한다.
6. GitHub 이슈가 없으면 레포 템플릿으로 이슈를 만든다.
7. PR은 **base `daeun/basic`**, **compare `daeun/monorepo`** 로 만든다. `develop`으로 올리지 않는다.
8. PR 본문 형식은 `references/pr-body.md`를 따른다.

## 하지 말 것
- `develop`에 직접 푸시하지 않는다.
- GitHub 비밀번호로 인증하지 않는다.
- `.git` 폴더 안에서 commit/push 하지 않는다.
- 레포 PR 템플릿을 그대로 두지 않는다. 본문을 바꿔 쓴다.