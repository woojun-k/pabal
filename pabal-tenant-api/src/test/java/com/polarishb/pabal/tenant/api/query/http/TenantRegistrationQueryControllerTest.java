package com.polarishb.pabal.tenant.api.query.http;

import com.polarishb.pabal.tenant.api.query.mapper.TenantRegistrationQueryMapper;
import com.polarishb.pabal.tenant.application.query.handler.GetTenantRegistrationQueryHandler;
import com.polarishb.pabal.tenant.application.query.input.GetTenantRegistrationQuery;
import com.polarishb.pabal.tenant.application.query.output.TenantRegistrationDto;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TenantRegistrationQueryControllerTest {

    private final GetTenantRegistrationQueryHandler getTenantRegistrationQueryHandler =
            mock(GetTenantRegistrationQueryHandler.class);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        TenantRegistrationQueryController controller = new TenantRegistrationQueryController(
                new TenantRegistrationQueryMapper(),
                getTenantRegistrationQueryHandler
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getTenantRegistration_maps_path_registration_id_to_query_and_response() throws Exception {
        UUID registrationId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-06-19T00:00:00Z");
        Instant updatedAt = Instant.parse("2026-06-19T00:05:00Z");
        Instant verificationExpiresAt = Instant.parse("2026-06-26T00:00:00Z");
        Instant activationExpiresAt = Instant.parse("2026-07-03T00:00:00Z");
        when(getTenantRegistrationQueryHandler.handle(any(GetTenantRegistrationQuery.class)))
                .thenReturn(new TenantRegistrationDto(
                        registrationId,
                        tenantId,
                        "Acme",
                        "example.com",
                        "ACTIVATED",
                        "_pabal-verification.example.com",
                        "pabal-verification=abcdefghijklmnopqrstuvwxyzABCDEF",
                        verificationExpiresAt,
                        activationExpiresAt,
                        updatedAt,
                        updatedAt,
                        createdAt,
                        updatedAt
                ));

        mockMvc.perform(get("/api/v1/tenant-registrations/{registrationId}", registrationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationId").value(registrationId.toString()))
                .andExpect(jsonPath("$.tenantId").value(tenantId.toString()))
                .andExpect(jsonPath("$.tenantName").value("Acme"))
                .andExpect(jsonPath("$.domainName").value("example.com"))
                .andExpect(jsonPath("$.status").value("ACTIVATED"))
                .andExpect(jsonPath("$.verificationExpiresAt").value(verificationExpiresAt.toString()))
                .andExpect(jsonPath("$.activationExpiresAt").value(activationExpiresAt.toString()))
                .andExpect(jsonPath("$.expiresAt").doesNotExist())
                .andExpect(jsonPath("$.createdAt").value(createdAt.toString()))
                .andExpect(jsonPath("$.updatedAt").value(updatedAt.toString()));

        ArgumentCaptor<GetTenantRegistrationQuery> queryCaptor =
                ArgumentCaptor.forClass(GetTenantRegistrationQuery.class);
        verify(getTenantRegistrationQueryHandler).handle(queryCaptor.capture());
        assertThat(queryCaptor.getValue().registrationId()).isEqualTo(registrationId);
    }

    /**
     * Contract: GET .../{id} passes all five status strings through verbatim, including
     * DOMAIN_VERIFIED and REVERIFICATION_REQUIRED (previously-unreachable statuses at
     * this endpoint boundary before the two-phase split).
     */
    @Test
    void getTenantRegistration_passes_domain_verified_and_reverification_required_statuses_verbatim() throws Exception {
        UUID registrationId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-06-19T00:00:00Z");
        Instant updatedAt = Instant.parse("2026-06-19T00:05:00Z");
        Instant verificationExpiresAt = Instant.parse("2026-06-26T00:00:00Z");
        Instant activationExpiresAt = Instant.parse("2026-07-03T00:00:00Z");

        when(getTenantRegistrationQueryHandler.handle(any(GetTenantRegistrationQuery.class)))
                .thenReturn(new TenantRegistrationDto(
                        registrationId,
                        null,
                        "Acme",
                        "example.com",
                        "DOMAIN_VERIFIED",
                        "_pabal-verification.example.com",
                        "pabal-verification=abcdefghijklmnopqrstuvwxyzABCDEF",
                        verificationExpiresAt,
                        activationExpiresAt,
                        updatedAt,
                        null,
                        createdAt,
                        updatedAt
                ));

        mockMvc.perform(get("/api/v1/tenant-registrations/{registrationId}", registrationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DOMAIN_VERIFIED"))
                .andExpect(jsonPath("$.activationExpiresAt").value(activationExpiresAt.toString()));

        when(getTenantRegistrationQueryHandler.handle(any(GetTenantRegistrationQuery.class)))
                .thenReturn(new TenantRegistrationDto(
                        registrationId,
                        null,
                        "Acme",
                        "example.com",
                        "REVERIFICATION_REQUIRED",
                        "_pabal-verification.example.com",
                        "pabal-verification=abcdefghijklmnopqrstuvwxyzABCDEF",
                        verificationExpiresAt,
                        activationExpiresAt,
                        updatedAt,
                        null,
                        createdAt,
                        activationExpiresAt
                ));

        mockMvc.perform(get("/api/v1/tenant-registrations/{registrationId}", registrationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVERIFICATION_REQUIRED"));
    }
}
