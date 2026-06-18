package com.polarishb.pabal.user.domain.model.vo;

import com.polarishb.pabal.common.exception.InvalidInputException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserNameTest {

    @Test
    void constructor_trims_value() {
        UserName name = new UserName(" Alice ");

        assertThat(name.value()).isEqualTo("Alice");
    }

    @Test
    void constructor_rejects_blank_value() {
        assertThatThrownBy(() -> new UserName("   "))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void constructor_rejects_value_over_100_chars() {
        assertThatThrownBy(() -> new UserName("a".repeat(101)))
                .isInstanceOf(InvalidInputException.class);
    }
}
