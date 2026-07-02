package com.polarishb.pabal.tenant.api.command.mapper;

import com.polarishb.pabal.tenant.api.command.http.request.RequestTenantRegistrationRequest;
import com.polarishb.pabal.tenant.api.command.http.response.ActivateTenantRegistrationResponse;
import com.polarishb.pabal.tenant.api.command.http.response.ReverifyTenantRegistrationResponse;
import com.polarishb.pabal.tenant.api.command.http.response.TenantRegistrationResponse;
import com.polarishb.pabal.tenant.api.command.http.response.VerifyTenantDomainResponse;
import com.polarishb.pabal.tenant.application.command.input.ActivateTenantRegistrationCommand;
import com.polarishb.pabal.tenant.application.command.input.RenewTenantRegistrationTokenCommand;
import com.polarishb.pabal.tenant.application.command.input.RequestTenantRegistrationCommand;
import com.polarishb.pabal.tenant.application.command.input.ReverifyTenantRegistrationCommand;
import com.polarishb.pabal.tenant.application.command.input.VerifyTenantDomainCommand;
import com.polarishb.pabal.tenant.application.command.output.ActivateTenantRegistrationResult;
import com.polarishb.pabal.tenant.application.command.output.ReverifyTenantRegistrationResult;
import com.polarishb.pabal.tenant.application.command.output.TenantRegistrationResult;
import com.polarishb.pabal.tenant.application.command.output.VerifyTenantDomainResult;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TenantRegistrationCommandMapper {

    public RequestTenantRegistrationCommand toRequestTenantRegistrationCommand(RequestTenantRegistrationRequest request) {
        return new RequestTenantRegistrationCommand(request.tenantName(), request.domainName());
    }

    public VerifyTenantDomainCommand toVerifyTenantDomainCommand(UUID registrationId) {
        return new VerifyTenantDomainCommand(registrationId);
    }

    public RenewTenantRegistrationTokenCommand toRenewTenantRegistrationTokenCommand(UUID registrationId) {
        return new RenewTenantRegistrationTokenCommand(registrationId);
    }

    public ActivateTenantRegistrationCommand toActivateTenantRegistrationCommand(UUID registrationId) {
        return new ActivateTenantRegistrationCommand(registrationId);
    }

    public ReverifyTenantRegistrationCommand toReverifyTenantRegistrationCommand(UUID registrationId) {
        return new ReverifyTenantRegistrationCommand(registrationId);
    }

    public TenantRegistrationResponse toTenantRegistrationResponse(TenantRegistrationResult result) {
        return new TenantRegistrationResponse(
                result.registrationId(),
                result.tenantName(),
                result.domainName(),
                result.status(),
                result.verificationDnsName(),
                result.verificationTxtValue(),
                result.verificationExpiresAt(),
                result.activationExpiresAt(),
                result.createdAt()
        );
    }

    public VerifyTenantDomainResponse toVerifyTenantDomainResponse(VerifyTenantDomainResult result) {
        return new VerifyTenantDomainResponse(
                result.registrationId(),
                result.tenantName(),
                result.domainName(),
                result.status(),
                result.verifiedAt(),
                result.activationExpiresAt()
        );
    }

    public ActivateTenantRegistrationResponse toActivateTenantRegistrationResponse(ActivateTenantRegistrationResult result) {
        return new ActivateTenantRegistrationResponse(
                result.registrationId(),
                result.tenantId(),
                result.tenantName(),
                result.domainName(),
                result.status(),
                result.verifiedAt(),
                result.activatedAt()
        );
    }

    public ReverifyTenantRegistrationResponse toReverifyTenantRegistrationResponse(ReverifyTenantRegistrationResult result) {
        return new ReverifyTenantRegistrationResponse(
                result.registrationId(),
                result.tenantName(),
                result.domainName(),
                result.status(),
                result.verifiedAt(),
                result.activationExpiresAt()
        );
    }
}
