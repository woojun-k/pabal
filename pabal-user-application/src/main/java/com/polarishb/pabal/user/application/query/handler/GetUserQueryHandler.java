package com.polarishb.pabal.user.application.query.handler;

import com.polarishb.pabal.common.cqrs.QueryHandler;
import com.polarishb.pabal.user.application.port.out.persistence.UserRepository;
import com.polarishb.pabal.user.application.query.input.GetUserQuery;
import com.polarishb.pabal.user.application.query.output.UserDto;
import com.polarishb.pabal.user.contract.persistence.PersistedUser;
import com.polarishb.pabal.user.domain.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class GetUserQueryHandler implements QueryHandler<GetUserQuery, UserDto> {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDto handle(GetUserQuery query) {
        PersistedUser persistedUser = userRepository.findByTenantIdAndId(query.tenantId(), query.userId())
                .filter(user -> user.user().isActive())
                .orElseThrow(() -> new UserNotFoundException(query.userId()));

        return new UserDto(
                persistedUser.state().id(),
                persistedUser.state().tenantId(),
                persistedUser.state().name(),
                persistedUser.state().status().name(),
                persistedUser.state().createdAt(),
                persistedUser.state().updatedAt()
        );
    }
}
