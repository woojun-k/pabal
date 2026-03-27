package com.polarishb.pabal.messenger.application.command.handler;

import com.polarishb.pabal.common.cqrs.CommandHandler;
import com.polarishb.pabal.messenger.application.command.input.GetOrCreateDirectRoomCommand;
import com.polarishb.pabal.messenger.application.command.output.CreateRoomResult;
import com.polarishb.pabal.messenger.application.service.DirectRoomCreationService;
import com.polarishb.pabal.messenger.contract.persistence.directchatmapping.PersistedDirectChatMapping;
import com.polarishb.pabal.messenger.domain.exception.DirectChatMappingNotFoundException;
import com.polarishb.pabal.messenger.domain.exception.DuplicateDirectChatMappingException;
import com.polarishb.pabal.messenger.domain.repository.DirectChatMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GetOrCreateDirectRoomCommandHandler implements CommandHandler<GetOrCreateDirectRoomCommand, CreateRoomResult> {

    private final DirectChatMappingRepository directChatMappingRepository;
    private final DirectRoomCreationService directRoomCreationService;

    @Override
    public CreateRoomResult handle(GetOrCreateDirectRoomCommand command) {

        Optional<PersistedDirectChatMapping> existing = directChatMappingRepository
                .findByTenantIdAndUserIds(command.tenantId(), command.requesterId(), command.participantId());

        if (existing.isPresent()) {
            return new CreateRoomResult(existing.get().mapping().getChatRoomId(), null);
        }

        try {
            UUID chatRoomId = directRoomCreationService.create(command);
            return new CreateRoomResult(chatRoomId, null);
        } catch (DuplicateDirectChatMappingException e) {
            PersistedDirectChatMapping persisted = directChatMappingRepository
                    .findByTenantIdAndUserIds(command.tenantId(), command.requesterId(), command.participantId())
                    .orElseThrow(DirectChatMappingNotFoundException::new);

            return new CreateRoomResult(persisted.mapping().getChatRoomId(), null);
        }
    }
}
