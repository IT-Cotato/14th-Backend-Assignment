# 백엔드 컨벤션

- `backend/src/main/java/com/cotato/backend14th/domain/`에서 도메인별로 컨트롤러, 서비스, DTO, 엔티티, 리포지토리를 둔다. 모집글과 댓글의 배치는 `domain/study/`를 참고한다.
- 공통 응답과 엔티티는 `global/common/`, 예외 처리는 `global/exception/`, 인증은 `global/security/`에 둔다.
- 단위 테스트는 `backend/src/test/java/`에서 대상 코드의 패키지를 따라 배치한다.
- 새 API를 `/api`에 두고 성공 응답은 `ResponseUtils`로 감싼다. 삭제와 로그아웃의 204는 본문을 비운다.
- 엔티티에서 필요한 필드만 응답 DTO에 담는다. 사용자 응답에는 비밀번호를 포함하지 않는다.
- `ErrorCode`와 `Backend14thException`으로 도메인 오류를 표현한다. 인증 필터와 컨트롤러의 실패 응답은 공통 `ErrorResponse`를 사용한다.
- 읽기 API는 공개한다. 쓰기 API는 Bearer JWT를 검증하고, 수정·삭제는 서비스에서 작성자를 확인한다.
