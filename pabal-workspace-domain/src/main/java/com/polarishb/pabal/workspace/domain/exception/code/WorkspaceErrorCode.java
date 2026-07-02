package com.polarishb.pabal.workspace.domain.exception.code;

import com.polarishb.pabal.common.exception.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WorkspaceErrorCode implements ErrorCode {

    WORKSPACE_MEMBER_LEAVE_NOT_ALLOWED("WSP409001", "workspace member가 탈퇴할 수 없습니다", 409),
    WORKSPACE_MEMBER_ROLE_CHANGE_NOT_ALLOWED("WSP409002", "workspace member role을 변경할 수 없습니다", 409),
    WORKSPACE_NOT_FOUND("WSP404001", "workspace를 찾을 수 없습니다", 404);

    private final String code;
    private final String message;
    private final int httpStatus;
}
