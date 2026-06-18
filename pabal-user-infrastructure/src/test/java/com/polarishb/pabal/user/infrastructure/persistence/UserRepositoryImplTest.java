package com.polarishb.pabal.user.infrastructure.persistence;

import com.polarishb.pabal.support.AbstractUserPostgresDataJpaTest;
import com.polarishb.pabal.user.application.port.out.persistence.UserRepository;
import com.polarishb.pabal.user.contract.persistence.PersistedUser;
import com.polarishb.pabal.user.domain.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserRepositoryImplTest extends AbstractUserPostgresDataJpaTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void save_and_findByTenantIdAndId_round_trips_active_user() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        Instant now = Instant.parse("2026-04-08T00:00:00Z");
        User user = User.create(userId, tenantId, "Alice", now);

        PersistedUser saved = userRepository.save(user);
        Optional<PersistedUser> found = userRepository.findByTenantIdAndId(tenantId, userId);

        assertThat(saved.state().id()).isEqualTo(userId);
        assertThat(saved.state().tenantId()).isEqualTo(tenantId);
        assertThat(saved.state().name()).isEqualTo("Alice");
        assertThat(saved.state().version()).isEqualTo(0L);
        assertThat(found).isPresent();
        assertThat(found.orElseThrow().state().id()).isEqualTo(userId);
        assertThat(userRepository.existsActiveByTenantIdAndId(tenantId, userId)).isTrue();
    }

    @Test
    void existsActiveByTenantIdAndId_returns_false_for_disabled_user() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        Instant now = Instant.parse("2026-04-08T00:00:00Z");
        User user = User.create(userId, tenantId, "Alice", now);
        user.disable(now.plusSeconds(60));

        userRepository.save(user);

        assertThat(userRepository.existsActiveByTenantIdAndId(tenantId, userId)).isFalse();
    }
}
