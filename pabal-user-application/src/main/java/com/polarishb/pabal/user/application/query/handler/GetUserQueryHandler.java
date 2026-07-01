package com.polarishb.pabal.user.application.query.handler;

import com.polarishb.pabal.common.cqrs.QueryHandler;
import com.polarishb.pabal.user.application.port.out.persistence.UserRepository;
import com.polarishb.pabal.user.application.query.input.GetUserQuery;
import com.polarishb.pabal.user.application.query.output.UserDto;
import com.polarishb.pabal.user.contract.persistence.UserState;
import com.polarishb.pabal.user.domain.exception.UserNotFoundException;
import com.polarishb.pabal.user.domain.model.type.UserStatus;
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
        UserState user = userRepository.findStateByTenantIdAndId(query.tenantId(), query.userId())
                .filter(state -> state.status() == UserStatus.ACTIVE)
                .orElseThrow(() -> new UserNotFoundException(query.userId()));

        return new UserDto(
                user.id(),
                user.tenantId(),
                user.name(),
                user.status().name(),
                user.createdAt(),
                user.updatedAt()
        );
    }
}
