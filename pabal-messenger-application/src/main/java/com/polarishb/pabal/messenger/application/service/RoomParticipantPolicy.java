package com.polarishb.pabal.messenger.application.service;

import com.polarishb.pabal.messenger.application.port.out.identity.RoomParticipantDirectoryPort;
import com.polarishb.pabal.messenger.domain.exception.RoomParticipantNotInvitableException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class RoomParticipantPolicy {

    private final RoomParticipantDirectoryPort participantDirectoryPort;
    private final ChatRoomAuthorizationService authorizationService;

    public List<UUID> validateGroupParticipants(
            UUID tenantId,
            UUID requesterId,
            List<UUID> participantIds
    ) {
        List<UUID> participants = normalizeParticipants(requesterId, participantIds);
        if (!participants.isEmpty()) {
            authorizationService.requireRoomInvite(tenantId, requesterId);
        }

        Set<UUID> requestedMemberIds = memberIds(requesterId, participants);
        Set<UUID> tenantMemberIds = participantDirectoryPort.findTenantMemberIds(tenantId, requestedMemberIds);
        validateAllInvitable(tenantId, null, requestedMemberIds, tenantMemberIds);
        return participants;
    }

    public List<UUID> validateChannelParticipants(
            UUID tenantId,
            UUID workspaceId,
            UUID requesterId,
            List<UUID> participantIds
    ) {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        List<UUID> participants = normalizeParticipants(requesterId, participantIds);
        if (!participants.isEmpty()) {
            authorizationService.requireChannelInvite(tenantId, requesterId, workspaceId);
        }

        Set<UUID> requestedMemberIds = memberIds(requesterId, participants);
        Set<UUID> workspaceMemberIds = participantDirectoryPort.findWorkspaceMemberIds(
                tenantId,
                workspaceId,
                requestedMemberIds
        );
        validateAllInvitable(tenantId, workspaceId, requestedMemberIds, workspaceMemberIds);
        return participants;
    }

    public UUID validateDirectParticipant(UUID tenantId, UUID requesterId, UUID participantId) {
        UUID requiredParticipantId = Objects.requireNonNull(participantId, "participantId must not be null");
        Set<UUID> requestedMemberIds = memberIds(requesterId, List.of(requiredParticipantId));
        Set<UUID> tenantMemberIds = participantDirectoryPort.findTenantMemberIds(tenantId, requestedMemberIds);
        validateAllInvitable(tenantId, null, requestedMemberIds, tenantMemberIds);
        return requiredParticipantId;
    }

    private List<UUID> normalizeParticipants(UUID requesterId, List<UUID> participantIds) {
        UUID requiredRequesterId = Objects.requireNonNull(requesterId, "requesterId must not be null");
        Objects.requireNonNull(participantIds, "participantIds must not be null");

        return participantIds.stream()
                .map(participantId -> Objects.requireNonNull(participantId, "participantId must not be null"))
                .filter(participantId -> !participantId.equals(requiredRequesterId))
                .distinct()
                .toList();
    }

    private Set<UUID> memberIds(UUID requesterId, List<UUID> participantIds) {
        return Stream.concat(Stream.of(Objects.requireNonNull(requesterId)), participantIds.stream())
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
    }

    private void validateAllInvitable(
            UUID tenantId,
            UUID workspaceId,
            Set<UUID> requestedMemberIds,
            Set<UUID> invitableMemberIds
    ) {
        Set<UUID> missingMemberIds = new LinkedHashSet<>(requestedMemberIds);
        missingMemberIds.removeAll(invitableMemberIds == null ? Set.of() : invitableMemberIds);

        if (!missingMemberIds.isEmpty()) {
            throw new RoomParticipantNotInvitableException(tenantId, workspaceId, missingMemberIds);
        }
    }
}
