package com.polarishb.pabal.workspace.infrastructure.application;

import com.polarishb.pabal.common.contract.TenantContract;
import com.polarishb.pabal.common.contract.UserContract;
import com.polarishb.pabal.common.contract.dto.UserInfo;
import com.polarishb.pabal.common.exception.InvalidInputException;
import com.polarishb.pabal.support.AbstractWorkspacePostgresDataJpaTest;
import com.polarishb.pabal.workspace.application.command.handler.CreateWorkspaceCommandHandler;
import com.polarishb.pabal.workspace.application.command.input.CreateWorkspaceCommand;
import com.polarishb.pabal.workspace.application.command.output.CreateWorkspaceResult;
import com.polarishb.pabal.workspace.application.port.out.persistence.WorkspaceMemberRepository;
import com.polarishb.pabal.workspace.application.port.out.persistence.WorkspaceRepository;
import com.polarishb.pabal.workspace.application.port.out.time.ClockPort;
import com.polarishb.pabal.workspace.contract.persistence.PersistedWorkspace;
import com.polarishb.pabal.workspace.domain.model.type.WorkspaceStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import({
        CreateWorkspaceCommandHandler.class,
        WorkspaceApplicationIntegrationTest.TestPorts.class
})
class WorkspaceApplicationIntegrationTest extends AbstractWorkspacePostgresDataJpaTest {

    @Autowired
    private CreateWorkspaceCommandHandler createWorkspaceHandler;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Autowired
    private MutableTenantContract tenantContract;

    @Autowired
    private MutableUserContract userContract;

    @Autowired
    private MutableClockPort clockPort;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void resetTestPorts() {
        tenantContract.clear();
        userContract.clear();
        clockPort.setNow(Instant.parse("2026-06-19T00:00:00Z"));
    }

    @Test
    void create_workspace_handler_persists_workspace_and_owner_member_through_repository_ports() {
        UUID tenantId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        tenantContract.addActiveTenant(tenantId);
        userContract.addUser(ownerId, new UserInfo(ownerId, "Alice", tenantId));
        clockPort.setNow(now);

        CreateWorkspaceResult result = createWorkspaceHandler.handle(
                new CreateWorkspaceCommand(tenantId, ownerId, "Engineering")
        );

        flushAndClear();
        PersistedWorkspace workspace = workspaceRepository.findByTenantIdAndId(
                tenantId,
                result.workspaceId()
        ).orElseThrow();
        Set<UUID> activeOwnerIds = workspaceMemberRepository.findActiveUserIds(
                tenantId,
                result.workspaceId(),
                Set.of(ownerId)
        );

        assertThat(result.tenantId()).isEqualTo(tenantId);
        assertThat(result.name()).isEqualTo("Engineering");
        assertThat(result.status()).isEqualTo("ACTIVE");
        assertThat(result.ownerId()).isEqualTo(ownerId);
        assertThat(result.createdAt()).isEqualTo(now);
        assertThat(workspace.state().id()).isEqualTo(result.workspaceId());
        assertThat(workspace.state().status()).isEqualTo(WorkspaceStatus.ACTIVE);
        assertThat(workspace.state().version()).isEqualTo(0L);
        assertThat(workspaceRepository.existsActiveByTenantIdAndId(tenantId, result.workspaceId())).isTrue();
        assertThat(workspaceMemberRepository.existsActiveMember(tenantId, result.workspaceId(), ownerId)).isTrue();
        assertThat(activeOwnerIds).containsExactly(ownerId);
    }

    @Test
    void create_workspace_handler_rejects_inactive_tenant_before_save() {
        UUID tenantId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        assertThatThrownBy(() -> createWorkspaceHandler.handle(
                new CreateWorkspaceCommand(tenantId, ownerId, "Engineering")
        )).isInstanceOf(InvalidInputException.class);

        flushAndClear();
        assertThat(workspaceRowCount()).isZero();
        assertThat(workspaceMemberRowCount()).isZero();
    }

    @Test
    void create_workspace_handler_rejects_owner_outside_tenant_before_save() {
        UUID tenantId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        tenantContract.addActiveTenant(tenantId);

        assertThatThrownBy(() -> createWorkspaceHandler.handle(
                new CreateWorkspaceCommand(tenantId, ownerId, "Engineering")
        )).isInstanceOf(InvalidInputException.class);

        flushAndClear();
        assertThat(workspaceRowCount()).isZero();
        assertThat(workspaceMemberRowCount()).isZero();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private long workspaceRowCount() {
        return entityManager.createQuery("select count(w) from WorkspaceEntity w", Long.class)
                .getSingleResult();
    }

    private long workspaceMemberRowCount() {
        return entityManager.createQuery("select count(m) from WorkspaceMemberEntity m", Long.class)
                .getSingleResult();
    }

    @TestConfiguration
    static class TestPorts {

        @Bean
        MutableTenantContract tenantContract() {
            return new MutableTenantContract();
        }

        @Bean
        MutableUserContract userContract() {
            return new MutableUserContract();
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

    static class MutableUserContract implements UserContract {
        private final Map<UUID, UserInfo> usersById = new HashMap<>();

        void clear() {
            usersById.clear();
        }

        void addUser(UUID userId, UserInfo userInfo) {
            usersById.put(userId, userInfo);
        }

        @Override
        public boolean existsUserInTenant(UUID userId, UUID tenantId) {
            UserInfo userInfo = usersById.get(userId);
            return userInfo != null && userInfo.tenantId().equals(tenantId);
        }

        @Override
        public Set<UUID> findActiveUserIdsInTenant(UUID tenantId, Set<UUID> userIds) {
            Set<UUID> activeUserIds = new HashSet<>();
            for (UUID userId : userIds) {
                if (existsUserInTenant(userId, tenantId)) {
                    activeUserIds.add(userId);
                }
            }
            return activeUserIds;
        }

        @Override
        public UserInfo getUserInfo(UUID userId) {
            return Optional.ofNullable(usersById.get(userId))
                    .orElseThrow();
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
