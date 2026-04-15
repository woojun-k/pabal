package com.polarishb.pabal.messenger.domain.exception.code;

import com.polarishb.pabal.common.exception.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MessengerErrorCode implements ErrorCode {

    // 400 Bad Request
    INVALID_REPLY_TARGET("MSG400001", "유효하지 않은 대상입니다", 400),
    ROOM_CANNOT_BE_DELETED("MSG400002", "채팅방을 삭제할 수 없는 타입입니다", 400),
    INVALID_ROOM_STATUS("MSG400003", "채팅방을 삭제할 수 없는 상태입니다", 400),
    INVALID_ROOM_STATUS_TRANSITION("MSG400004", "채팅방 상태를 변경할 수 없습니다", 400),
    MESSAGE_ALREADY_DELETED("MSG400005", "이미 삭제된 메시지입니다", 400),

    // 404 Not Found
    CHAT_ROOM_NOT_FOUND("MSG404001", "채팅방을 찾을 수 없습니다", 404),
    MESSAGE_NOT_FOUND("MSG404002", "메시지를 찾을 수 없습니다", 404),
    MEMBER_NOT_FOUND("MSG404003", "채팅방에 없는 멤버입니다", 404),
    DIRECT_CHAT_MAPPING_NOT_FOUND("MSG404004", "다이렉트 채팅 매핑을 찾을 수 없습니다", 404),

    // 403 Forbidden
    MEMBER_NOT_IN_ROOM("MSG403001", "채팅방의 멤버가 아닙니다", 403),
    MEMBER_NOT_ACTIVE("MSG403002", "비활성 상태의 멤버입니다", 403),
    MESSAGE_EDIT_FORBIDDEN("MSG403003", "메시지를 수정할 권한이 없습니다", 403),
    ROOM_DELETE_FORBIDDEN("MSG403004", "채팅방을 삭제할 권한이 없습니다", 403),
    MESSAGE_DELETE_FORBIDDEN("MSG403005", "메시지를 삭제할 권한이 없습니다", 403),
    ROOM_OPERATION_NOT_ALLOWED("MSG403006", "현재 채팅방 상태에서는 이 작업을 수행할 수 없습니다", 403),

    // 409 Conflict
    DUPLICATE_MESSAGE("MSG409001", "이미 전송된 메시지입니다", 409),
    DUPLICATE_DIRECT_MAPPING("MSG409002", "이미 생성된 채팅방입니다", 409),
    DUPLICATE_CHANNEL_NAME("MSG409003", "이미 있는 채널 이름입니다", 409),
    MEMBER_ALREADY_ACTIVE("MSG409004", "이미 활성 상태의 멤버입니다", 409);

    private final String code;
    private final String message;
    private final int httpStatus;
}
