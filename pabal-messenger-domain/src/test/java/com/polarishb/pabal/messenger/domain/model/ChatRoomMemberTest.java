package com.polarishb.pabal.messenger.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ChatRoomMemberTest {

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
}
