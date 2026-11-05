# 로컬 개발 환경 점검

Java 21과 MySQL을 준비한다. 앱 실행과 테스트에는 검증 전용 로컬 스키마를 사용한다. 로컬 프로필은 Hibernate의 `ddl-auto: update` 설정으로 스키마를 갱신할 수 있다.

1. 프로젝트 루트의 Git에서 제외된 `.env` 또는 셸 환경 변수에 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`를 설정한다. URL 형식은 `jdbc:mysql://127.0.0.1:3306/<스키마>`다. `git check-ignore .env`로 제외 여부를 확인한다.
2. `./gradlew build`를 실행한다. Windows PowerShell에서는 `./gradlew.bat build`를 쓴다. DB 설정이 준비되기 전에는 `assemble`로 컴파일과 패키징을 먼저 확인할 수 있다.
3. `./gradlew bootRun`으로 앱을 띄운다. Windows에서는 `./gradlew.bat bootRun`을 쓴다. 로그에서 Hikari 연결과 애플리케이션 시작을 확인한다.
4. `http://localhost:8080/swagger-ui/index.html`에서 Swagger UI를 열고 `/v3/api-docs`가 생성되는지 확인한다. `GET /hello`로 HTTP 응답을, 검증 전용 DB에서 `POST /books` 후 반환된 ID의 `GET /books/{id}`로 DB 저장과 조회를 확인한다.
5. 실행한 앱을 종료한다. 결과를 공유할 때는 빌드, DB, Swagger를 구분하고 자격 증명은 생략한다.

연결 실패 시 MySQL 실행 상태, URL의 호스트·포트와 스키마 존재 여부, 계정 권한을 차례로 확인한다.
