package com.polarishb.pabal.tenant.api.dev.http;

import com.polarishb.pabal.tenant.api.command.http.response.VerifyTenantDomainResponse;
import com.polarishb.pabal.tenant.api.command.mapper.TenantRegistrationCommandMapper;
import com.polarishb.pabal.tenant.application.command.handler.VerifyTenantDomainCommandHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Profile({"local", "test"})
@RestController
@RequestMapping("/dev")
@RequiredArgsConstructor
public class DevTenantRegistrationCommandController {

    private final TenantRegistrationCommandMapper tenantRegistrationCommandMapper;
    private final VerifyTenantDomainCommandHandler verifyTenantDomainCommandHandler;

    @PostMapping("/tenant-registrations/{registrationId}/domain-verification")
    public VerifyTenantDomainResponse verifyTenantDomain(@PathVariable UUID registrationId) {
        return tenantRegistrationCommandMapper.toVerifyTenantDomainResponse(
                verifyTenantDomainCommandHandler.handle(
                        tenantRegistrationCommandMapper.toVerifyTenantDomainCommand(registrationId)
                )
        );
    }
}
