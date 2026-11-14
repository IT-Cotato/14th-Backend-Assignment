# 코테이토 스터디 매칭 플랫폼 — 프론트엔드

Vite + React + TypeScript + Tailwind CSS + React Router + axios로 만든 SPA입니다.
요구사항은 [docs/api-spec.md](../docs/api-spec.md) 9장을 따릅니다.

## 실행

```powershell
npm install
npm run dev      # http://localhost:3000 (포트 고정)
npm run build    # 타입 검사 + 빌드
npm run lint     # oxlint
```

## 환경변수

| 이름 | 설명 | 기본값 |
|---|---|---|
| `VITE_API_BASE_URL` | 백엔드 주소 | `http://localhost:8081` |

기본값과 다른 주소를 쓰려면 셸에서 환경변수를 지정해 실행합니다.

## 구조

```text
src/
├─ api/          axios 인스턴스(client), API 함수(auth), 응답 타입(types), 에러 메시지(errors)
├─ auth/         토큰 저장(localStorage), AuthProvider, useAuth, RequireAuth
├─ components/   Layout, Header, FormField, ErrorAlert
├─ pages/        SignupPage, LoginPage, MyPage, StudiesPage
└─ validation/   회원가입 입력 검증
```

## 인증 흐름

- 로그인하면 accessToken을 localStorage에 저장하고, 요청마다 `Authorization: Bearer` 헤더를 붙입니다.
- 401이 오면 토큰을 지우고 `/login`으로 보냅니다. 로그인과 회원가입 요청의 401은 화면에 에러 메시지로 보여줍니다.
- 로그아웃은 `POST /api/auth/logout`을 호출한 뒤, 성공 여부와 관계없이 토큰을 지우고 `/login`으로 이동합니다.
