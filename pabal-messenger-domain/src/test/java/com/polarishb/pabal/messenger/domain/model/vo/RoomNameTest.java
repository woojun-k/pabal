package com.polarishb.pabal.messenger.domain.model.vo;

import com.polarishb.pabal.common.exception.InvalidInputException;
import com.polarishb.pabal.messenger.domain.model.type.RoomType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoomNameTest {

    @Test
    void of_creates_channel_name_for_channel_rooms() {
        RoomName roomName = RoomName.of(RoomType.CHANNEL, "General");

        assertThat(roomName).isInstanceOf(ChannelName.class);
        assertThat(roomName.valueOrNull()).isEqualTo("general");
    }

    @Test
    void of_creates_optional_name_for_group_and_direct_rooms() {
        RoomName groupName = RoomName.of(RoomType.GROUP, "  team  ");
        RoomName directName = RoomName.of(RoomType.DIRECT, null);

        assertThat(groupName).isInstanceOf(OptionalName.class);
        assertThat(groupName.valueOrNull()).isEqualTo("team");
        assertThat(directName).isInstanceOf(OptionalName.class);
        assertThat(directName.valueOrNull()).isNull();
    }

    @Test
    void of_rejects_null_room_type() {
        assertThatThrownBy(() -> RoomName.of(null, "team"))
                .isInstanceOf(InvalidInputException.class);
    }
}
