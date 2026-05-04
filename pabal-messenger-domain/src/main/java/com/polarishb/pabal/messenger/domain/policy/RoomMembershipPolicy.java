package com.polarishb.pabal.messenger.domain.policy;

import com.polarishb.pabal.messenger.domain.exception.RoomJoinForbiddenException;
import com.polarishb.pabal.messenger.domain.exception.RoomOperationNotAllowedException;
import com.polarishb.pabal.messenger.domain.model.ChatRoom;
import com.polarishb.pabal.messenger.domain.model.type.RoomAccessOperation;
import com.polarishb.pabal.messenger.domain.model.type.RoomStatus;
import com.polarishb.pabal.messenger.domain.model.type.RoomType;
import com.polarishb.pabal.messenger.domain.model.vo.ChannelSettings;

import java.util.Objects;

public final class RoomMembershipPolicy {

    private RoomMembershipPolicy() {
    }

    public static boolean canSelfJoin(ChatRoom room) {
        ChatRoom requiredRoom = Objects.requireNonNull(room);
        return requiredRoom.getStatus() == RoomStatus.ACTIVE
                && requiredRoom.getType() == RoomType.CHANNEL
                && isPublicChannel(requiredRoom);
    }

    public static void validateSelfJoin(ChatRoom room) {
        ChatRoom requiredRoom = Objects.requireNonNull(room);
        if (requiredRoom.getStatus() != RoomStatus.ACTIVE) {
            throw new RoomOperationNotAllowedException(
                    requiredRoom.getId(),
                    requiredRoom.getStatus(),
                    RoomAccessOperation.JOIN
            );
        }
        if (requiredRoom.getType() != RoomType.CHANNEL || !isPublicChannel(requiredRoom)) {
            throw new RoomJoinForbiddenException(
                    requiredRoom.getId(),
                    requiredRoom.getType(),
                    channelPrivacy(requiredRoom)
            );
        }
    }

    private static boolean isPublicChannel(ChatRoom room) {
        ChannelSettings settings = room.getChannelSettings();
        return settings != null && !settings.isPrivate();
    }

    private static Boolean channelPrivacy(ChatRoom room) {
        ChannelSettings settings = room.getChannelSettings();
        return settings != null ? settings.isPrivate() : null;
    }
}
