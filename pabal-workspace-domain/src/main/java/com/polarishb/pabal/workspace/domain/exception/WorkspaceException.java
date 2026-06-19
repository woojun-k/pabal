package com.polarishb.pabal.workspace.domain.exception;

import com.polarishb.pabal.common.exception.GlobalException;
import com.polarishb.pabal.workspace.domain.exception.code.WorkspaceErrorCode;

import java.util.Map;

public class WorkspaceException extends GlobalException {

    protected WorkspaceException(WorkspaceErrorCode errorCode) {
        super(errorCode);
    }

    protected WorkspaceException(WorkspaceErrorCode errorCode, String message, Map<String, Object> payload) {
        super(errorCode, message, payload);
    }
}
