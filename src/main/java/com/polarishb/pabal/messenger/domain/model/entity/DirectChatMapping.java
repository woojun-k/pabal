package com.polarishb.pabal.messenger.domain.model.entity;

import com.polarishb.pabal.messenger.domain.exception.InvalidDirectChatParticipantsException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DirectChatMapping {

    @EqualsAndHashCode.Include
    private UUID id;
    private UUID tenantId;
    private UUID chatRoomId;
    private UUID userIdMin;
    private UUID userIdMax;
    
    private Instant createdAt;
    private Instant updatedAt;

    public static DirectChatMapping create(
            UUID tenantId,
            UUID chatRoomId,
            UUID userId1,
            UUID userId2,
            Instant createdAt
    ) {
        validateParticipants(userId1, userId2);

        int comparison = userId1.compareTo(userId2);
        UUID userIdMin = comparison < 0 ? userId1 : userId2;
        UUID userIdMax = comparison < 0 ? userId2 : userId1;

        Instant now = Objects.requireNonNull(createdAt);
        return new DirectChatMapping(
                null,
                tenantId,
                chatRoomId,
                userIdMin,
                userIdMax,
                now,
                now
        );
    }

    public static void validateParticipants(UUID requesterId, UUID participantId) {
        UUID requiredRequesterId = Objects.requireNonNull(requesterId);
        UUID requiredParticipantId = Objects.requireNonNull(participantId);
        if (requiredRequesterId.equals(requiredParticipantId)) {
            throw new InvalidDirectChatParticipantsException(requiredRequesterId, requiredParticipantId);
        }
    }

    public static DirectChatMapping reconstitute(
            UUID id,
            UUID tenantId,
            UUID chatRoomId,
            UUID userIdMin,
            UUID userIdMax,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new DirectChatMapping(
                id,
                tenantId,
                chatRoomId,
                userIdMin,
                userIdMax,
                createdAt,
                updatedAt
        );
    }
}
