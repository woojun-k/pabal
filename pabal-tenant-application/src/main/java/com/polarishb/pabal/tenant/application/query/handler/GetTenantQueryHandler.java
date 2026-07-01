package com.polarishb.pabal.tenant.application.query.handler;

import com.polarishb.pabal.common.cqrs.QueryHandler;
import com.polarishb.pabal.tenant.application.port.out.persistence.TenantRepository;
import com.polarishb.pabal.tenant.application.query.input.GetTenantQuery;
import com.polarishb.pabal.tenant.application.query.output.TenantDto;
import com.polarishb.pabal.tenant.contract.persistence.TenantState;
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
        TenantState tenant = tenantRepository.findStateById(query.tenantId())
                .orElseThrow(() -> new TenantNotFoundException(query.tenantId()));
        return new TenantDto(
                tenant.id(),
                tenant.name(),
                tenant.status().name(),
                tenant.createdAt(),
                tenant.updatedAt()
        );
    }
}
