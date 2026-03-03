package com.polarishb.pabal.messenger.application.command.handler;

import com.polarishb.pabal.common.cqrs.CommandHandler;
import com.polarishb.pabal.messenger.application.command.input.GetOrCreateDirectRoomCommand;
import com.polarishb.pabal.messenger.application.command.output.CreateRoomResult;
import com.polarishb.pabal.messenger.application.service.DirectRoomCreationService;
import com.polarishb.pabal.messenger.domain.exception.DuplicateDirectChatMappingException;
import com.polarishb.pabal.messenger.domain.model.entity.DirectChatMapping;
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

        Optional<DirectChatMapping> existing = directChatMappingRepository
                .findByTenantIdAndUserIds(command.tenantId(), command.requesterId(), command.participantId());

        if (existing.isPresent()) {
            return new CreateRoomResult(existing.get().getChatRoomId());
        }

        try {
            UUID chatRoomId = directRoomCreationService.create(command);
            return new CreateRoomResult(chatRoomId);
        } catch (DuplicateDirectChatMappingException e) {
            DirectChatMapping mapping = directChatMappingRepository
                    .findByTenantIdAndUserIds(command.tenantId(), command.requesterId(), command.participantId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Direct chat mapping duplicate detected, but existing mapping was not found.", e
                    ));

            return new CreateRoomResult(mapping.getChatRoomId());
        }
    }
}
