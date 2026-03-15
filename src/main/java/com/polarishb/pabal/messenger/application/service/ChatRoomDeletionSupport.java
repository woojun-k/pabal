package com.polarishb.pabal.messenger.application.service;

import com.polarishb.pabal.messenger.contract.persistence.chatroom.PersistedChatRoom;
import com.polarishb.pabal.messenger.domain.exception.ChatRoomNotFoundException;
import com.polarishb.pabal.messenger.domain.exception.RoomMustBePendingDeletionException;
import com.polarishb.pabal.messenger.domain.exception.UnauthorizedRoomDeletionException;
import com.polarishb.pabal.messenger.domain.model.entity.ChatRoom;
import com.polarishb.pabal.messenger.domain.model.type.RoomStatus;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ChatRoomDeletionSupport {

    private final ChatRoomRepository chatRoomRepository;

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

        // 현재: 채널 생성자 + PENDING_DELETION만 허용
        if (!room.getCreatedBy().equals(requesterId)) {
            throw new UnauthorizedRoomDeletionException(requesterId, room.getId());
        }

        if (room.getStatus() != RoomStatus.PENDING_DELETION) {
            throw new RoomMustBePendingDeletionException(room.getId(), room.getStatus());
        }
    }

    public void scheduleForDeletion(PersistedChatRoom persistedRoom) {
        ChatRoom room = persistedRoom.chatRoom();
        room.scheduleForDeletion(Instant.now());
        chatRoomRepository.update(persistedRoom);
    }

    public void deleteImmediately(PersistedChatRoom persistedRoom) {
        ChatRoom room = persistedRoom.chatRoom();
        room.deleteImmediately();
        chatRoomRepository.update(persistedRoom);
    }
}
