# 개발 환경과 검증

## 준비

- JDK 21, Node.js 20 이상, 로컬 MySQL 8, Python 3(하네스 훅)
- Gradle은 `backend/`의 Wrapper를 사용한다.
- `backend/.env`에 `backend/.env.example`의 키(`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`)를 채운다. 값은 공유하거나 로그에 출력하지 않는다.
- `application.yml`은 실행 위치의 `.env`를 선택적으로 불러온다. 그래서 서버는 반드시 `backend/`에서 실행한다.
- `JWT_SECRET`은 32바이트 이상이어야 한다. 짧으면 서버가 시작하지 않는다.
- `ddl-auto: update`이므로 실행 시 스키마가 바뀔 수 있다. 개인 개발 DB를 사용한다.

## 백엔드

```sh
cd backend
./gradlew test       # 단위 테스트. H2 메모리 DB를 쓰므로 MySQL이 없어도 된다
./gradlew bootRun    # http://localhost:8080
```

Windows PowerShell 예시:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
cd backend
.\gradlew.bat test
.\gradlew.bat bootRun
```

실행 후 확인:

1. 시작 로그에 DB 연결 성공이 나오는지 확인한다.
2. `http://localhost:8080/swagger-ui/index.html`과 `/v3/api-docs`를 확인한다.
3. Swagger에서 회원가입 → 로그인 → Authorize에 토큰 입력 → 모집글·댓글 API를 호출한다.
4. 로그아웃 후 같은 토큰으로 `/api/users/me`를 호출해 401(`COMMON-004`)을 확인한다.

## 프론트엔드

```sh
cd frontend
npm install
npm run dev      # http://localhost:3000 (strictPort)
npm run build
npm run lint
```

API 주소는 기본 `http://localhost:8080`이다. 바꾸려면 `frontend/.env.local`에 `VITE_API_BASE_URL`을 둔다.

## E2E (Playwright)

```sh
cd frontend
npx playwright install chromium   # 처음 한 번
npx playwright test               # 백엔드와 DB가 켜져 있어야 한다. 프론트 서버는 자동으로 뜬다
npx playwright show-report        # 실패 시 스크린샷과 trace 확인
```

- 테스트 계정 이메일은 `e2e-`로 시작한다. 개발 DB에 쌓이므로 직접 만든 데이터와 구분된다.
- 비밀번호는 실행할 때마다 무작위로 만든다. 고정하려면 `E2E_PASSWORD` 환경 변수를 쓴다.
- 백엔드 주소가 다르면 `E2E_API_URL`로 바꾼다.

## 하네스

Python 3의 표준 라이브러리만 사용한다.

```sh
python -m unittest discover -s .claude/hooks/tests
```

`protect-env.py`는 `.env`, `.env.*` 파일의 Read/Edit/Write를 종료 코드 2로 막는다. 값이 없는 `.env.example`만 허용한다. Bash나 외부 프로그램을 통한 접근까지 막는 보안 경계는 아니므로 셸에서 비밀 파일을 열지 않는 원칙을 유지한다.
