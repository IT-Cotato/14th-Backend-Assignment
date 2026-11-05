# 컨벤션

## 코드

### 응답
- 컨트롤러는 `ResponseEntity<ApiResponse<T>>`를 반환한다.
- 조회/수정은 `ResponseUtils.ok(dto)`, 생성은 `ResponseUtils.created(dto)`를 쓴다.
- `ApiResponse`를 직접 `new` 하지 않는다. (생성자가 private)

### 예외
- 비즈니스 예외는 `throw new Backend14thException(ErrorCode.X)`로 던진다.
- `RuntimeException`, `IllegalArgumentException`을 직접 던지지 않는다.
- 컨트롤러에서 try-catch로 에러 응답을 만들지 않는다. `GlobalExceptionHandler`가 처리한다.

### ErrorCode
- 형식: `<DOMAIN>_<상황>(HttpStatus.X, "<DOMAIN>-<3자리 번호>", "<한글 메시지>")`
- 예: `BOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "BOOK-001", "책을 찾을 수 없습니다.")`
- 도메인별로 `// BOOK` 같은 주석 그룹을 만들고, 번호는 그룹 안에서 001부터 이어 붙인다.
- 메시지는 `~입니다.`로 끝나는 문장으로 쓴다.

### 엔티티
- 모든 엔티티는 `BaseEntity`를 상속한다. (`createdAt`, `updatedAt` 자동 관리)
- PK는 `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id`
- 엔티티를 API 응답으로 직접 반환하지 않는다. DTO로 변환한다.

### DTO
- 위치: `domain/<도메인>/dto/`
- 이름: `<도메인>Request`, `<도메인>Response` (용도가 여러 개면 `<도메인><동작>Request`)

### Swagger
- 컨트롤러 클래스에 `@Tag(name = "<도메인> API", description = "...")`
- 각 메서드에 `@Operation(summary, description)`, PathVariable에 `@Parameter(description, example)`

### 스타일
- 주석과 Swagger 설명은 한글로 쓴다.
- Lombok은 `@Getter`, `@RequiredArgsConstructor`, `@NoArgsConstructor` 위주로 쓰고 `@Setter`는 피한다.

## Git

### 브랜치
- 기능 작업: `<type>/<이슈번호>-<설명>` (예: `feat/84-response-security`)
- 과제 누적 브랜치: `seungmin/basic`, 주차 작업 브랜치: `seungmin/<작업명>` (예: `seungmin/harness`)
- PR base: 과제는 `seungmin/basic`, 그 외는 `develop`

### 커밋
- 형식: `<type>: <한글 설명>` (예: `feat: 공통 응답 포맷, CORS, Security, BaseEntity 구현`)
- type: `feat`, `fix`, `refactor`, `docs`, `chore`, `test`, `style`
- 설명은 명사형 또는 `~구현/추가/수정`으로 끝낸다. 마침표는 붙이지 않는다.

### 이슈 / PR
- 이슈 제목: `<type>: [seungmin] <작업명>`
- PR 제목: `<type>: [seungmin] <작업명> (closes #이슈번호)`
- PR 본문은 `.github/PULL_REQUEST_TEMPLATE.md`를 따른다. 과제 PR은 과제가 지정한 형식으로 바꾼다.
- PR을 올리기 전에 `docs/features/<이슈번호>-<작업명>.md`를 정리한다. (`feature-doc` 스킬)
