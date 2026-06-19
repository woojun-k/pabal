package com.polarishb.pabal.user.application.service;

import com.polarishb.pabal.common.contract.dto.UserInfo;
import com.polarishb.pabal.user.application.port.out.persistence.UserRepository;
import com.polarishb.pabal.user.contract.persistence.PersistedUser;
import com.polarishb.pabal.user.contract.persistence.UserPersistenceMapper;
import com.polarishb.pabal.user.domain.exception.UserNotFoundException;
import com.polarishb.pabal.user.domain.model.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserContractServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserContractService service = new UserContractService(userRepository);

    @Test
    void existsUserInTenant_delegates_to_active_tenant_user_lookup() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(userRepository.existsActiveByTenantIdAndId(tenantId, userId)).thenReturn(true);

        assertThat(service.existsUserInTenant(userId, tenantId)).isTrue();
    }

    @Test
    void findActiveUserIdsInTenant_delegates_to_bulk_active_tenant_user_lookup() {
        UUID tenantId = UUID.randomUUID();
        UUID activeUserId = UUID.randomUUID();
        UUID disabledUserId = UUID.randomUUID();
        Set<UUID> requestedUserIds = Set.of(activeUserId, disabledUserId);
        when(userRepository.findActiveIdsByTenantIdAndIds(tenantId, requestedUserIds))
                .thenReturn(Set.of(activeUserId));

        Set<UUID> activeUserIds = service.findActiveUserIdsInTenant(tenantId, requestedUserIds);

        assertThat(activeUserIds).containsExactly(activeUserId);
    }

    @Test
    void getUserInfo_returns_active_user_info() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User user = User.create(userId, tenantId, "Alice", Instant.parse("2026-04-08T00:00:00Z"));
        when(userRepository.findById(userId)).thenReturn(Optional.of(persisted(user, 0L)));

        UserInfo userInfo = service.getUserInfo(userId);

        assertThat(userInfo.userId()).isEqualTo(userId);
        assertThat(userInfo.tenantId()).isEqualTo(tenantId);
        assertThat(userInfo.name()).isEqualTo("Alice");
    }

    @Test
    void getUserInfo_rejects_disabled_user() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User user = User.create(userId, tenantId, "Alice", Instant.parse("2026-04-08T00:00:00Z"));
        user.disable(Instant.parse("2026-04-08T00:10:00Z"));
        when(userRepository.findById(userId)).thenReturn(Optional.of(persisted(user, 1L)));

        assertThatThrownBy(() -> service.getUserInfo(userId))
                .isInstanceOf(UserNotFoundException.class);
    }

    private PersistedUser persisted(User user, Long version) {
        return new PersistedUser(user, UserPersistenceMapper.toState(user, version));
    }
}
