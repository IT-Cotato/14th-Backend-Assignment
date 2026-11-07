# 코딩 컨벤션

## 패키지 구조

- 도메인별로 `domain/<도메인명>/` 아래에 `controller`, `dto`, `entity`, `repository`를 만든다.
- 여러 도메인이 공유하는 코드만 `global/` 아래에 둔다.
- 도메인명은 소문자 단수형을 쓴다. (`book`, `member`)

## 네이밍

| 종류 | 규칙 | 예시 |
| --- | --- | --- |
| Controller | `<도메인>Controller` | `BookController` |
| Repository | `<도메인>Repository` | `BookRepository` |
| Entity | 도메인명 그대로 | `Book` |
| 요청 DTO | `<도메인>Request` | `BookRequest` |
| 응답 DTO | `<도메인>Response` | `BookResponse` |

## Entity

- 모든 엔티티는 `BaseEntity`를 상속해서 `createdAt`, `updatedAt`을 자동으로 기록한다.
- 식별자는 `Long id`를 쓰고 `@GeneratedValue(strategy = GenerationType.IDENTITY)`를 붙인다.

## DTO

- 요청 DTO는 `@Getter`와 `@NoArgsConstructor`를 붙인다.
  `@RequestBody`로 JSON을 변환할 때 기본 생성자가 필요하다.
- Lombok은 import만으로는 동작하지 않는다. 클래스 위에 어노테이션을 반드시 붙인다.

## Controller

- 응답은 `ResponseUtils`를 통해 `ApiResponse`로 감싸서 반환한다.
- `@PathVariable`에는 파라미터 이름을 명시한다. (`@PathVariable("id") Long id`)
  이름을 생략하면 빌드 환경에 따라 인식되지 않는 경우가 있다.
- Swagger 문서화를 위해 `@Tag`, `@Operation`, `@Parameter`, `@ApiResponse`를 붙인다.

## 예외

- 예외 처리 방법은 `docs/error-handling.md`를 참고한다.