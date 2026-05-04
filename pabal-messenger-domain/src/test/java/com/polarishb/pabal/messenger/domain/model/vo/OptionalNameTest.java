package com.polarishb.pabal.messenger.domain.model.vo;

import com.polarishb.pabal.common.exception.InvalidInputException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OptionalNameTest {

    @Test
    void constructor_accepts_null_blank_and_maximum_length_name() {
        assertThat(new OptionalName(null).valueOrNull()).isNull();
        assertThat(new OptionalName("   ").valueOrNull()).isEmpty();
        assertThatCode(() -> new OptionalName("a".repeat(OptionalName.MAX_LENGTH))).doesNotThrowAnyException();
    }

    @Test
    void constructor_trims_surrounding_whitespace() {
        OptionalName name = new OptionalName("  team  ");

        assertThat(name.value()).isEqualTo("team");
        assertThat(name.valueOrNull()).isEqualTo("team");
    }

    @Test
    void constructor_rejects_name_longer_than_maximum_length() {
        assertThatThrownBy(() -> new OptionalName("a".repeat(OptionalName.MAX_LENGTH + 1)))
                .isInstanceOf(InvalidInputException.class);
    }
}
