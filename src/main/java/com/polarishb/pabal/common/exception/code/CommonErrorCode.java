package com.polarishb.pabal.common.exception.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode{
    INTERNAL_SERVER_ERROR("CMN001", "내부 서버 오류가 발생했습니다", 500),
    INVALID_INPUT("CMN002", "잘못된 입력입니다", 400),
    UNAUTHORIZED("CMN003", "인증이 필요합니다", 401),
    FORBIDDEN("CMN004", "권한이 없습니다", 403),
    NOT_FOUND("CMN005", "리소스를 찾을 수 없습니다", 404);

    private final String code;
    private final String message;
    private final int httpStatus;
}
