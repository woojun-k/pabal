package com.polarishb.pabal.messenger.infrastructure.identity;

import com.polarishb.pabal.common.contract.UserContract;
import com.polarishb.pabal.security.authentication.PabalPrincipal;
import com.polarishb.pabal.security.context.CurrentAuthentication;
import com.polarishb.pabal.security.context.CurrentAuthenticationProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CurrentAuthenticationRoomParticipantDirectoryAdapterTest {

    @Test
    void findTenantMemberIds_combines_current_principal_and_user_contract_results() {
        UUID tenantId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        UUID outsiderId = UUID.randomUUID();
        UserContract userContract = mock(UserContract.class);
        when(userContract.existsUserInTenant(participantId, tenantId)).thenReturn(true);
        ObjectProvider<UserContract> userContractProvider = userContractProvider(userContract);
        CurrentAuthenticationProvider authenticationProvider = authenticationProvider(tenantId, requesterId);
        CurrentAuthenticationRoomParticipantDirectoryAdapter adapter =
                new CurrentAuthenticationRoomParticipantDirectoryAdapter(authenticationProvider, userContractProvider);

        Set<UUID> memberIds = adapter.findTenantMemberIds(
                tenantId,
                Set.of(requesterId, participantId, outsiderId)
        );

        assertThat(memberIds).containsExactlyInAnyOrder(requesterId, participantId);
    }

    @Test
    void findWorkspaceMemberIds_fails_closed_to_current_principal_until_workspace_directory_exists() {
        UUID tenantId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        UserContract userContract = mock(UserContract.class);
        when(userContract.existsUserInTenant(participantId, tenantId)).thenReturn(true);
        ObjectProvider<UserContract> userContractProvider = userContractProvider(userContract);
        CurrentAuthenticationProvider authenticationProvider = authenticationProvider(tenantId, requesterId);
        CurrentAuthenticationRoomParticipantDirectoryAdapter adapter =
                new CurrentAuthenticationRoomParticipantDirectoryAdapter(authenticationProvider, userContractProvider);

        Set<UUID> memberIds = adapter.findWorkspaceMemberIds(
                tenantId,
                workspaceId,
                Set.of(requesterId, participantId)
        );

        assertThat(memberIds).containsExactly(requesterId);
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<UserContract> userContractProvider(UserContract userContract) {
        ObjectProvider<UserContract> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(userContract);
        return provider;
    }

    private CurrentAuthenticationProvider authenticationProvider(UUID tenantId, UUID userId) {
        PabalPrincipal principal = new PabalPrincipal(userId, tenantId, userId.toString());
        CurrentAuthentication authentication = new CurrentAuthentication(principal, Set.of());
        return () -> Optional.of(authentication);
    }
}
