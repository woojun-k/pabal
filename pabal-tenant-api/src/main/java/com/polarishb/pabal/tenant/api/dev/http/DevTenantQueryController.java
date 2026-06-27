package com.polarishb.pabal.tenant.api.dev.http;

import com.polarishb.pabal.tenant.api.dev.http.response.TenantDevResponse;
import com.polarishb.pabal.tenant.api.dev.mapper.DevTenantQueryMapper;
import com.polarishb.pabal.tenant.application.query.handler.GetTenantQueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/dev")
@RequiredArgsConstructor
@Profile({"local", "test"})
public class DevTenantQueryController {

    private final DevTenantQueryMapper devTenantQueryMapper;
    private final GetTenantQueryHandler getTenantQueryHandler;

    @GetMapping("/tenants/{tenantId}")
    public TenantDevResponse getTenant(@PathVariable UUID tenantId) {
        return devTenantQueryMapper.toTenantDevResponse(
                getTenantQueryHandler.handle(
                        devTenantQueryMapper.toGetTenantQuery(tenantId)
                )
        );
    }
}
