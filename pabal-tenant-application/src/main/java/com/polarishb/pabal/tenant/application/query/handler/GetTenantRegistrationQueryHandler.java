package com.polarishb.pabal.tenant.application.query.handler;

import com.polarishb.pabal.common.cqrs.QueryHandler;
import com.polarishb.pabal.tenant.application.port.out.persistence.TenantRegistrationRepository;
import com.polarishb.pabal.tenant.application.query.input.GetTenantRegistrationQuery;
import com.polarishb.pabal.tenant.application.query.output.TenantRegistrationDto;
import com.polarishb.pabal.tenant.contract.persistence.TenantRegistrationPersistenceMapper;
import com.polarishb.pabal.tenant.contract.persistence.TenantRegistrationState;
import com.polarishb.pabal.tenant.domain.exception.TenantRegistrationNotFoundException;
import com.polarishb.pabal.tenant.domain.model.TenantRegistration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class GetTenantRegistrationQueryHandler implements QueryHandler<GetTenantRegistrationQuery, TenantRegistrationDto> {

    private final TenantRegistrationRepository tenantRegistrationRepository;

    @Override
    @Transactional(readOnly = true)
    public TenantRegistrationDto handle(GetTenantRegistrationQuery query) {
        TenantRegistrationState state = tenantRegistrationRepository.findStateById(query.registrationId())
                .orElseThrow(() -> new TenantRegistrationNotFoundException(query.registrationId()));
        TenantRegistration registration = TenantRegistrationPersistenceMapper.toDomain(state);

        return new TenantRegistrationDto(
                state.id(),
                state.tenantId(),
                state.tenantName(),
                state.domainName(),
                state.status().name(),
                registration.verificationDnsName(),
                registration.verificationTxtValue(),
                state.verificationExpiresAt(),
                state.activationExpiresAt(),
                state.verifiedAt(),
                state.activatedAt(),
                state.createdAt(),
                state.updatedAt()
        );
    }
}
