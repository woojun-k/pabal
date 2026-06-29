package com.polarishb.pabal.tenant.api.query.mapper;

import com.polarishb.pabal.tenant.api.query.http.response.TenantRegistrationDetailResponse;
import com.polarishb.pabal.tenant.application.query.input.GetTenantRegistrationQuery;
import com.polarishb.pabal.tenant.application.query.output.TenantRegistrationDto;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TenantRegistrationQueryMapper {

    public GetTenantRegistrationQuery toGetTenantRegistrationQuery(UUID registrationId) {
        return new GetTenantRegistrationQuery(registrationId);
    }

    public TenantRegistrationDetailResponse toTenantRegistrationDetailResponse(TenantRegistrationDto dto) {
        return new TenantRegistrationDetailResponse(
                dto.registrationId(),
                dto.tenantId(),
                dto.tenantName(),
                dto.domainName(),
                dto.status(),
                dto.verificationDnsName(),
                dto.verificationTxtValue(),
                dto.expiresAt(),
                dto.verifiedAt(),
                dto.activatedAt(),
                dto.createdAt(),
                dto.updatedAt()
        );
    }
}
