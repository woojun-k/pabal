package com.polarishb.pabal.tenant.api.dev.http.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTenantDevRequest(
        @NotBlank @Size(max = 100) String name
) {
}
