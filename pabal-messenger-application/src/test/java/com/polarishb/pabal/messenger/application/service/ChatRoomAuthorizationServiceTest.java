package com.polarishb.pabal.messenger.application.service;

import com.polarishb.pabal.messenger.application.authorization.MessengerPermission;
import com.polarishb.pabal.messenger.application.authorization.PermissionCheck;
import com.polarishb.pabal.messenger.application.port.out.authorization.PermissionPort;
import com.polarishb.pabal.messenger.domain.exception.ChannelPermissionDeniedException;
import com.polarishb.pabal.messenger.domain.exception.UnauthorizedRoomDeletionException;
import com.polarishb.pabal.messenger.domain.model.ChatRoom;
import com.polarishb.pabal.messenger.domain.model.type.RoomStatus;
import com.polarishb.pabal.messenger.domain.model.type.RoomType;
import com.polarishb.pabal.messenger.domain.model.vo.ChannelName;
import com.polarishb.pabal.messenger.domain.model.vo.ChannelSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatRoomAuthorizationServiceTest {

    @Mock
    private PermissionPort permissionPort;

    @InjectMocks
    private ChatRoomAuthorizationService authorizationService;

    @Test
    void validateCanScheduleRoomDeletion_requires_schedule_own_permission_for_creator() {
        UUID requesterId = uuid(1);
        ChatRoom room = channel(uuid(100), uuid(200), uuid(300), requesterId);

        when(permissionPort.hasPermission(org.mockito.ArgumentMatchers.any(PermissionCheck.class)))
                .thenReturn(true);

        authorizationService.validateCanScheduleRoomDeletion(room, requesterId);

        ArgumentCaptor<PermissionCheck> checkCaptor = ArgumentCaptor.forClass(PermissionCheck.class);
        verify(permissionPort).hasPermission(checkCaptor.capture());
        assertThat(checkCaptor.getValue().permission()).isEqualTo(MessengerPermission.CHANNEL_DELETE_SCHEDULE_OWN);
    }

    @Test
    void validateCanImmediatelyDeleteRoom_requires_execute_any_permission_for_non_creator() {
        UUID tenantId = uuid(100);
        UUID roomId = uuid(200);
        UUID workspaceId = uuid(300);
        UUID requesterId = uuid(2);
        ChatRoom room = channel(tenantId, roomId, workspaceId, uuid(1));

        when(permissionPort.hasPermission(org.mockito.ArgumentMatchers.any(PermissionCheck.class)))
                .thenReturn(true);

        authorizationService.validateCanImmediatelyDeleteRoom(room, requesterId);

        ArgumentCaptor<PermissionCheck> checkCaptor = ArgumentCaptor.forClass(PermissionCheck.class);
        verify(permissionPort).hasPermission(checkCaptor.capture());
        assertThat(checkCaptor.getValue().tenantId()).isEqualTo(tenantId);
        assertThat(checkCaptor.getValue().requesterId()).isEqualTo(requesterId);
        assertThat(checkCaptor.getValue().workspaceId()).isEqualTo(workspaceId);
        assertThat(checkCaptor.getValue().chatRoomId()).isEqualTo(roomId);
        assertThat(checkCaptor.getValue().permission()).isEqualTo(MessengerPermission.CHANNEL_DELETE_EXECUTE_ANY);
    }

    @Test
    void validateCanImmediatelyDeleteRoom_rejects_non_creator_without_delete_permission() {
        UUID requesterId = uuid(2);
        ChatRoom room = channel(uuid(100), uuid(200), uuid(300), uuid(1));

        when(permissionPort.hasPermission(org.mockito.ArgumentMatchers.any(PermissionCheck.class)))
                .thenReturn(false);

        assertThatThrownBy(() -> authorizationService.validateCanImmediatelyDeleteRoom(room, requesterId))
                .isInstanceOf(UnauthorizedRoomDeletionException.class);
    }

    @Test
    void requireChannelCreation_rejects_user_without_channel_create_permission() {
        UUID tenantId = uuid(100);
        UUID requesterId = uuid(1);
        UUID workspaceId = uuid(300);

        when(permissionPort.hasPermission(org.mockito.ArgumentMatchers.any(PermissionCheck.class)))
                .thenReturn(false);

        assertThatThrownBy(() -> authorizationService.requireChannelCreation(tenantId, requesterId, workspaceId))
                .isInstanceOf(ChannelPermissionDeniedException.class);

        ArgumentCaptor<PermissionCheck> checkCaptor = ArgumentCaptor.forClass(PermissionCheck.class);
        verify(permissionPort).hasPermission(checkCaptor.capture());
        assertThat(checkCaptor.getValue().tenantId()).isEqualTo(tenantId);
        assertThat(checkCaptor.getValue().requesterId()).isEqualTo(requesterId);
        assertThat(checkCaptor.getValue().workspaceId()).isEqualTo(workspaceId);
        assertThat(checkCaptor.getValue().permission()).isEqualTo(MessengerPermission.CHANNEL_CREATE);
    }

    private ChatRoom channel(UUID tenantId, UUID roomId, UUID workspaceId, UUID createdBy) {
        Instant createdAt = Instant.parse("2026-04-02T12:00:00Z");
        return ChatRoom.reconstitute(
                roomId,
                RoomType.CHANNEL,
                new ChannelName("general"),
                createdBy,
                tenantId,
                new ChannelSettings(workspaceId, false, null),
                RoomStatus.ACTIVE,
                null,
                null,
                0L,
                null,
                createdAt,
                createdAt
        );
    }

    private UUID uuid(int value) {
        return UUID.fromString("%08d-0000-0000-0000-000000000000".formatted(value));
    }
}
