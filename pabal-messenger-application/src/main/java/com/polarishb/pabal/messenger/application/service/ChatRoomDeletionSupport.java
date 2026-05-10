package com.polarishb.pabal.messenger.application.service;

import com.polarishb.pabal.messenger.application.port.out.time.ClockPort;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.PersistedChatRoom;
import com.polarishb.pabal.messenger.domain.exception.ChatRoomNotFoundException;
import com.polarishb.pabal.messenger.domain.model.ChatRoom;
import com.polarishb.pabal.messenger.application.port.out.persistence.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ChatRoomDeletionSupport {

    private final ChatRoomRepository chatRoomRepository;
    private final ClockPort clockPort;
    private final ChatRoomAuthorizationService authorizationService;

    public PersistedChatRoom loadRoom(UUID tenantId, UUID roomId) {
        return chatRoomRepository.findByTenantIdAndId(tenantId, roomId)
                .orElseThrow(() -> new ChatRoomNotFoundException(roomId));
    }

    public void validateScheduleDeletionPermission(ChatRoom room, UUID requesterId) {
        authorizationService.validateCanScheduleRoomDeletion(room, requesterId);
    }

    public void validateImmediateDeletionPermission(ChatRoom room, UUID requesterId) {
        authorizationService.validateCanImmediatelyDeleteRoom(room, requesterId);
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
