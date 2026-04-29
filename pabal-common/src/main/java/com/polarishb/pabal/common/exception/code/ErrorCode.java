package com.polarishb.pabal.common.exception.code;

public interface ErrorCode {
    String getCode();
    String getMessage();
    int getHttpStatus();
}
