package com.polarishb.pabal.user.domain.model;

import com.polarishb.pabal.user.domain.model.type.UserStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void create_sets_active_user_state() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        Instant now = Instant.parse("2026-04-08T00:00:00Z");

        User user = User.create(userId, tenantId, " Alice ", now);

        assertThat(user.getId()).isEqualTo(userId);
        assertThat(user.getTenantId()).isEqualTo(tenantId);
        assertThat(user.getName().value()).isEqualTo("Alice");
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.isActive()).isTrue();
        assertThat(user.getCreatedAt()).isEqualTo(now);
        assertThat(user.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    void disable_marks_user_inactive_and_updates_time() {
        Instant createdAt = Instant.parse("2026-04-08T00:00:00Z");
        Instant disabledAt = Instant.parse("2026-04-08T00:10:00Z");
        User user = User.create(UUID.randomUUID(), UUID.randomUUID(), "Alice", createdAt);

        user.disable(disabledAt);

        assertThat(user.getStatus()).isEqualTo(UserStatus.DISABLED);
        assertThat(user.isActive()).isFalse();
        assertThat(user.getUpdatedAt()).isEqualTo(disabledAt);
    }
}
