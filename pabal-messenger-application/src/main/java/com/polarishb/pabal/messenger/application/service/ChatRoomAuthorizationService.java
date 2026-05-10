package com.polarishb.pabal.messenger.application.service;

import com.polarishb.pabal.messenger.application.authorization.MessengerPermission;
import com.polarishb.pabal.messenger.application.authorization.PermissionCheck;
import com.polarishb.pabal.messenger.application.port.out.authorization.PermissionPort;
import com.polarishb.pabal.messenger.domain.exception.ChannelPermissionDeniedException;
import com.polarishb.pabal.messenger.domain.exception.UnauthorizedRoomDeletionException;
import com.polarishb.pabal.messenger.domain.model.ChatRoom;
import com.polarishb.pabal.messenger.domain.model.vo.ChannelSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ChatRoomAuthorizationService {

    private final PermissionPort permissionPort;

    public void requireChannelCreation(UUID tenantId, UUID requesterId, UUID workspaceId) {
        PermissionCheck check = PermissionCheck.workspace(
                tenantId,
                requesterId,
                workspaceId,
                MessengerPermission.CHANNEL_CREATE
        );

        if (!permissionPort.hasPermission(check)) {
            throw new ChannelPermissionDeniedException(
                    requesterId,
                    workspaceId,
                    null,
                    MessengerPermission.CHANNEL_CREATE.value()
            );
        }
    }

    public void validateCanScheduleRoomDeletion(ChatRoom room, UUID requesterId) {
        validateCanDeleteRoom(
                room,
                requesterId,
                MessengerPermission.CHANNEL_DELETE_SCHEDULE_OWN,
                MessengerPermission.CHANNEL_DELETE_SCHEDULE_ANY
        );
    }

    public void validateCanImmediatelyDeleteRoom(ChatRoom room, UUID requesterId) {
        validateCanDeleteRoom(
                room,
                requesterId,
                MessengerPermission.CHANNEL_DELETE_EXECUTE_OWN,
                MessengerPermission.CHANNEL_DELETE_EXECUTE_ANY
        );
    }

    private void validateCanDeleteRoom(
            ChatRoom room,
            UUID requesterId,
            MessengerPermission ownPermission,
            MessengerPermission anyPermission
    ) {
        MessengerPermission requiredPermission = room.getCreatedBy().equals(requesterId)
                ? ownPermission
                : anyPermission;
        PermissionCheck check = PermissionCheck.room(
                room.getTenantId(),
                requesterId,
                workspaceIdOf(room),
                room.getId(),
                requiredPermission
        );

        if (permissionPort.hasPermission(check)) {
            return;
        }

        throw new UnauthorizedRoomDeletionException(requesterId, room.getId(), requiredPermission.value());
    }

    private UUID workspaceIdOf(ChatRoom room) {
        ChannelSettings settings = room.getChannelSettings();
        return settings == null ? null : settings.workspaceId();
    }
}
