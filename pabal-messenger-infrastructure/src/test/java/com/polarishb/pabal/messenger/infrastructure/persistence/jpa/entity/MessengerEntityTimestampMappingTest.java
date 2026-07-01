package com.polarishb.pabal.messenger.infrastructure.persistence.jpa.entity;

import com.polarishb.pabal.messenger.contract.persistence.chatroom.ChatRoomState;
import com.polarishb.pabal.messenger.contract.persistence.chatroommember.ChatRoomMemberState;
import com.polarishb.pabal.messenger.contract.persistence.directchatmapping.DirectChatMappingState;
import com.polarishb.pabal.messenger.contract.persistence.message.MessageState;
import com.polarishb.pabal.messenger.domain.model.type.MessageStatus;
import com.polarishb.pabal.messenger.domain.model.type.MessageType;
import com.polarishb.pabal.messenger.domain.model.type.RoomStatus;
import com.polarishb.pabal.messenger.domain.model.type.RoomType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MessengerEntityTimestampMappingTest {

    private static final Instant CREATED_AT = Instant.parse("2026-06-19T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-06-19T01:00:00Z");

    @Test
    void message_fromNewState_maps_contract_timestamps_to_base_entity_fields() {
        MessageState state = new MessageState(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                1L,
                MessageType.USER,
                "hello",
                MessageStatus.ACTIVE,
                null,
                CREATED_AT,
                UPDATED_AT,
                null,
                null
        );

        MessageEntity entity = MessageEntity.fromNewState(state);

        assertThat(entity.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(entity.getUpdatedAt()).isEqualTo(UPDATED_AT);
        assertThat(entity.toState().createdAt()).isEqualTo(CREATED_AT);
        assertThat(entity.toState().updatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    void chatRoom_fromNewState_maps_contract_timestamps_to_base_entity_fields() {
        ChatRoomState state = new ChatRoomState(
                UUID.randomUUID(),
                RoomType.GROUP,
                "General",
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                RoomStatus.ACTIVE,
                null,
                null,
                0L,
                null,
                CREATED_AT,
                UPDATED_AT,
                null
        );

        ChatRoomEntity entity = ChatRoomEntity.fromNewState(state);

        assertThat(entity.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(entity.getUpdatedAt()).isEqualTo(UPDATED_AT);
        assertThat(entity.toState().createdAt()).isEqualTo(CREATED_AT);
        assertThat(entity.toState().updatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    void chatRoomMember_fromNewState_maps_contract_timestamps_to_base_entity_fields() {
        ChatRoomMemberState state = new ChatRoomMemberState(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                0L,
                CREATED_AT,
                CREATED_AT,
                null,
                CREATED_AT,
                UPDATED_AT,
                null
        );

        ChatRoomMemberEntity entity = ChatRoomMemberEntity.fromNewState(state);

        assertThat(entity.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(entity.getUpdatedAt()).isEqualTo(UPDATED_AT);
        assertThat(entity.toState().createdAt()).isEqualTo(CREATED_AT);
        assertThat(entity.toState().updatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    void directChatMapping_fromNewState_maps_contract_timestamps_to_base_entity_fields() {
        DirectChatMappingState state = new DirectChatMappingState(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                CREATED_AT,
                UPDATED_AT,
                null
        );

        DirectChatMappingEntity entity = DirectChatMappingEntity.fromNewState(state);

        assertThat(entity.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(entity.getUpdatedAt()).isEqualTo(UPDATED_AT);
        assertThat(entity.toState().createdAt()).isEqualTo(CREATED_AT);
        assertThat(entity.toState().updatedAt()).isEqualTo(UPDATED_AT);
    }
}
