package com.polarishb.pabal.tenant.domain.exception;

import com.polarishb.pabal.common.exception.GlobalException;
import com.polarishb.pabal.tenant.domain.exception.code.TenantErrorCode;

import java.util.Map;

public class TenantException extends GlobalException {

    protected TenantException(TenantErrorCode errorCode) {
        super(errorCode);
    }

    protected TenantException(TenantErrorCode errorCode, String message, Map<String, Object> payload) {
        super(errorCode, message, payload);
    }
}
