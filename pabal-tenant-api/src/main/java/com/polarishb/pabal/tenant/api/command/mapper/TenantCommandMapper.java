package com.polarishb.pabal.tenant.api.command.mapper;

import com.polarishb.pabal.tenant.api.command.http.request.CreateTenantRequest;
import com.polarishb.pabal.tenant.api.command.http.response.CreateTenantResponse;
import com.polarishb.pabal.tenant.application.command.input.CreateTenantCommand;
import com.polarishb.pabal.tenant.application.command.output.CreateTenantResult;
import org.springframework.stereotype.Component;

@Component
public class TenantCommandMapper {

    public CreateTenantCommand toCreateTenantCommand(CreateTenantRequest request) {
        return new CreateTenantCommand(request.name());
    }

    public CreateTenantResponse toCreateTenantResponse(CreateTenantResult result) {
        return new CreateTenantResponse(
                result.tenantId(),
                result.name(),
                result.status(),
                result.createdAt()
        );
    }
}
