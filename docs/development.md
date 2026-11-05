# 개발 환경과 검증

## 실행 준비

- JDK 21과 로컬 MySQL이 필요하다. `java -version`과 `./gradlew --version`으로 JVM을 확인한다.
- Gradle은 저장소의 Wrapper를 사용한다.
- 로컬 `.env`에 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`를 직접 설정한다. 값은 공유하거나 로그에 출력하지 않는다.
- `application.yml`은 `.env`를 선택적으로 불러오며 기본 프로필은 `local`이다.
- DB URL 형식은 `jdbc:mysql://localhost:3306/<DB명>`이다. 연결할 DB는 미리 준비한다.
- `ddl-auto: update`이므로 실행 시 스키마가 바뀔 수 있다. 개인 개발 DB를 사용한다.

```sh
./gradlew test bootJar
./gradlew bootRun
```

Windows PowerShell 예시:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
.\gradlew.bat test bootJar
.\gradlew.bat bootRun
```

## 실행 후 확인

1. 시작 로그에 애플리케이션 시작과 DB 연결 성공이 나오는지 확인한다.
2. `http://localhost:8080/hello` 응답을 확인한다.
3. `http://localhost:8080/swagger-ui/index.html`과 `/v3/api-docs`를 확인한다.
4. Swagger에서 책을 등록하고 반환된 ID로 조회한다. 존재하지 않는 ID는 404와 `BOOK-001`을 확인한다.

화면이 열리는 것과 API 명세가 정상 생성되는 것은 별개이므로 두 URL 모두 확인한다. 빌드·실행 실패 시 버전, 의존성, DB 연결 중 어느 단계에서 실패했는지 기록하고 성공으로 표시하지 않는다.

## 하네스

Python 3의 표준 라이브러리만 사용한다. `python --version`으로 실행 가능 여부를 확인한다.
Claude Code를 저장소 루트에서 시작하고 `/hooks`에서 `PreToolUse`의 `Read|Edit|Write` 연결을 확인한다. 설정을 바꿨다면 세션을 다시 시작한다.

훅은 `.env` 및 `.env.*` 파일의 Read/Edit/Write 접근을 종료 코드 2로 차단한다. 경로 구분자는 Windows와 Unix 형식을 모두 처리한다. 일반 소스 파일은 통과한다. `.env.example`도 차단 대상이며, 예시 설정은 이 문서에 키 이름과 형식만 적는다.

```sh
python -m unittest discover -s .claude/hooks/tests
```

이 훅은 Bash, 외부 프로그램, 심볼릭 링크를 통한 접근까지 검사하는 보안 경계가 아니다. Read 권한 거부도 함께 두었으며 셸에서 비밀 파일을 열지 않는 작업 원칙을 유지한다. 실제 도구 호출 차단 여부와 `/hooks` 등록은 Claude Code에서 별도로 확인한다.
