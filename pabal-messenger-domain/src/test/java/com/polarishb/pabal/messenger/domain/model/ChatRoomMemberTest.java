package com.polarishb.pabal.messenger.domain.model;

import com.polarishb.pabal.messenger.domain.exception.MemberAlreadyActiveException;
import com.polarishb.pabal.messenger.domain.exception.MemberNotActiveException;
import com.polarishb.pabal.messenger.domain.model.snapshot.ChatRoomMemberSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatRoomMemberTest {

    @Test
    void snapshot_round_trip_preserves_membership_state() {
        Instant joinedAt = Instant.parse("2026-04-02T00:00:00Z");
        Instant lastReadAt = joinedAt.plusSeconds(30);
        Instant updatedAt = joinedAt.plusSeconds(60);
        ChatRoomMemberSnapshot snapshot = new ChatRoomMemberSnapshot(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                15L,
                lastReadAt,
                joinedAt,
                null,
                joinedAt,
                updatedAt
        );

        ChatRoomMember member = ChatRoomMember.reconstitute(snapshot);

        assertThat(member.snapshot()).isEqualTo(snapshot);
    }

    @Test
    void updateLastRead_keeps_highest_sequence() {
        UUID tenantId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant joinedAt = Instant.parse("2026-04-02T00:00:00Z");
        ChatRoomMember member = ChatRoomMember.create(tenantId, chatRoomId, userId, joinedAt, 0L);

        UUID newerMessageId = UUID.randomUUID();
        Instant newerReadAt = joinedAt.plusSeconds(10);
        assertThat(member.updateLastRead(newerMessageId, 10L, newerReadAt)).isTrue();

        UUID olderMessageId = UUID.randomUUID();
        Instant olderReadAt = joinedAt.plusSeconds(20);
        assertThat(member.updateLastRead(olderMessageId, 9L, olderReadAt)).isFalse();

        assertThat(member.getLastReadMessageId()).isEqualTo(newerMessageId);
        assertThat(member.getLastReadSequence()).isEqualTo(10L);
        assertThat(member.getLastReadAt()).isEqualTo(newerReadAt);
        assertThat(member.getUpdatedAt()).isEqualTo(newerReadAt);
    }

    @Test
    void wouldAdvanceLastReadCursorTo_requires_higher_sequence() {
        UUID tenantId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant joinedAt = Instant.parse("2026-04-02T00:00:00Z");
        ChatRoomMember member = ChatRoomMember.create(tenantId, chatRoomId, userId, joinedAt, 5L);

        assertThat(member.wouldAdvanceLastReadCursorTo(4L)).isFalse();
        assertThat(member.wouldAdvanceLastReadCursorTo(5L)).isFalse();
        assertThat(member.wouldAdvanceLastReadCursorTo(6L)).isTrue();
    }

    @Test
    void leave_marks_member_inactive_and_rejects_second_leave() {
        Instant joinedAt = Instant.parse("2026-04-02T00:00:00Z");
        Instant leftAt = joinedAt.plusSeconds(60);
        ChatRoomMember member = ChatRoomMember.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                joinedAt,
                0L
        );

        member.leave(leftAt);

        assertThat(member.isActive()).isFalse();
        assertThat(member.getLeftAt()).isEqualTo(leftAt);
        assertThat(member.getUpdatedAt()).isEqualTo(leftAt);
        assertThatThrownBy(() -> member.leave(leftAt.plusSeconds(60)))
                .isInstanceOf(MemberNotActiveException.class);
    }

    @Test
    void rejoin_requires_inactive_member_and_resets_read_cursor() {
        Instant joinedAt = Instant.parse("2026-04-02T00:00:00Z");
        Instant leftAt = joinedAt.plusSeconds(60);
        Instant rejoinedAt = joinedAt.plusSeconds(120);
        ChatRoomMember member = ChatRoomMember.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                joinedAt,
                0L
        );

        assertThatThrownBy(() -> member.rejoin(rejoinedAt, 10L))
                .isInstanceOf(MemberAlreadyActiveException.class);

        member.updateLastRead(UUID.randomUUID(), 9L, joinedAt.plusSeconds(30));
        member.leave(leftAt);
        member.rejoin(rejoinedAt, 10L);

        assertThat(member.isActive()).isTrue();
        assertThat(member.getLeftAt()).isNull();
        assertThat(member.getJoinedAt()).isEqualTo(rejoinedAt);
        assertThat(member.getLastReadMessageId()).isNull();
        assertThat(member.getLastReadSequence()).isEqualTo(10L);
        assertThat(member.getLastReadAt()).isNull();
        assertThat(member.getUpdatedAt()).isEqualTo(rejoinedAt);
    }
}
