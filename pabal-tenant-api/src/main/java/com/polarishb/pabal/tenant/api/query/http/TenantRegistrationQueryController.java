package com.polarishb.pabal.tenant.api.query.http;

import com.polarishb.pabal.tenant.api.query.http.response.TenantRegistrationDetailResponse;
import com.polarishb.pabal.tenant.api.query.mapper.TenantRegistrationQueryMapper;
import com.polarishb.pabal.tenant.application.query.handler.GetTenantRegistrationQueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TenantRegistrationQueryController {

    private final TenantRegistrationQueryMapper tenantRegistrationQueryMapper;
    private final GetTenantRegistrationQueryHandler getTenantRegistrationQueryHandler;

    @GetMapping("/tenant-registrations/{registrationId}")
    public TenantRegistrationDetailResponse getTenantRegistration(@PathVariable UUID registrationId) {
        return tenantRegistrationQueryMapper.toTenantRegistrationDetailResponse(
                getTenantRegistrationQueryHandler.handle(
                        tenantRegistrationQueryMapper.toGetTenantRegistrationQuery(registrationId)
                )
        );
    }
}
