---
paths:
  - "src/main/java/**/controller/**/*.java"
  - "src/main/java/**/HelloController.java"
---

# 컨트롤러 변경 규칙

- 새 엔드포인트는 요청 바인딩, 입력 검증, 응답 변환에 집중한다. 여러 저장소 호출이나 트랜잭션 경계가 필요하면 도메인 서비스에 둔다.
- 신규 API 응답과 도메인 오류는 `docs/backend-conventions.md`의 공통 형식을 따른다.
- Swagger 설명에 실제 HTTP 상태와 요청 형식을 반영한다. API가 바뀌면 정상 요청과 대표 실패 요청을 확인한다.
