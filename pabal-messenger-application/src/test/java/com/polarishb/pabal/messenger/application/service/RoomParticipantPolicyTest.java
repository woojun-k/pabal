package com.polarishb.pabal.messenger.application.service;

import com.polarishb.pabal.messenger.application.port.out.identity.RoomParticipantDirectoryPort;
import com.polarishb.pabal.messenger.domain.exception.RoomParticipantNotInvitableException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomParticipantPolicyTest {

    @Mock
    private RoomParticipantDirectoryPort participantDirectoryPort;

    @Mock
    private ChatRoomAuthorizationService authorizationService;

    @InjectMocks
    private RoomParticipantPolicy policy;

    @Test
    void validateGroupParticipants_requires_room_invite_and_checks_members_in_batch() {
        UUID tenantId = uuid(100);
        UUID requesterId = uuid(1);
        UUID participant1 = uuid(2);
        UUID participant2 = uuid(3);

        when(participantDirectoryPort.findTenantMemberIds(eq(tenantId), anySet()))
                .thenReturn(Set.of(requesterId, participant1, participant2));

        List<UUID> participants = policy.validateGroupParticipants(
                tenantId,
                requesterId,
                List.of(participant1, requesterId, participant1, participant2)
        );

        assertThat(participants).containsExactly(participant1, participant2);
        verify(authorizationService).requireRoomInvite(tenantId, requesterId);
        verify(participantDirectoryPort).findTenantMemberIds(
                tenantId,
                Set.of(requesterId, participant1, participant2)
        );
    }

    @Test
    void validateChannelParticipants_requires_channel_invite_and_workspace_membership() {
        UUID tenantId = uuid(100);
        UUID workspaceId = uuid(200);
        UUID requesterId = uuid(1);
        UUID participantId = uuid(2);

        when(participantDirectoryPort.findWorkspaceMemberIds(eq(tenantId), eq(workspaceId), anySet()))
                .thenReturn(Set.of(requesterId, participantId));

        List<UUID> participants = policy.validateChannelParticipants(
                tenantId,
                workspaceId,
                requesterId,
                List.of(participantId)
        );

        assertThat(participants).containsExactly(participantId);
        verify(authorizationService).requireChannelInvite(tenantId, requesterId, workspaceId);
        verify(participantDirectoryPort).findWorkspaceMemberIds(
                tenantId,
                workspaceId,
                Set.of(requesterId, participantId)
        );
    }

    @Test
    void validateChannelParticipants_checks_requester_membership_even_without_invitees() {
        UUID tenantId = uuid(100);
        UUID workspaceId = uuid(200);
        UUID requesterId = uuid(1);

        when(participantDirectoryPort.findWorkspaceMemberIds(eq(tenantId), eq(workspaceId), anySet()))
                .thenReturn(Set.of(requesterId));

        List<UUID> participants = policy.validateChannelParticipants(
                tenantId,
                workspaceId,
                requesterId,
                List.of()
        );

        assertThat(participants).isEmpty();
        verifyNoInteractions(authorizationService);
        verify(participantDirectoryPort).findWorkspaceMemberIds(tenantId, workspaceId, Set.of(requesterId));
    }

    @Test
    void validateDirectParticipant_rejects_unknown_or_cross_tenant_member() {
        UUID tenantId = uuid(100);
        UUID requesterId = uuid(1);
        UUID participantId = uuid(2);

        when(participantDirectoryPort.findTenantMemberIds(eq(tenantId), anySet()))
                .thenReturn(Set.of(requesterId));

        assertThatThrownBy(() -> policy.validateDirectParticipant(tenantId, requesterId, participantId))
                .isInstanceOf(RoomParticipantNotInvitableException.class);

        verify(participantDirectoryPort).findTenantMemberIds(tenantId, Set.of(requesterId, participantId));
    }

    private UUID uuid(int value) {
        return UUID.fromString("%08d-0000-0000-0000-000000000000".formatted(value));
    }
}
