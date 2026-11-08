
---
paths:
- "backend/src/main/java/**/controller/**"
---

# Controller 작성 규칙

> 적용 범위: backend/src/main/java/ 아래의 controller 패키지
> 목적: 코테이토 5·6주차 스터디 매칭 플랫폼의
> API 응답, 예외 처리, 계층 구조를 일관되게 유지해요.

## 1. Controller 위치와 구성

- Controller는 각 도메인의 controller 패키지에 작성해요.
- 기존 패키지 구조와 네이밍 규칙을 유지해요.
- 모든 신규 API 경로는 `/api`로 시작해요.
- `@RestController`를 사용해요.
- Swagger `@Tag`, `@Operation`을 작성해요.
- 필요한 경로 변수에는 `@Parameter`를 사용해요.
- 의존성은 생성자 주입으로 받아요.
- 새 Controller에는 `@RequiredArgsConstructor`를 사용해요.
- 필드 주입(`@Autowired` 필드)은 사용하지 않아요.

## 2. Service 계층 사용

5·6주차 신규 기능은 다음 구조를 따라요.

Controller → Service → Repository

- Controller는 HTTP 요청과 응답을 담당해요.
- Service는 비즈니스 로직을 담당해요.
- Repository는 데이터베이스 접근을 담당해요.
- Controller에서 Repository를 직접 호출하지 않아요.
- Controller에 비즈니스 로직을 작성하지 않아요.
- 트랜잭션은 필요한 Service 메서드에서 관리해요.
- 기존 1~4주차 Controller는 요청 없이 변경하지 않아요.

## 3. 성공 응답

기존 3주차의 공통 응답 형식을 유지해요.

반환 타입:
`ResponseEntity<ApiResponse<T>>`

기존 ResponseUtils를 사용해요.

- 조회: `ResponseUtils.ok(data)`
- 생성: `ResponseUtils.created(data)`

성공 응답 구조:
- success
- status
- data
- timestamp

- 응답에는 Entity가 아니라 DTO를 사용해요.
- ApiResponse를 Controller에서 직접 생성하지 않아요.
- 본문이 없는 204 응답은 공통 응답으로 감싸지 않아요.
- 204 응답은 `ResponseEntity<Void>` 등을 사용해요.
- 기존 ResponseUtils는 필요한 경우에만 확장해요.

## 4. 예외 처리

기존 예외 처리 방식을 유지해요.

`throw new Backend14thException(ErrorCode.XXX)`

- Controller에서 ErrorResponse를 직접 생성하지 않아요.
- Controller에서 예외를 임의로 처리하지 않아요.
- 예외는 GlobalExceptionHandler에서 공통으로 처리해요.
- 필요한 ErrorCode는 기존 enum에 추가해요.
- 에러 코드는 도메인별 prefix와 번호를 사용해요.

예시:
- COMMON-003
- USER-001
- STUDY-001
- COMMENT-001

실패 응답에는 다음 필드가 포함되어야 해요.

- success
- status
- code
- message
- method
- path
- timestamp

기존 ErrorCode 메시지의 `~습니다` 스타일을 유지해요.

## 5. 입력값 검증

- 요청 DTO에 Jakarta Validation을 사용해요.
- 검증 대상 요청에는 `@Valid`를 사용해요.
- 필수 입력값과 길이 제한을 API 명세대로 검증해요.
- 검증 실패 시 HTTP 400을 반환해요.
- GlobalExceptionHandler에서 검증 예외를 처리해요.
- 잘못된 요청을 500으로 반환하지 않도록 해요.

## 6. 인증과 권한

Spring Security와 JWT를 사용해요.

공개 API:
- 회원가입 및 로그인
- 모집글 목록 및 상세 조회
- 댓글 목록 조회

로그인 필요 API:
- 로그아웃
- 내 정보 조회
- 모집글 작성
- 댓글 작성

작성자 권한 필요 API:
- 모집글 수정 및 삭제
- 댓글 수정 및 삭제

- 인증되지 않은 사용자의 보호 API 접근을 차단해요.
- 다른 사용자의 게시글과 댓글을 수정하거나 삭제할 수 없어요.
- 작성자 검증은 서버에서도 수행해요.
- 권한 검증을 프론트엔드에만 의존하지 않아요.
- 모집이 마감된 글에는 댓글을 작성할 수 없어요.

## 7. Swagger 문서화

- 클래스에는 `@Tag`를 사용해요.
- 메서드에는 `@Operation`을 사용해요.
- 경로 변수에는 필요한 경우 `@Parameter`를 사용해요.
- API 설명은 한국어 `~해요` 스타일을 사용해요.
- 공통 ApiResponse와 Swagger ApiResponse의
  import 충돌을 주의해요.
- API 변경 사항을 docs/api-spec.md에도 반영해요.

## 8. 기존 코드 보호

- 기존 BookController를 요청 없이 수정하지 않아요.
- 기존 HelloController를 요청 없이 수정하지 않아요.
- 기존 API 응답 구조를 무단으로 변경하지 않아요.
- 신규 기능은 기존 프로젝트 구조와 일관되게 작성해요.
- Service, DTO, Entity, Repository 수정이 필요하다면
  관련 변경 범위를 함께 확인해요.

## 9. 작성 후 점검

- [ ] 모든 신규 API가 `/api`로 시작하나요?
- [ ] Controller에서 Service를 호출하나요?
- [ ] 성공 응답에 ApiResponse를 사용하나요?
- [ ] 본문 없는 204 응답을 올바르게 반환하나요?
- [ ] Entity 대신 응답 DTO를 사용하나요?
- [ ] 예외를 GlobalExceptionHandler에서 처리하나요?
- [ ] 입력값 검증 실패 시 400을 반환하나요?
- [ ] JWT 인증과 작성자 권한을 검증하나요?
- [ ] Swagger 어노테이션을 작성했나요?
- [ ] 기존 코드를 불필요하게 변경하지 않았나요?
