package com.polarishb.pabal.messenger.domain.model.vo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChannelNameTest {

    @Test
    void constructor_normalizes_name_to_lower_case() {
        ChannelName channelName = new ChannelName("General_Notice");

        assertThat(channelName.value()).isEqualTo("general_notice");
    }
}
