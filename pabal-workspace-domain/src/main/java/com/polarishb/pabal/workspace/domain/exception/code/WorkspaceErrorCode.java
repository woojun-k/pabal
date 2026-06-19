package com.polarishb.pabal.workspace.domain.exception.code;

import com.polarishb.pabal.common.exception.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WorkspaceErrorCode implements ErrorCode {

    WORKSPACE_NOT_FOUND("WSP404001", "workspace를 찾을 수 없습니다", 404);

    private final String code;
    private final String message;
    private final int httpStatus;
}
