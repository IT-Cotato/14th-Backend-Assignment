---
name: new-domain
description: 새 도메인(기능 단위)의 entity, repository, dto, controller 뼈대를 Book 도메인 코드 패턴대로 만든다. "OO 도메인 추가해줘", "새 엔티티와 API 뼈대 만들어줘"처럼 새 도메인을 시작할 때 사용한다.
---

# new-domain

## 절차

1. 도메인 이름(영문 소문자 단수형), 엔티티 필드, 필요한 엔드포인트를 사용자에게 확인한다. 모르면 추측하지 말고 묻는다
2. `docs/architecture.md`와 `docs/conventions.md`를 읽고, 같은 이름의 도메인이 이미 있는지 확인한다
3. `domain/<name>/` 아래에 `entity`, `repository`, `dto`(Request, Response), `controller`를 만든다. 파일별 형식은 `references/templates.md`를 따른다
4. 없는 데이터를 다루는 엔드포인트가 있으면 `global/exception/ErrorCode.java`에 `<NAME>_NOT_FOUND`를 추가한다
5. `./gradlew build`로 컴파일되는지 확인하고, 만든 파일 목록을 사용자에게 알린다

## 하지 말 것

- 요청받지 않은 엔드포인트와 필드를 만들지 않는다
- Service 같은 새 계층을 만들지 않는다. Controller가 Repository를 직접 호출한다
- 엔티티를 Controller 응답으로 직접 반환하지 않는다
- 기존 도메인(`book` 등)의 파일을 수정하지 않는다
