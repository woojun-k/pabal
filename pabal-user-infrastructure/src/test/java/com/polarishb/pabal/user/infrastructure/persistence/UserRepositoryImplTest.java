package com.polarishb.pabal.user.infrastructure.persistence;

import com.polarishb.pabal.support.AbstractUserPostgresDataJpaTest;
import com.polarishb.pabal.user.application.port.out.persistence.UserRepository;
import com.polarishb.pabal.user.contract.persistence.PersistedUser;
import com.polarishb.pabal.user.domain.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
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

    @Test
    void findActiveIdsByTenantIdAndIds_returns_only_active_requested_tenant_users() {
        UUID tenantId = UUID.randomUUID();
        UUID otherTenantId = UUID.randomUUID();
        UUID activeUserId = UUID.randomUUID();
        UUID disabledUserId = UUID.randomUUID();
        UUID otherTenantUserId = UUID.randomUUID();
        UUID missingUserId = UUID.randomUUID();
        Instant now = Instant.parse("2026-04-08T00:00:00Z");
        User disabledUser = User.create(disabledUserId, tenantId, "Disabled", now);
        disabledUser.disable(now.plusSeconds(60));

        userRepository.save(User.create(activeUserId, tenantId, "Active", now));
        userRepository.save(disabledUser);
        userRepository.save(User.create(otherTenantUserId, otherTenantId, "Other Tenant", now));

        Set<UUID> activeIds = userRepository.findActiveIdsByTenantIdAndIds(
                tenantId,
                Set.of(activeUserId, disabledUserId, otherTenantUserId, missingUserId)
        );

        assertThat(activeIds).containsExactly(activeUserId);
    }

    @Test
    void findActiveIdsByTenantIdAndIds_returns_empty_without_querying_empty_id_set() {
        UUID tenantId = UUID.randomUUID();

        Set<UUID> activeIds = userRepository.findActiveIdsByTenantIdAndIds(tenantId, Set.of());

        assertThat(activeIds).isEmpty();
    }
}
