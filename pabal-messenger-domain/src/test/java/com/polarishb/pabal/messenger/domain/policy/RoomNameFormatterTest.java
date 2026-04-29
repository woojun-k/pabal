package com.polarishb.pabal.messenger.domain.policy;

import com.polarishb.pabal.messenger.domain.model.vo.OptionalName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class RoomNameFormatterTest {

    @Test
    void formatGroupRoomName_uses_short_uuid_tokens_within_optional_name_limit() {
        String roomName = RoomNameFormatter.formatGroupRoomName(
                uuid(1),
                List.of(uuid(2))
        );

        assertThat(roomName).isEqualTo("00000001, 00000002");
        assertThat(roomName).hasSizeLessThanOrEqualTo(OptionalName.MAX_LENGTH);
        assertThatCode(() -> new OptionalName(roomName)).doesNotThrowAnyException();
    }

    @Test
    void formatGroupRoomName_summarizes_extra_members_within_optional_name_limit() {
        String roomName = RoomNameFormatter.formatGroupRoomName(
                uuid(1),
                List.of(uuid(2), uuid(3), uuid(4), uuid(5))
        );

        assertThat(roomName).isEqualTo("00000001, 00000002, 00000003 외 2명");
        assertThat(roomName).hasSizeLessThanOrEqualTo(OptionalName.MAX_LENGTH);
        assertThatCode(() -> new OptionalName(roomName)).doesNotThrowAnyException();
    }

    private UUID uuid(int value) {
        return UUID.fromString("%08d-0000-0000-0000-000000000000".formatted(value));
    }
}
