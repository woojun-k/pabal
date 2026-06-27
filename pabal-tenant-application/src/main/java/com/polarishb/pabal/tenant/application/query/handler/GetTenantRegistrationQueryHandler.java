package com.polarishb.pabal.tenant.application.query.handler;

import com.polarishb.pabal.common.cqrs.QueryHandler;
import com.polarishb.pabal.tenant.application.port.out.persistence.TenantRegistrationRepository;
import com.polarishb.pabal.tenant.application.query.input.GetTenantRegistrationQuery;
import com.polarishb.pabal.tenant.application.query.output.TenantRegistrationDto;
import com.polarishb.pabal.tenant.contract.persistence.PersistedTenantRegistration;
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
        TenantRegistration registration = tenantRegistrationRepository.findById(query.registrationId())
                .map(PersistedTenantRegistration::registration)
                .orElseThrow(() -> new TenantRegistrationNotFoundException(query.registrationId()));

        return new TenantRegistrationDto(
                registration.getId(),
                registration.getTenantId(),
                registration.getTenantName().value(),
                registration.getDomainName().value(),
                registration.getStatus().name(),
                registration.verificationDnsName(),
                registration.verificationTxtValue(),
                registration.getExpiresAt(),
                registration.getVerifiedAt(),
                registration.getActivatedAt(),
                registration.getCreatedAt(),
                registration.getUpdatedAt()
        );
    }
}
