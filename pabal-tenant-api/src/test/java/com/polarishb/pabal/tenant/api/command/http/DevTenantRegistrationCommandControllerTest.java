package com.polarishb.pabal.tenant.api.command.http;

import com.polarishb.pabal.tenant.api.command.mapper.TenantRegistrationCommandMapper;
import com.polarishb.pabal.tenant.api.dev.http.DevTenantRegistrationCommandController;
import com.polarishb.pabal.tenant.application.command.handler.VerifyTenantDomainCommandHandler;
import com.polarishb.pabal.tenant.application.command.input.VerifyTenantDomainCommand;
import com.polarishb.pabal.tenant.application.command.output.VerifyTenantDomainResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DevTenantRegistrationCommandControllerTest {

    private final VerifyTenantDomainCommandHandler verifyTenantDomainCommandHandler =
            mock(VerifyTenantDomainCommandHandler.class);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        DevTenantRegistrationCommandController controller = new DevTenantRegistrationCommandController(
                new TenantRegistrationCommandMapper(),
                verifyTenantDomainCommandHandler
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    /**
     * Contract under test (verify-only split): the dev domain-verification endpoint
     * surfaces the verify-only result - status = "DOMAIN_VERIFIED", no
     * tenantId/activatedAt fields, and activationExpiresAt instead.
     */
    @Test
    void verifyTenantDomain_maps_path_registration_id_to_command_and_returns_domain_verified_response() throws Exception {
        UUID registrationId = UUID.randomUUID();
        Instant verifiedAt = Instant.parse("2026-06-19T00:05:00Z");
        Instant activationExpiresAt = verifiedAt.plusSeconds(604_800);
        when(verifyTenantDomainCommandHandler.handle(any(VerifyTenantDomainCommand.class)))
                .thenReturn(new VerifyTenantDomainResult(
                        registrationId,
                        "Acme",
                        "example.com",
                        "DOMAIN_VERIFIED",
                        verifiedAt,
                        activationExpiresAt
                ));

        mockMvc.perform(post("/dev/tenant-registrations/{registrationId}/domain-verification", registrationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationId").value(registrationId.toString()))
                .andExpect(jsonPath("$.status").value("DOMAIN_VERIFIED"))
                .andExpect(jsonPath("$.verifiedAt").value(verifiedAt.toString()))
                .andExpect(jsonPath("$.activationExpiresAt").value(activationExpiresAt.toString()))
                .andExpect(jsonPath("$.tenantId").doesNotExist())
                .andExpect(jsonPath("$.activatedAt").doesNotExist());

        ArgumentCaptor<VerifyTenantDomainCommand> commandCaptor =
                ArgumentCaptor.forClass(VerifyTenantDomainCommand.class);
        verify(verifyTenantDomainCommandHandler).handle(commandCaptor.capture());
        assertThat(commandCaptor.getValue().registrationId()).isEqualTo(registrationId);
    }
}
