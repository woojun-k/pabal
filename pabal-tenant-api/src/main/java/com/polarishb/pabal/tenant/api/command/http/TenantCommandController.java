package com.polarishb.pabal.tenant.api.command.http;

import com.polarishb.pabal.tenant.api.command.http.request.CreateTenantRequest;
import com.polarishb.pabal.tenant.api.command.http.response.CreateTenantResponse;
import com.polarishb.pabal.tenant.api.command.mapper.TenantCommandMapper;
import com.polarishb.pabal.tenant.application.command.handler.CreateTenantCommandHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TenantCommandController {

    private final TenantCommandMapper tenantCommandMapper;
    private final CreateTenantCommandHandler createTenantCommandHandler;

    @PostMapping("/tenants")
    public CreateTenantResponse createTenant(@Valid @RequestBody CreateTenantRequest request) {
        return tenantCommandMapper.toCreateTenantResponse(
                createTenantCommandHandler.handle(
                        tenantCommandMapper.toCreateTenantCommand(request)
                )
        );
    }
}
