# 코딩 컨벤션

## 네이밍

- 패키지: `com.cotato.backend14th.domain.{도메인}.{레이어}` (소문자)
- 클래스: PascalCase
- DTO: `{Action}Request` / `{Action}Response`
- Repository: `{Entity}Repository`
- ErrorCode: `{Domain}ErrorCode` 또는 공통 `ErrorCode`

## Lombok

- `@Getter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder` 사용 가능
- DI는 `@RequiredArgsConstructor` + `final` 필드 (생성자 주입)
- 엔티티에 `@Setter` 금지 — 의미 있는 상태변경 메서드로 표현

## 트랜잭션

- 클래스 레벨 `@Transactional(readOnly = true)` + 쓰기 메서드만 `@Transactional` 오버라이드

## Validation

- Request DTO에 `@Valid` + Bean Validation
- 위반 시 `MethodArgumentNotValidException` → `GlobalExceptionHandler`에서 일괄 처리

## 커밋 메시지

```
타입: 요약 (한 줄, 50자 이내)
```

| 타입 | 용도 |
|---|---|
| feat | 새 기능 추가 |
| fix | 버그 수정 |
| refactor | 리팩토링 (기능 변화 없음) |
| docs | 문서 수정 |
| chore | 빌드, 설정, 기타 잡일 |
| test | 테스트 추가/수정 |

## 브랜치

- `develop`: 기본 브랜치
- `이름/basic`: 개인 누적 브랜치
- `이름/작업명`: 작업 브랜치 → PR은 `이름/basic`으로
