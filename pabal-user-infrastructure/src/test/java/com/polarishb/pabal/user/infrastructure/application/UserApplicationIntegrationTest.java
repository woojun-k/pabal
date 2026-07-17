package com.polarishb.pabal.user.infrastructure.application;

import com.polarishb.pabal.integration.contract.TenantContract;
import com.polarishb.pabal.integration.contract.dto.UserInfo;
import com.polarishb.pabal.common.exception.InvalidInputException;
import com.polarishb.pabal.support.AbstractUserPostgresDataJpaTest;
import com.polarishb.pabal.user.application.command.handler.CreateUserCommandHandler;
import com.polarishb.pabal.user.application.command.input.CreateUserCommand;
import com.polarishb.pabal.user.application.command.output.CreateUserResult;
import com.polarishb.pabal.user.application.port.out.persistence.UserRepository;
import com.polarishb.pabal.user.application.port.out.time.ClockPort;
import com.polarishb.pabal.user.application.service.UserContractService;
import com.polarishb.pabal.user.contract.persistence.PersistedUser;
import com.polarishb.pabal.user.domain.exception.DuplicateUserException;
import com.polarishb.pabal.user.domain.exception.UserNotFoundException;
import com.polarishb.pabal.user.domain.model.User;
import com.polarishb.pabal.user.domain.model.type.UserStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import({
        CreateUserCommandHandler.class,
        UserContractService.class,
        UserApplicationIntegrationTest.TestPorts.class
})
class UserApplicationIntegrationTest extends AbstractUserPostgresDataJpaTest {

    @Autowired
    private CreateUserCommandHandler createUserHandler;

    @Autowired
    private UserContractService userContractService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MutableTenantContract tenantContract;

    @Autowired
    private MutableClockPort clockPort;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void resetTestPorts() {
        tenantContract.clear();
        clockPort.setNow(Instant.parse("2026-06-19T00:00:00Z"));
    }

    @Test
    void create_user_handler_persists_active_user_through_repository_port() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        tenantContract.addActiveTenant(tenantId);
        clockPort.setNow(now);

        CreateUserResult result = createUserHandler.handle(new CreateUserCommand(userId, tenantId, "Alice"));

        flushAndClear();
        PersistedUser found = userRepository.findByTenantIdAndId(tenantId, userId).orElseThrow();

        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.tenantId()).isEqualTo(tenantId);
        assertThat(result.name()).isEqualTo("Alice");
        assertThat(result.status()).isEqualTo("ACTIVE");
        assertThat(result.createdAt()).isEqualTo(now);
        assertThat(found.state().id()).isEqualTo(userId);
        assertThat(found.state().tenantId()).isEqualTo(tenantId);
        assertThat(found.state().status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(found.state().version()).isEqualTo(0L);
    }

    @Test
    void create_user_handler_rejects_duplicate_user_found_in_database() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        userRepository.save(User.create(userId, tenantId, "Alice", now));
        flushAndClear();

        tenantContract.addActiveTenant(tenantId);

        assertThatThrownBy(() -> createUserHandler.handle(new CreateUserCommand(userId, tenantId, "Alice")))
                .isInstanceOf(DuplicateUserException.class);
    }

    @Test
    void create_user_handler_rejects_inactive_tenant_before_save() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        assertThatThrownBy(() -> createUserHandler.handle(new CreateUserCommand(userId, tenantId, "Alice")))
                .isInstanceOf(InvalidInputException.class);

        flushAndClear();
        assertThat(userRepository.findByTenantIdAndId(tenantId, userId)).isEmpty();
    }

    @Test
    void user_contract_service_reads_active_user_state_from_database() {
        UUID tenantId = UUID.randomUUID();
        UUID activeUserId = UUID.randomUUID();
        UUID disabledUserId = UUID.randomUUID();
        UUID otherTenantUserId = UUID.randomUUID();
        UUID missingUserId = UUID.randomUUID();
        Instant now = Instant.parse("2026-06-19T00:00:00Z");

        userRepository.save(User.create(activeUserId, tenantId, "Active", now));
        User disabled = User.create(disabledUserId, tenantId, "Disabled", now)
                .disable(now.plusSeconds(60));
        userRepository.save(disabled);
        userRepository.save(User.create(otherTenantUserId, UUID.randomUUID(), "Other", now));
        flushAndClear();

        UserInfo userInfo = userContractService.getUserInfo(activeUserId);

        assertThat(userContractService.existsUserInTenant(activeUserId, tenantId)).isTrue();
        assertThat(userContractService.existsUserInTenant(disabledUserId, tenantId)).isFalse();
        assertThat(userContractService.findActiveUserIdsInTenant(
                tenantId,
                Set.of(activeUserId, disabledUserId, otherTenantUserId, missingUserId)
        )).containsExactly(activeUserId);
        assertThat(userInfo.userId()).isEqualTo(activeUserId);
        assertThat(userInfo.tenantId()).isEqualTo(tenantId);
        assertThat(userInfo.name()).isEqualTo("Active");
        assertThatThrownBy(() -> userContractService.getUserInfo(disabledUserId))
                .isInstanceOf(UserNotFoundException.class);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @TestConfiguration
    static class TestPorts {

        @Bean
        MutableTenantContract tenantContract() {
            return new MutableTenantContract();
        }

        @Bean
        MutableClockPort clockPort() {
            return new MutableClockPort();
        }
    }

    static class MutableTenantContract implements TenantContract {
        private final Set<UUID> activeTenantIds = new HashSet<>();

        void clear() {
            activeTenantIds.clear();
        }

        void addActiveTenant(UUID tenantId) {
            activeTenantIds.add(tenantId);
        }

        @Override
        public boolean existsActiveTenant(UUID tenantId) {
            return activeTenantIds.contains(tenantId);
        }
    }

    static class MutableClockPort implements ClockPort {
        private Instant now = Instant.parse("2026-06-19T00:00:00Z");

        void setNow(Instant now) {
            this.now = Objects.requireNonNull(now, "now must not be null");
        }

        @Override
        public Instant now() {
            return now;
        }
    }
}
