package com.polarishb.pabal.messenger.domain.exception.code;

import com.polarishb.pabal.common.exception.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MessengerErrorCode implements ErrorCode {
    // 404 Not Found
    CHAT_ROOM_NOT_FOUND("MSG404001", "채팅방을 찾을 수 없습니다", 404),
    MESSAGE_NOT_FOUND("MSG404002", "메시지를 찾을 수 없습니다", 404),

    // 403 Forbidden
    MEMBER_NOT_IN_ROOM("MSG403001", "채팅방의 멤버가 아닙니다", 403),
    MEMBER_NOT_ACTIVE("MSG403002", "비활성 상태의 멤버입니다", 403),

    // 409 Conflict
    DUPLICATE_MESSAGE("MSG409001", "이미 전송된 메시지입니다", 409);

    private final String code;
    private final String message;
    private final int httpStatus;
}
