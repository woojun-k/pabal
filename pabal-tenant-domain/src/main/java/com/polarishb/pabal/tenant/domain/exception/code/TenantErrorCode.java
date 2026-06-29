package com.polarishb.pabal.tenant.domain.exception.code;

import com.polarishb.pabal.common.exception.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TenantErrorCode implements ErrorCode {

    TENANT_NOT_FOUND("TNT404001", "tenant를 찾을 수 없습니다", 404),
    TENANT_REGISTRATION_NOT_FOUND("TNT404002", "tenant 등록 요청을 찾을 수 없습니다", 404),
    TENANT_REGISTRATION_EXPIRED("TNT410001", "tenant 등록 요청이 만료되었습니다", 410),
    TENANT_DOMAIN_ALREADY_REGISTERED("TNT409001", "이미 등록 진행 중인 domain입니다", 409),
    TENANT_DOMAIN_VERIFICATION_FAILED("TNT409002", "domain 소유권 검증에 실패했습니다", 409),
    TENANT_REGISTRATION_NOT_PENDING("TNT409003", "domain 검증을 진행할 수 없는 tenant 등록 요청입니다", 409);

    private final String code;
    private final String message;
    private final int httpStatus;
}
