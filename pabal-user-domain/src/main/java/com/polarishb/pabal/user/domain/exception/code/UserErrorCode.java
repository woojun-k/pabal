package com.polarishb.pabal.user.domain.exception.code;

import com.polarishb.pabal.common.exception.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    USER_NOT_FOUND("USR404001", "사용자를 찾을 수 없습니다", 404),
    DUPLICATE_USER("USR409001", "이미 등록된 사용자입니다", 409),
    USER_ALREADY_DISABLED("USR409002", "이미 비활성화된 사용자입니다", 409);

    private final String code;
    private final String message;
    private final int httpStatus;
}
