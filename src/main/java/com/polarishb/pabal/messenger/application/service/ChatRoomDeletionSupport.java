package com.polarishb.pabal.messenger.application.service;

import com.polarishb.pabal.messenger.application.port.out.time.ClockPort;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.PersistedChatRoom;
import com.polarishb.pabal.messenger.domain.exception.ChatRoomNotFoundException;
import com.polarishb.pabal.messenger.domain.exception.UnauthorizedRoomDeletionException;
import com.polarishb.pabal.messenger.domain.model.entity.ChatRoom;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ChatRoomDeletionSupport {

    private final ChatRoomRepository chatRoomRepository;
    private final ClockPort clockPort;

    public PersistedChatRoom loadRoom(UUID tenantId, UUID roomId) {
        return chatRoomRepository.findByTenantIdAndId(tenantId, roomId)
                .orElseThrow(() -> new ChatRoomNotFoundException(roomId));
    }

    public void validateScheduleDeletionPermission(ChatRoom room, UUID requesterId) {
        if (!room.getCreatedBy().equals(requesterId)) {
            throw new UnauthorizedRoomDeletionException(requesterId, room.getId());
        }
    }

    public void validateImmediateDeletionPermission(ChatRoom room, UUID requesterId) {
        // TODO: RBAC 추가 시 구현
        // - 테넌트 관리자는 언제든지 삭제 가능
        // - 워크스페이스 관리자는 PENDING_DELETION일 때만 가능

        // 현재: 채널 생성자만 허용
        // 상태 전이(PENDING_DELETION 여부)는 ChatRoom 도메인 엔티티가 검증
        if (!room.getCreatedBy().equals(requesterId)) {
            throw new UnauthorizedRoomDeletionException(requesterId, room.getId());
        }
    }

    public void scheduleForDeletion(PersistedChatRoom persistedRoom) {
        ChatRoom room = persistedRoom.chatRoom();
        // TODO: 추후 default 혹은 현재 설정 값으로 불러와야함
        room.scheduleForDeletion(clockPort.now());
        chatRoomRepository.update(persistedRoom);
    }

    public void deleteImmediately(PersistedChatRoom persistedRoom) {
        ChatRoom room = persistedRoom.chatRoom();
        room.deleteImmediately(clockPort.now());
        chatRoomRepository.update(persistedRoom);
    }
}
