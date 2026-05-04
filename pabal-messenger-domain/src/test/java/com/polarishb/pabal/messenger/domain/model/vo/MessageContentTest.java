package com.polarishb.pabal.messenger.domain.model.vo;

import com.polarishb.pabal.common.exception.InvalidInputException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MessageContentTest {

    @Test
    void constructor_accepts_single_character_and_maximum_length_content() {
        assertThatCode(() -> new MessageContent("a")).doesNotThrowAnyException();
        assertThatCode(() -> new MessageContent("a".repeat(5000))).doesNotThrowAnyException();
    }

    @Test
    void constructor_preserves_non_blank_content_as_given() {
        MessageContent content = new MessageContent("  hello  ");

        assertThat(content.value()).isEqualTo("  hello  ");
    }

    @Test
    void constructor_rejects_null_blank_and_too_long_content() {
        assertThatThrownBy(() -> new MessageContent(null))
                .isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> new MessageContent("   "))
                .isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> new MessageContent("a".repeat(5001)))
                .isInstanceOf(InvalidInputException.class);
    }
}
