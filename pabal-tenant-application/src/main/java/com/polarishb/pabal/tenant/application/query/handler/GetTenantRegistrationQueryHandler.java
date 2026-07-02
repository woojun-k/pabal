package com.polarishb.pabal.tenant.application.query.handler;

import com.polarishb.pabal.common.cqrs.QueryHandler;
import com.polarishb.pabal.tenant.application.port.out.persistence.TenantRegistrationRepository;
import com.polarishb.pabal.tenant.application.query.input.GetTenantRegistrationQuery;
import com.polarishb.pabal.tenant.application.query.output.TenantRegistrationDto;
import com.polarishb.pabal.tenant.contract.persistence.TenantRegistrationState;
import com.polarishb.pabal.tenant.domain.exception.TenantRegistrationNotFoundException;
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
        TenantRegistrationState registration = tenantRegistrationRepository.findStateById(query.registrationId())
                .orElseThrow(() -> new TenantRegistrationNotFoundException(query.registrationId()));

        return new TenantRegistrationDto(
                registration.id(),
                registration.tenantId(),
                registration.tenantName(),
                registration.domainName(),
                registration.status().name(),
                registration.verificationDnsName(),
                registration.verificationTxtValue(),
                registration.verificationExpiresAt(),
                registration.activationExpiresAt(),
                registration.verifiedAt(),
                registration.activatedAt(),
                registration.createdAt(),
                registration.updatedAt()
        );
    }
}
