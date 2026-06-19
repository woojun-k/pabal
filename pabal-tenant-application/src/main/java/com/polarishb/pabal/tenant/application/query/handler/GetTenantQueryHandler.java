package com.polarishb.pabal.tenant.application.query.handler;

import com.polarishb.pabal.common.cqrs.QueryHandler;
import com.polarishb.pabal.tenant.application.port.out.persistence.TenantRepository;
import com.polarishb.pabal.tenant.application.query.input.GetTenantQuery;
import com.polarishb.pabal.tenant.application.query.output.TenantDto;
import com.polarishb.pabal.tenant.contract.persistence.PersistedTenant;
import com.polarishb.pabal.tenant.domain.exception.TenantNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class GetTenantQueryHandler implements QueryHandler<GetTenantQuery, TenantDto> {

    private final TenantRepository tenantRepository;

    @Override
    @Transactional(readOnly = true)
    public TenantDto handle(GetTenantQuery query) {
        PersistedTenant tenant = tenantRepository.findById(query.tenantId())
                .orElseThrow(() -> new TenantNotFoundException(query.tenantId()));
        return new TenantDto(
                tenant.state().id(),
                tenant.state().name(),
                tenant.state().status().name(),
                tenant.state().createdAt(),
                tenant.state().updatedAt()
        );
    }
}
