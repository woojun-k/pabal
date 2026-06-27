package com.polarishb.pabal.tenant.api.command.http;

import com.polarishb.pabal.tenant.api.command.http.request.RequestTenantRegistrationRequest;
import com.polarishb.pabal.tenant.api.command.http.response.TenantRegistrationResponse;
import com.polarishb.pabal.tenant.api.command.mapper.TenantRegistrationCommandMapper;
import com.polarishb.pabal.tenant.application.command.handler.RenewTenantRegistrationTokenCommandHandler;
import com.polarishb.pabal.tenant.application.command.handler.RequestTenantRegistrationCommandHandler;
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
}
