package com.polarishb.pabal.tenant.api.query.http;

import com.polarishb.pabal.tenant.api.query.http.response.TenantResponse;
import com.polarishb.pabal.tenant.api.query.mapper.TenantQueryMapper;
import com.polarishb.pabal.tenant.application.query.handler.GetTenantQueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TenantQueryController {

    private final TenantQueryMapper tenantQueryMapper;
    private final GetTenantQueryHandler getTenantQueryHandler;

    @GetMapping("/tenants/{tenantId}")
    public TenantResponse getTenant(@PathVariable UUID tenantId) {
        return tenantQueryMapper.toTenantResponse(
                getTenantQueryHandler.handle(
                        tenantQueryMapper.toGetTenantQuery(tenantId)
                )
        );
    }
}
