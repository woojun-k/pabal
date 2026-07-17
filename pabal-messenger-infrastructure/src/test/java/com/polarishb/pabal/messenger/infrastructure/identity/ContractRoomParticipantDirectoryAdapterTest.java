package com.polarishb.pabal.messenger.infrastructure.identity;

import com.polarishb.pabal.integration.contract.UserContract;
import com.polarishb.pabal.integration.contract.WorkspaceContract;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ContractRoomParticipantDirectoryAdapterTest {

    @Test
    void existsActiveTenantMember_delegates_to_user_contract() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserContract userContract = mock(UserContract.class);
        WorkspaceContract workspaceContract = mock(WorkspaceContract.class);
        when(userContract.existsUserInTenant(userId, tenantId)).thenReturn(true);
        ContractRoomParticipantDirectoryAdapter adapter = new ContractRoomParticipantDirectoryAdapter(
                userContract,
                workspaceContract
        );

        boolean exists = adapter.existsActiveTenantMember(tenantId, userId);

        assertThat(exists).isTrue();
        verify(userContract).existsUserInTenant(userId, tenantId);
        verifyNoInteractions(workspaceContract);
    }

    @Test
    void findTenantMemberIds_uses_user_contract_for_all_requested_users() {
        UUID tenantId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        UUID outsiderId = UUID.randomUUID();
        UserContract userContract = mock(UserContract.class);
        WorkspaceContract workspaceContract = mock(WorkspaceContract.class);
        Set<UUID> requestedUserIds = Set.of(requesterId, participantId, outsiderId);
        when(userContract.findActiveUserIdsInTenant(tenantId, requestedUserIds)).thenReturn(Set.of(participantId));
        ContractRoomParticipantDirectoryAdapter adapter = new ContractRoomParticipantDirectoryAdapter(
                userContract,
                workspaceContract
        );

        Set<UUID> memberIds = adapter.findTenantMemberIds(tenantId, requestedUserIds);

        assertThat(memberIds).containsExactly(participantId);
        verify(userContract).findActiveUserIdsInTenant(tenantId, requestedUserIds);
        verifyNoInteractions(workspaceContract);
    }

    @Test
    void findTenantMemberIds_returns_empty_without_user_contract_call_for_empty_request() {
        UUID tenantId = UUID.randomUUID();
        UserContract userContract = mock(UserContract.class);
        WorkspaceContract workspaceContract = mock(WorkspaceContract.class);
        ContractRoomParticipantDirectoryAdapter adapter = new ContractRoomParticipantDirectoryAdapter(
                userContract,
                workspaceContract
        );

        Set<UUID> memberIds = adapter.findTenantMemberIds(tenantId, Set.of());

        assertThat(memberIds).isEmpty();
        verifyNoInteractions(userContract, workspaceContract);
    }

    @Test
    void findWorkspaceMemberIds_uses_workspace_contract_for_all_requested_users() {
        UUID tenantId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        UUID outsiderId = UUID.randomUUID();
        UserContract userContract = mock(UserContract.class);
        WorkspaceContract workspaceContract = mock(WorkspaceContract.class);
        Set<UUID> requestedUserIds = Set.of(requesterId, participantId, outsiderId);
        when(workspaceContract.findActiveMemberIds(tenantId, workspaceId, requestedUserIds))
                .thenReturn(Set.of(requesterId, participantId));
        ContractRoomParticipantDirectoryAdapter adapter = new ContractRoomParticipantDirectoryAdapter(
                userContract,
                workspaceContract
        );

        Set<UUID> memberIds = adapter.findWorkspaceMemberIds(tenantId, workspaceId, requestedUserIds);

        assertThat(memberIds).containsExactlyInAnyOrder(requesterId, participantId);
        verify(workspaceContract).findActiveMemberIds(tenantId, workspaceId, requestedUserIds);
        verifyNoInteractions(userContract);
    }

    @Test
    void findWorkspaceMemberIds_returns_empty_without_workspace_contract_call_for_empty_request() {
        UUID tenantId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        UserContract userContract = mock(UserContract.class);
        WorkspaceContract workspaceContract = mock(WorkspaceContract.class);
        ContractRoomParticipantDirectoryAdapter adapter = new ContractRoomParticipantDirectoryAdapter(
                userContract,
                workspaceContract
        );

        Set<UUID> memberIds = adapter.findWorkspaceMemberIds(tenantId, workspaceId, Set.of());

        assertThat(memberIds).isEmpty();
        verifyNoInteractions(userContract, workspaceContract);
    }
}
