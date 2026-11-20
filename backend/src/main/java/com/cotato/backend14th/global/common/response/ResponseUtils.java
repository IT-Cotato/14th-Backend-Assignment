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

    // 본문이 없는 204 응답은 ApiResponse로 감싸지 않고 그대로 내려줘요
    public ResponseEntity<Void> noContent() {
        return ResponseEntity.noContent().build();
    }
}