package com.polarishb.pabal.common.api;

import com.polarishb.pabal.common.exception.GlobalException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GlobalException.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(GlobalException e) {
        log.error("GlobalException: code={}, message={}",
                e.getErrorCode().getCode(),
                e.getMessage(),
                e);

        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(new ErrorResponse(
                        e.getErrorCode().getCode(),
                        e.getErrorCode().getMessage(),
                        e.getPayload()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unexpected exception", e);

        return ResponseEntity
                .status(500)
                .body(new ErrorResponse(
                        "CMN500001",
                        "내부 서버 오류가 발생했습니다"
                ));
    }
}
