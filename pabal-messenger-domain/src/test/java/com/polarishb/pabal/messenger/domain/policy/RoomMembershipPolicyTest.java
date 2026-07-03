package com.polarishb.pabal.messenger.domain.policy;

import com.polarishb.pabal.messenger.domain.exception.RoomJoinForbiddenException;
import com.polarishb.pabal.messenger.domain.exception.RoomOperationNotAllowedException;
import com.polarishb.pabal.messenger.domain.model.ChatRoom;
import com.polarishb.pabal.messenger.domain.model.type.RoomStatus;
import com.polarishb.pabal.messenger.domain.model.type.RoomType;
import com.polarishb.pabal.messenger.domain.model.vo.ChannelName;
import com.polarishb.pabal.messenger.domain.model.vo.ChannelSettings;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoomMembershipPolicyTest {

    private static final Instant CREATED_AT = Instant.parse("2026-04-02T00:00:00Z");

    @Test
    void active_public_channel_allows_self_join_without_throwing() {
        ChatRoom room = ChatRoom.createChannel(
                "general",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                false,
                "team room",
                CREATED_AT
        );

        assertThat(RoomMembershipPolicy.canSelfJoin(room)).isTrue();
        assertThatCode(() -> RoomMembershipPolicy.validateSelfJoin(room)).doesNotThrowAnyException();
    }

    @Test
    void non_active_status_is_checked_first_and_rejects_self_join_even_when_also_non_public() {
        ChatRoom room = reconstituteChannel(
                RoomStatus.PENDING_DELETION,
                new ChannelSettings(UUID.randomUUID(), true, "private, non-active room")
        );

        assertThat(RoomMembershipPolicy.canSelfJoin(room)).isFalse();
        assertThatThrownBy(() -> RoomMembershipPolicy.validateSelfJoin(room))
                .isInstanceOf(RoomOperationNotAllowedException.class);
    }

    @Test
    void active_non_channel_type_rejects_self_join() {
        ChatRoom directRoom = ChatRoom.createDirect(null, UUID.randomUUID(), UUID.randomUUID(), CREATED_AT);
        ChatRoom groupRoom = ChatRoom.createGroup("team", UUID.randomUUID(), UUID.randomUUID(), CREATED_AT);

        assertThat(RoomMembershipPolicy.canSelfJoin(directRoom)).isFalse();
        assertThat(RoomMembershipPolicy.canSelfJoin(groupRoom)).isFalse();
        assertThatThrownBy(() -> RoomMembershipPolicy.validateSelfJoin(directRoom))
                .isInstanceOf(RoomJoinForbiddenException.class);
        assertThatThrownBy(() -> RoomMembershipPolicy.validateSelfJoin(groupRoom))
                .isInstanceOf(RoomJoinForbiddenException.class);
    }

    @Test
    void active_private_channel_rejects_self_join() {
        ChatRoom room = ChatRoom.createChannel(
                "secret",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                true,
                "private room",
                CREATED_AT
        );

        assertThat(RoomMembershipPolicy.canSelfJoin(room)).isFalse();
        assertThatThrownBy(() -> RoomMembershipPolicy.validateSelfJoin(room))
                .isInstanceOf(RoomJoinForbiddenException.class);
    }

    @Test
    void active_channel_with_null_channel_settings_cannot_self_join() {
        ChatRoom room = reconstituteChannel(RoomStatus.ACTIVE, null);

        assertThat(RoomMembershipPolicy.canSelfJoin(room)).isFalse();
        assertThatThrownBy(() -> RoomMembershipPolicy.validateSelfJoin(room))
                .isInstanceOf(RoomJoinForbiddenException.class);
    }

    @Test
    void null_room_throws_null_pointer_exception() {
        assertThatThrownBy(() -> RoomMembershipPolicy.canSelfJoin(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> RoomMembershipPolicy.validateSelfJoin(null))
                .isInstanceOf(NullPointerException.class);
    }

    private static ChatRoom reconstituteChannel(RoomStatus status, ChannelSettings channelSettings) {
        return ChatRoom.reconstitute(
                UUID.randomUUID(),
                RoomType.CHANNEL,
                new ChannelName("general"),
                UUID.randomUUID(),
                UUID.randomUUID(),
                channelSettings,
                status,
                null,
                null,
                0L,
                null,
                CREATED_AT,
                CREATED_AT
        );
    }
}
