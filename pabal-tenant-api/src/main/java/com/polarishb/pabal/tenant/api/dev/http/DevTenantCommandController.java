package com.polarishb.pabal.tenant.api.dev.http;

import com.polarishb.pabal.tenant.api.dev.http.request.CreateTenantDevRequest;
import com.polarishb.pabal.tenant.api.dev.http.response.CreateTenantDevResponse;
import com.polarishb.pabal.tenant.api.dev.mapper.DevTenantCommandMapper;
import com.polarishb.pabal.tenant.application.command.handler.CreateTenantCommandHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dev")
@RequiredArgsConstructor
@Profile({"local", "test"})
public class DevTenantCommandController {

    private final DevTenantCommandMapper devTenantCommandMapper;
    private final CreateTenantCommandHandler createTenantCommandHandler;

    @PostMapping("/tenants")
    public CreateTenantDevResponse createTenant(@Valid @RequestBody CreateTenantDevRequest request) {
        return devTenantCommandMapper.toCreateTenantDevResponse(
                createTenantCommandHandler.handle(
                        devTenantCommandMapper.toCreateTenantCommand(request)
                )
        );
    }
}
