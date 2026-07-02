package com.polarishb.pabal.user.domain.model;

import com.polarishb.pabal.user.domain.exception.UserAlreadyDisabledException;
import com.polarishb.pabal.user.domain.model.type.UserStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    void rename_returns_new_instance_with_new_name_and_updatedAt_and_leaves_receiver_unchanged() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-04-08T00:00:00Z");
        Instant renamedAt = Instant.parse("2026-04-08T00:10:00Z");
        User user = User.create(userId, tenantId, "Alice", createdAt);

        User renamed = user.rename("Bob", renamedAt);

        assertThat(renamed).isNotSameAs(user);
        assertThat(renamed.getName().value()).isEqualTo("Bob");
        assertThat(renamed.getUpdatedAt()).isEqualTo(renamedAt);
        assertThat(renamed.getId()).isEqualTo(user.getId());
        assertThat(renamed.getTenantId()).isEqualTo(user.getTenantId());
        assertThat(renamed.getStatus()).isEqualTo(user.getStatus());
        assertThat(renamed.getCreatedAt()).isEqualTo(user.getCreatedAt());

        // receiver observably unchanged
        assertThat(user.getName().value()).isEqualTo("Alice");
        assertThat(user.getUpdatedAt()).isEqualTo(createdAt);
    }

    @Test
    void disable_returns_new_instance_marked_disabled_and_leaves_receiver_unchanged() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-04-08T00:00:00Z");
        Instant disabledAt = Instant.parse("2026-04-08T00:10:00Z");
        User user = User.create(userId, tenantId, "Alice", createdAt);

        User disabled = user.disable(disabledAt);

        assertThat(disabled).isNotSameAs(user);
        assertThat(disabled.getStatus()).isEqualTo(UserStatus.DISABLED);
        assertThat(disabled.isActive()).isFalse();
        assertThat(disabled.getUpdatedAt()).isEqualTo(disabledAt);
        assertThat(disabled.getId()).isEqualTo(user.getId());
        assertThat(disabled.getTenantId()).isEqualTo(user.getTenantId());
        assertThat(disabled.getName()).isEqualTo(user.getName());
        assertThat(disabled.getCreatedAt()).isEqualTo(user.getCreatedAt());

        // receiver observably unchanged
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.isActive()).isTrue();
        assertThat(user.getUpdatedAt()).isEqualTo(createdAt);
    }

    @Test
    void disable_on_already_disabled_user_throws_and_produces_no_new_state() {
        Instant createdAt = Instant.parse("2026-04-08T00:00:00Z");
        Instant firstDisableAt = Instant.parse("2026-04-08T00:10:00Z");
        User user = User.create(UUID.randomUUID(), UUID.randomUUID(), "Alice", createdAt);
        User disabled = user.disable(firstDisableAt);

        assertThatThrownBy(() -> disabled.disable(firstDisableAt.plusSeconds(60)))
                .isInstanceOf(UserAlreadyDisabledException.class);

        // receiver of the second call remains unchanged
        assertThat(disabled.getStatus()).isEqualTo(UserStatus.DISABLED);
        assertThat(disabled.getUpdatedAt()).isEqualTo(firstDisableAt);
    }

    @Test
    void rename_on_disabled_user_throws_and_produces_no_new_state() {
        Instant createdAt = Instant.parse("2026-04-08T00:00:00Z");
        Instant disabledAt = Instant.parse("2026-04-08T00:10:00Z");
        User user = User.create(UUID.randomUUID(), UUID.randomUUID(), "Alice", createdAt);
        User disabled = user.disable(disabledAt);

        assertThatThrownBy(() -> disabled.rename("Bob", disabledAt.plusSeconds(60)))
                .isInstanceOf(UserAlreadyDisabledException.class);

        // receiver of the rejected rename remains unchanged
        assertThat(disabled.getName().value()).isEqualTo("Alice");
        assertThat(disabled.getUpdatedAt()).isEqualTo(disabledAt);
    }

    @Test
    void rename_rejects_null_updatedAt() {
        Instant createdAt = Instant.parse("2026-04-08T00:00:00Z");
        User user = User.create(UUID.randomUUID(), UUID.randomUUID(), "Alice", createdAt);

        assertThatThrownBy(() -> user.rename("Bob", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void disable_rejects_null_updatedAt() {
        Instant createdAt = Instant.parse("2026-04-08T00:00:00Z");
        User user = User.create(UUID.randomUUID(), UUID.randomUUID(), "Alice", createdAt);

        assertThatThrownBy(() -> user.disable(null))
                .isInstanceOf(NullPointerException.class);
    }
}
