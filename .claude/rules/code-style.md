# 자바 코드 스타일

모든 자바 코드에 항상 적용해요.

## 패키지 위치
- 도메인 코드는 `domain/{도메인}/` 아래 `controller`, `dto`, `entity`, `repository`에 둬요.
- 여러 도메인이 같이 쓰는 코드는 `global/` 아래에 둬요.
- 새 패키지를 임의로 만들지 않아요. 필요하면 먼저 물어봐요.

## 이름 짓기
| 종류 | 형식 | 예시 |
|---|---|---|
| 엔티티 | 도메인 이름 그대로 | `Book` |
| 요청 DTO | `XxxRequest` | `BookRequest` |
| 응답 DTO | `XxxResponse` | `BookResponse` |
| 레포지토리 | `XxxRepository` | `BookRepository` |
| 컨트롤러 | `XxxController` | `BookController` |
| 에러 코드 상수 | `도메인_상황` | `BOOK_NOT_FOUND` |

## 엔티티
- `BaseEntity`를 상속해요. (`createdAt`, `updatedAt` 자동 관리)
- `@Id`에는 `@GeneratedValue(strategy = GenerationType.IDENTITY)`를 써요.
- `@Setter`를 붙이지 않아요. 값 변경이 필요하면 의미 있는 이름의 메서드를 만들어요.
- JPA를 위해 기본 생성자(`@NoArgsConstructor`)를 둬요.

## Lombok
- `@Getter`, `@RequiredArgsConstructor`, `@NoArgsConstructor`, `@AllArgsConstructor`를 상황에 맞게 써요.
- 모든 걸 한 번에 만드는 `@Data`는 쓰지 않아요.

## 기타
- 주석은 한국어로, 코드가 "왜" 그렇게 되어 있는지 설명할 때만 써요.
- 디버그용 `System.out.println`은 남기지 않아요. 로그가 필요하면 `@Slf4j`의 `log`를 써요.
