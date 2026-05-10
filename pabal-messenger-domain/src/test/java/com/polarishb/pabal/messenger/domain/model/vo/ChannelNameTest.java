package com.polarishb.pabal.messenger.domain.model.vo;

import com.polarishb.pabal.common.exception.InvalidInputException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChannelNameTest {

    @Test
    void constructor_accepts_minimum_and_maximum_length_names() {
        assertThatCode(() -> new ChannelName("a")).doesNotThrowAnyException();
        assertThatCode(() -> new ChannelName("a".repeat(50))).doesNotThrowAnyException();
    }

    @Test
    void constructor_normalizes_name_to_lower_case() {
        ChannelName channelName = new ChannelName("General_Notice");

        assertThat(channelName.value()).isEqualTo("general_notice");
    }

    @Test
    void constructor_trims_surrounding_whitespace() {
        ChannelName channelName = new ChannelName("  General  ");

        assertThat(channelName.value()).isEqualTo("general");
    }

    @Test
    void constructor_accepts_korean_letters_numbers_underscore_and_hyphen() {
        ChannelName channelName = new ChannelName("공지_채널-1");

        assertThat(channelName.value()).isEqualTo("공지_채널-1");
    }

    @Test
    void constructor_rejects_null_blank_too_long_and_unsupported_characters() {
        assertThatThrownBy(() -> new ChannelName(null))
                .isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> new ChannelName("   "))
                .isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> new ChannelName("a".repeat(51)))
                .isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> new ChannelName("general notice"))
                .isInstanceOf(InvalidInputException.class);
    }
}
