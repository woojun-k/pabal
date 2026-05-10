package com.polarishb.pabal.messenger.application.command.handler;

import com.polarishb.pabal.common.cqrs.CommandHandler;
import com.polarishb.pabal.messenger.application.command.input.GetOrCreateDirectRoomCommand;
import com.polarishb.pabal.messenger.application.command.output.GetOrCreateDirectRoomResult;
import com.polarishb.pabal.messenger.application.service.DirectRoomCreationService;
import com.polarishb.pabal.messenger.contract.persistence.directchatmapping.PersistedDirectChatMapping;
import com.polarishb.pabal.messenger.domain.exception.DirectChatMappingNotFoundException;
import com.polarishb.pabal.messenger.domain.exception.DuplicateDirectChatMappingException;
import com.polarishb.pabal.messenger.application.port.out.persistence.DirectChatMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GetOrCreateDirectRoomCommandHandler implements CommandHandler<GetOrCreateDirectRoomCommand, GetOrCreateDirectRoomResult> {

    private final DirectChatMappingRepository directChatMappingRepository;
    private final DirectRoomCreationService directRoomCreationService;

    @Override
    public GetOrCreateDirectRoomResult handle(GetOrCreateDirectRoomCommand command) {

        Optional<PersistedDirectChatMapping> existing = directChatMappingRepository
                .findByTenantIdAndUserIds(command.tenantId(), command.requesterId(), command.participantId());

        if (existing.isPresent()) {
            return new GetOrCreateDirectRoomResult(existing.get().mapping().getChatRoomId());
        }

        try {
            UUID chatRoomId = directRoomCreationService.create(command);
            return new GetOrCreateDirectRoomResult(chatRoomId);
        } catch (DuplicateDirectChatMappingException e) {
            PersistedDirectChatMapping persisted = directChatMappingRepository
                    .findByTenantIdAndUserIds(command.tenantId(), command.requesterId(), command.participantId())
                    .orElseThrow(DirectChatMappingNotFoundException::new);

            return new GetOrCreateDirectRoomResult(persisted.mapping().getChatRoomId());
        }
    }
}
