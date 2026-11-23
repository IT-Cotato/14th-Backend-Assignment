// Backend14thException.java
package com.cotato.backend14th.global.exception;

import lombok.Getter;

@Getter
public class Backend14thException extends RuntimeException {

    private final ErrorCode errorCode;

    public Backend14thException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public Backend14thException(ErrorCode errorCode, String customMessage) {
        super(customMessage != null ? customMessage : errorCode.getMessage());
        this.errorCode = errorCode;
    }
}