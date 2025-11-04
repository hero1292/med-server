package org.aleksanyan.medserver.exception;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {
    private final ErrorCode code;

    public ApiException(ErrorCode code) {
        super(code.getMessage());
        this.code = code;
    }
}
