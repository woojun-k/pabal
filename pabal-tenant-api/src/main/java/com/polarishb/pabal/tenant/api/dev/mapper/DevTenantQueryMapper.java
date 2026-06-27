package com.polarishb.pabal.tenant.api.dev.mapper;

import com.polarishb.pabal.tenant.api.dev.http.response.TenantDevResponse;
import com.polarishb.pabal.tenant.application.query.input.GetTenantQuery;
import com.polarishb.pabal.tenant.application.query.output.TenantDto;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Profile({"local", "test"})
public class DevTenantQueryMapper {

    public GetTenantQuery toGetTenantQuery(UUID tenantId) {
        return new GetTenantQuery(tenantId);
    }

    public TenantDevResponse toTenantDevResponse(TenantDto dto) {
        return new TenantDevResponse(
                dto.tenantId(),
                dto.name(),
                dto.status(),
                dto.createdAt(),
                dto.updatedAt()
        );
    }
}
