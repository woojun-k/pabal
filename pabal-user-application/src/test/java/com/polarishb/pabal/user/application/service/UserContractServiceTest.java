package com.polarishb.pabal.user.application.service;

import com.polarishb.pabal.user.application.port.out.persistence.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class UserContractServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);

    private UserContractService service;

    @BeforeEach
    void setUp() {
        service = new UserContractService(userRepository);
    }

    @Test
    void existsUserInTenant_delegates_to_tenant_scoped_repository_lookup() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(userRepository.existsActiveByTenantIdAndId(tenantId, userId)).thenReturn(true);

        assertThat(service.existsUserInTenant(userId, tenantId)).isTrue();

        verify(userRepository).existsActiveByTenantIdAndId(tenantId, userId);
    }

    @Test
    void findActiveUserIdsInTenant_delegates_with_tenant_id_and_requested_user_ids() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Set<UUID> requestedIds = Set.of(userId, UUID.randomUUID());
        when(userRepository.findActiveIdsByTenantIdAndIds(tenantId, requestedIds)).thenReturn(Set.of(userId));

        Set<UUID> result = service.findActiveUserIdsInTenant(tenantId, requestedIds);

        assertThat(result).containsExactly(userId);
        verify(userRepository).findActiveIdsByTenantIdAndIds(tenantId, requestedIds);
    }
}
