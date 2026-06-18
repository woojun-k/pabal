package com.polarishb.pabal.user.application.command.handler;

import com.polarishb.pabal.common.cqrs.CommandHandler;
import com.polarishb.pabal.user.application.command.input.CreateUserCommand;
import com.polarishb.pabal.user.application.command.output.CreateUserResult;
import com.polarishb.pabal.user.application.port.out.persistence.UserRepository;
import com.polarishb.pabal.user.application.port.out.time.ClockPort;
import com.polarishb.pabal.user.contract.persistence.PersistedUser;
import com.polarishb.pabal.user.domain.exception.DuplicateUserException;
import com.polarishb.pabal.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class CreateUserCommandHandler implements CommandHandler<CreateUserCommand, CreateUserResult> {

    private final UserRepository userRepository;
    private final ClockPort clockPort;

    @Override
    @Transactional
    public CreateUserResult handle(CreateUserCommand command) {
        userRepository.findById(command.userId())
                .ifPresent(existing -> {
                    throw new DuplicateUserException(existing.state().id());
                });

        Instant now = clockPort.now();
        User user = User.create(
                command.userId(),
                command.tenantId(),
                command.name(),
                now
        );

        PersistedUser saved = userRepository.save(user);
        return new CreateUserResult(
                saved.state().id(),
                saved.state().tenantId(),
                saved.state().name(),
                saved.state().status().name(),
                saved.state().createdAt()
        );
    }
}
