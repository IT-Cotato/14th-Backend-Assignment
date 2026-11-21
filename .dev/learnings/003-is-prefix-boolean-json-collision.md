# 003. `isAuthor` getter 가 `author` 객체를 응답에서 지운 사례

- 날짜: 2026-11-19
- 작업: 5,6주차 스터디 모집글 상세, 댓글 목록 응답 DTO

## 무엇이 틀렸나

응답에 `author`(작성자 객체)와 `isAuthor`(boolean)가 함께 있어야 했다. AI 는 Lombok 이 만드는 `isAuthor()` 가 JSON 에서 `author` 로 읽힌다는 것까지는 알고, 아래처럼 getter 를 직접 만들었다.

```java
@Getter(AccessLevel.NONE)
private final boolean isAuthor;

@JsonProperty("isAuthor")
public boolean isAuthor() { return isAuthor; }
```

컴파일도 되고 서비스 단위 테스트도 통과했지만, 실제 JSON 에서는 `author` 객체가 통째로 사라졌다. Jackson 은 `getAuthor()` 와 `isAuthor()` 를 암묵적 이름 `author` 로 먼저 한 프로퍼티로 묶고, 그 안에 명시적 이름(`isAuthor`)이 하나 있으면 프로퍼티 전체의 이름을 바꾼다.

## 어떻게 발견했나

DB 없이 `ObjectMapper` 로 DTO 를 직렬화해 보는 `StudyPostResponseJsonTest` 를 추가했더니 `json.get("author")` 가 null 이었다. 서버를 띄우기 전에 잡혔다.

## 해결

getter 이름을 `getIsAuthor()` 로 바꿔 암묵적 이름이 `author` 와 겹치지 않게 했다.

```java
@JsonProperty("isAuthor")
public boolean getIsAuthor() { return isAuthor; }
```

## 교훈

- 서비스 단위 테스트는 JSON 필드 이름을 검증하지 못한다. 응답 DTO 의 모양은 직렬화 테스트로 따로 확인한다.
- `is` 로 시작하는 boolean 필드는 getter 이름까지 직접 정한다.
- rule 로 승격: `.claude/rules/api-layer.md` 에 두 항목을 추가했다.
