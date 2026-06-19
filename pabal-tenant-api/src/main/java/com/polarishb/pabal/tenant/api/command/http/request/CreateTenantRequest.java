package com.polarishb.pabal.tenant.api.command.http.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTenantRequest(
        @NotBlank @Size(max = 100) String name
) {
}
