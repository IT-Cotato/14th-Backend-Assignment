# 하네스 점검 기록

- 이슈: #105
- 확인일: 2026-11-05
- 환경: Windows, JDK 21, Python 3.12, 로컬 MySQL

| 항목 | 결과 |
| --- | --- |
| `gradlew.bat test bootJar` | 성공, Spring 테스트 1개 통과 |
| 애플리케이션 실행 | local 프로필로 시작, MySQL 연결 성공 |
| `GET /hello` | 200 |
| `GET /swagger-ui/index.html` | 200 |
| `GET /v3/api-docs` | 200, API 명세 반환 |
| 없는 도서 ID 조회 | 404, BOOK-001 |
| 훅 단위 테스트 | 3개 통과: 환경 파일 차단, 일반 파일 허용, 잘못된 입력 차단 |
| settings.json의 실제 명령 실행 | Git Bash에서 실행, .env는 종료 코드 2, 일반 문서는 0 |
| 스킬 형식 | frontmatter와 이름 검사 통과 |

설정 명령은 저장소 상위 폴더에서도 실행해 프로젝트 경로에 한글·공백이 있어도 스크립트를 찾는지 확인했다. Windows의 한글 출력 인코딩 차이를 피하도록 훅 실행에 `-X utf8`을 지정했다.

첫 빌드는 Maven 저장소 DNS 오류로 실패했으며 재시도에서 성공했다. 의존성 버전은 변경하지 않았다.

Claude Code CLI가 로그인되어 있지 않아 `/hooks` 화면의 등록 표시와 Claude의 실제 Read/Edit/Write 호출 차단은 아직 확인하지 못했다. 로그인 후 저장소 루트에서 새 세션을 열어 `/hooks`를 확인하고, 비밀 값이 없는 임시 `.env` 파일을 대상으로 차단 여부를 점검해야 한다. 설정 명령 실행 검증과 Claude 내부 동작 검증은 구분한다.
