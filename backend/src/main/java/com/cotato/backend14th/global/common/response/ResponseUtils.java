// ResponseUtils.java
package com.cotato.backend14th.global.common.response;

import org.springframework.http.ResponseEntity;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ResponseUtils {

    public <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    public <T> ResponseEntity<ApiResponse<T>> created(T data) {
        return ResponseEntity
                .status(201)
                .body(ApiResponse.created(data));
    }

    // 204: 본문 없이 내려줘요 (ApiResponse로 감싸지 않음)
    public ResponseEntity<Void> noContent() {
        return ResponseEntity.noContent().build();
    }
}
