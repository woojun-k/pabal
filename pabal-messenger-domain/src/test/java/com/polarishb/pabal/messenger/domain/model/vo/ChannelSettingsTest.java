package com.polarishb.pabal.messenger.domain.model.vo;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ChannelSettingsTest {

    @Test
    void create_uses_public_channel_defaults() {
        UUID workspaceId = UUID.randomUUID();

        ChannelSettings settings = ChannelSettings.create(workspaceId);

        assertThat(settings.workspaceId()).isEqualTo(workspaceId);
        assertThat(settings.isPrivate()).isFalse();
        assertThat(settings.description()).isNull();
    }

    @Test
    void withers_return_new_settings_without_mutating_original() {
        UUID workspaceId = UUID.randomUUID();
        ChannelSettings original = ChannelSettings.create(workspaceId);

        ChannelSettings described = original.withDescription("team room");
        ChannelSettings privateRoom = described.withPrivacy(true);

        assertThat(original.description()).isNull();
        assertThat(original.isPrivate()).isFalse();
        assertThat(described.description()).isEqualTo("team room");
        assertThat(described.isPrivate()).isFalse();
        assertThat(privateRoom.description()).isEqualTo("team room");
        assertThat(privateRoom.isPrivate()).isTrue();
    }
}
