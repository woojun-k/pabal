package com.polarishb.pabal.tenant.api.query.mapper;

import com.polarishb.pabal.tenant.api.query.http.response.TenantResponse;
import com.polarishb.pabal.tenant.application.query.input.GetTenantQuery;
import com.polarishb.pabal.tenant.application.query.output.TenantDto;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TenantQueryMapper {

    public GetTenantQuery toGetTenantQuery(UUID tenantId) {
        return new GetTenantQuery(tenantId);
    }

    public TenantResponse toTenantResponse(TenantDto tenant) {
        return new TenantResponse(
                tenant.tenantId(),
                tenant.name(),
                tenant.status(),
                tenant.createdAt(),
                tenant.updatedAt()
        );
    }
}
