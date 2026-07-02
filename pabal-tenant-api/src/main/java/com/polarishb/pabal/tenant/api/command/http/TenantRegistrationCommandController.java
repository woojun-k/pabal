package com.polarishb.pabal.tenant.api.command.http;

import com.polarishb.pabal.tenant.api.command.http.request.RequestTenantRegistrationRequest;
import com.polarishb.pabal.tenant.api.command.http.response.ActivateTenantRegistrationResponse;
import com.polarishb.pabal.tenant.api.command.http.response.ReverifyTenantRegistrationResponse;
import com.polarishb.pabal.tenant.api.command.http.response.TenantRegistrationResponse;
import com.polarishb.pabal.tenant.api.command.mapper.TenantRegistrationCommandMapper;
import com.polarishb.pabal.tenant.application.command.handler.ActivateTenantRegistrationCommandHandler;
import com.polarishb.pabal.tenant.application.command.handler.RenewTenantRegistrationTokenCommandHandler;
import com.polarishb.pabal.tenant.application.command.handler.RequestTenantRegistrationCommandHandler;
import com.polarishb.pabal.tenant.application.command.handler.ReverifyTenantRegistrationCommandHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TenantRegistrationCommandController {

    private final TenantRegistrationCommandMapper tenantRegistrationCommandMapper;
    private final RequestTenantRegistrationCommandHandler requestTenantRegistrationCommandHandler;
    private final RenewTenantRegistrationTokenCommandHandler renewTenantRegistrationTokenCommandHandler;
    private final ActivateTenantRegistrationCommandHandler activateTenantRegistrationCommandHandler;
    private final ReverifyTenantRegistrationCommandHandler reverifyTenantRegistrationCommandHandler;

    @PostMapping("/tenant-registrations")
    @ResponseStatus(HttpStatus.CREATED)
    public TenantRegistrationResponse requestTenantRegistration(
            @Valid @RequestBody RequestTenantRegistrationRequest request
    ) {
        return tenantRegistrationCommandMapper.toTenantRegistrationResponse(
                requestTenantRegistrationCommandHandler.handle(
                        tenantRegistrationCommandMapper.toRequestTenantRegistrationCommand(request)
                )
        );
    }

    @PostMapping("/tenant-registrations/{registrationId}/verification-token")
    public TenantRegistrationResponse renewVerificationToken(@PathVariable UUID registrationId) {
        return tenantRegistrationCommandMapper.toTenantRegistrationResponse(
                renewTenantRegistrationTokenCommandHandler.handle(
                        tenantRegistrationCommandMapper.toRenewTenantRegistrationTokenCommand(registrationId)
                )
        );
    }

    @PostMapping("/tenant-registrations/{registrationId}/activation")
    public ActivateTenantRegistrationResponse activateTenantRegistration(@PathVariable UUID registrationId) {
        return tenantRegistrationCommandMapper.toActivateTenantRegistrationResponse(
                activateTenantRegistrationCommandHandler.handle(
                        tenantRegistrationCommandMapper.toActivateTenantRegistrationCommand(registrationId)
                )
        );
    }

    @PostMapping("/tenant-registrations/{registrationId}/reverification")
    public ReverifyTenantRegistrationResponse reverifyTenantRegistration(@PathVariable UUID registrationId) {
        return tenantRegistrationCommandMapper.toReverifyTenantRegistrationResponse(
                reverifyTenantRegistrationCommandHandler.handle(
                        tenantRegistrationCommandMapper.toReverifyTenantRegistrationCommand(registrationId)
                )
        );
    }
}
