package com.polarishb.pabal.tenant.api.command.http.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RequestTenantRegistrationRequest(
        @NotBlank @Size(max = 100) String tenantName,
        @NotBlank @Size(max = 253) String domainName
) {
}
