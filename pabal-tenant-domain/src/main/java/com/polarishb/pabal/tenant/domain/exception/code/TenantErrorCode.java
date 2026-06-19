package com.polarishb.pabal.tenant.domain.exception.code;

import com.polarishb.pabal.common.exception.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TenantErrorCode implements ErrorCode {

    TENANT_NOT_FOUND("TNT404001", "tenant를 찾을 수 없습니다", 404);

    private final String code;
    private final String message;
    private final int httpStatus;
}
