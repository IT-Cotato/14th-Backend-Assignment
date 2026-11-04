# 파일별 템플릿

`<Name>`은 파스칼 케이스 도메인 이름(예: `Applicant`), `<name>`은 소문자(예: `applicant`), `<NAME>`은 대문자(예: `APPLICANT`)다. 기준 코드는 `domain/book/`이다.

## entity/<Name>.java

```java
package com.cotato.backend14th.domain.<name>.entity;

import com.cotato.backend14th.global.common.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class <Name> extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 요청받은 필드
}
```

## repository/<Name>Repository.java

```java
package com.cotato.backend14th.domain.<name>.repository;

import com.cotato.backend14th.domain.<name>.entity.<Name>;
import org.springframework.data.jpa.repository.JpaRepository;

public interface <Name>Repository extends JpaRepository<<Name>, Long> {
}
```

## dto/<Name>Request.java

```java
package com.cotato.backend14th.domain.<name>.dto;

import lombok.Getter;

@Getter
public class <Name>Request {
    // 요청받은 필드
}
```

## dto/<Name>Response.java

```java
package com.cotato.backend14th.domain.<name>.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class <Name>Response {
    private Long id;
    // 요청받은 필드
}
```

## controller/<Name>Controller.java

```java
@Tag(name = "<Name> API", description = "<설명>")
@RestController
@RequestMapping("/<복수형>")
public class <Name>Controller {

    private final <Name>Repository <name>Repository;

    public <Name>Controller(<Name>Repository <name>Repository) {
        this.<name>Repository = <name>Repository;
    }

    @Operation(summary = "<설명> 단건 조회", description = "ID로 조회해요.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<<Name>Response>> get<Name>(
            @Parameter(description = "조회할 ID", example = "1")
            @PathVariable Long id) {
        <Name> <name> = <name>Repository.findById(id)
                .orElseThrow(() -> new Backend14thException(ErrorCode.<NAME>_NOT_FOUND));
        return ResponseUtils.ok(new <Name>Response(<name>.getId() /* , 나머지 필드 */));
    }

    @Operation(summary = "<설명> 등록", description = "새로 등록해요.")
    @PostMapping
    public ResponseEntity<ApiResponse<<Name>Response>> create<Name>(@RequestBody <Name>Request request) {
        <Name> saved = <name>Repository.save(new <Name>(null /* , request의 필드 */));
        return ResponseUtils.created(new <Name>Response(saved.getId() /* , 나머지 필드 */));
    }
}
```

## ErrorCode 추가 (global/exception/ErrorCode.java)

```java
// <NAME>
<NAME>_NOT_FOUND(HttpStatus.NOT_FOUND, "<NAME>-001", "<설명>을 찾을 수 없습니다.");
```

enum 상수 목록은 `;`로 끝난다. 기존 마지막 상수 줄 끝의 `;`를 `,`로 바꾸고, 새 상수를 그 아래에 추가해서 새 상수가 `;`로 끝나게 한다.
