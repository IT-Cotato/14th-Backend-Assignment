---
paths:
  - "frontend/src/**/*.ts"
  - "frontend/src/**/*.tsx"
---

# 프론트엔드 규칙

## 기준
- 화면과 동작은 `docs/api-spec.md` 9장을 따릅니다. 필드 이름은 백엔드 응답 그대로 `src/api/types.ts`에 둡니다.
- API 호출은 `src/api/{도메인}.ts`에 두고, `apiClient`(`src/api/client.ts`)만 씁니다. 함수는 `ApiResponse` 래퍼를 벗기고 `data`만 돌려줍니다.

## 접근성 (E2E가 역할과 라벨로 요소를 찾습니다)
- 모든 입력창은 `label htmlFor` ↔ `id`로 연결합니다. `FormField` 컴포넌트를 씁니다.
- 동작은 `<button type="button|submit">`, 이동은 `<Link>`(a)로 만듭니다. 페이지 이동도 링크입니다.
- 같은 화면에 이름이 같은 버튼이 여러 개 생기면 `role="group"`과 `aria-label`로 묶어 구분합니다.
- 삭제 확인은 `window.confirm` 대신 화면 안의 확인 버튼으로 만듭니다. (Playwright는 dialog를 기본으로 닫아 버립니다)

## 상태 표시
- 서버 에러는 `getErrorMessage(error)`로 서버의 `message`를 꺼내 `ErrorAlert`(`role="alert"`)로 보여줍니다.
- 로딩 중은 `LoadingStatus`(`role="status"`)로 보여줍니다.
- 401 처리는 `apiClient` 인터셉터가 맡습니다. 화면에서 따로 `/login`으로 보내지 않습니다.
- 로그인이 필요한 화면은 `App.tsx`에서 `RequireAuth`로 감쌉니다.

## effect와 상태 (oxlint `react(set-state-in-effect)`)
- `useEffect` 안에서 `setState`를 동기적으로 호출하지 않습니다. 로딩 여부는 따로 상태로 두지 말고 계산합니다.
  - 예: 응답과 함께 요청 키를 저장하고 `isLoading = loaded?.key !== requestKey`로 판단합니다. (`StudiesPage`, `CommentSection`)
  - 예: `isLoading = data === null && error === null` (`StudyDetailPage`)
- 비동기 응답은 `cancelled` 플래그로 언마운트 뒤의 상태 변경을 막습니다.

## 스타일
- Tailwind CSS만 씁니다. 버튼, 입력 클래스는 `components/form.ts`의 상수를 재사용합니다.
- 모바일 우선으로 작성하고(`grid-cols-1` → `sm:`), 긴 제목과 내용에는 `break-words`를 붙입니다.

## 검증
- 수정 후 `frontend/`에서 `npm run build`와 `npm run lint`를 실행합니다. lint 경고도 0으로 유지합니다.
