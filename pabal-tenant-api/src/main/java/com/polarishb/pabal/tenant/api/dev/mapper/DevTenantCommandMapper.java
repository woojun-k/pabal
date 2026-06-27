package com.polarishb.pabal.tenant.api.dev.mapper;

import com.polarishb.pabal.tenant.api.dev.http.request.CreateTenantDevRequest;
import com.polarishb.pabal.tenant.api.dev.http.response.CreateTenantDevResponse;
import com.polarishb.pabal.tenant.application.command.input.CreateTenantCommand;
import com.polarishb.pabal.tenant.application.command.output.CreateTenantResult;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "test"})
public class DevTenantCommandMapper {

    public CreateTenantCommand toCreateTenantCommand(CreateTenantDevRequest request) {
        return new CreateTenantCommand(request.name());
    }

    public CreateTenantDevResponse toCreateTenantDevResponse(CreateTenantResult result) {
        return new CreateTenantDevResponse(
                result.tenantId(),
                result.name(),
                result.status(),
                result.createdAt()
        );
    }
}
