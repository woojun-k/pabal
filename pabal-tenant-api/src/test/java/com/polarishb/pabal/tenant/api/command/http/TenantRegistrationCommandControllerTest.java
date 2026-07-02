package com.polarishb.pabal.tenant.api.command.http;

import com.polarishb.pabal.tenant.api.command.mapper.TenantRegistrationCommandMapper;
import com.polarishb.pabal.tenant.application.command.handler.ActivateTenantRegistrationCommandHandler;
import com.polarishb.pabal.tenant.application.command.handler.RenewTenantRegistrationTokenCommandHandler;
import com.polarishb.pabal.tenant.application.command.handler.RequestTenantRegistrationCommandHandler;
import com.polarishb.pabal.tenant.application.command.handler.ReverifyTenantRegistrationCommandHandler;
import com.polarishb.pabal.tenant.application.command.input.ActivateTenantRegistrationCommand;
import com.polarishb.pabal.tenant.application.command.input.RenewTenantRegistrationTokenCommand;
import com.polarishb.pabal.tenant.application.command.input.RequestTenantRegistrationCommand;
import com.polarishb.pabal.tenant.application.command.input.ReverifyTenantRegistrationCommand;
import com.polarishb.pabal.tenant.application.command.output.ActivateTenantRegistrationResult;
import com.polarishb.pabal.tenant.application.command.output.ReverifyTenantRegistrationResult;
import com.polarishb.pabal.tenant.application.command.output.TenantRegistrationResult;
import com.polarishb.pabal.tenant.domain.exception.TenantDomainVerificationFailedException;
import com.polarishb.pabal.tenant.domain.exception.TenantRegistrationExpiredException;
import com.polarishb.pabal.tenant.domain.exception.TenantRegistrationNotFoundException;
import com.polarishb.pabal.tenant.domain.exception.TenantRegistrationNotPendingException;
import com.polarishb.pabal.tenant.domain.exception.code.TenantErrorCode;
import com.polarishb.pabal.tenant.domain.model.type.TenantRegistrationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TenantRegistrationCommandControllerTest {

    private static final Instant CREATED_AT = Instant.parse("2026-06-19T00:00:00Z");
    private static final Instant VERIFICATION_EXPIRES_AT = Instant.parse("2026-06-26T00:00:00Z");
    private static final Instant VERIFIED_AT = Instant.parse("2026-06-20T00:00:00Z");
    private static final Instant ACTIVATION_EXPIRES_AT = Instant.parse("2026-06-27T00:00:00Z");
    private static final Instant ACTIVATED_AT = Instant.parse("2026-06-20T00:05:00Z");

    private final RequestTenantRegistrationCommandHandler requestTenantRegistrationCommandHandler =
            mock(RequestTenantRegistrationCommandHandler.class);
    private final RenewTenantRegistrationTokenCommandHandler renewTenantRegistrationTokenCommandHandler =
            mock(RenewTenantRegistrationTokenCommandHandler.class);
    private final ActivateTenantRegistrationCommandHandler activateTenantRegistrationCommandHandler =
            mock(ActivateTenantRegistrationCommandHandler.class);
    private final ReverifyTenantRegistrationCommandHandler reverifyTenantRegistrationCommandHandler =
            mock(ReverifyTenantRegistrationCommandHandler.class);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        TenantRegistrationCommandController controller = new TenantRegistrationCommandController(
                new TenantRegistrationCommandMapper(),
                requestTenantRegistrationCommandHandler,
                renewTenantRegistrationTokenCommandHandler,
                activateTenantRegistrationCommandHandler,
                reverifyTenantRegistrationCommandHandler
        );

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setValidator(validator)
                .build();
    }

    @Test
    void requestTenantRegistration_maps_request_body_to_command_and_returns_created_response() throws Exception {
        UUID registrationId = UUID.randomUUID();
        when(requestTenantRegistrationCommandHandler.handle(any(RequestTenantRegistrationCommand.class)))
                .thenReturn(new TenantRegistrationResult(
                        registrationId,
                        "Acme",
                        "example.com",
                        "PENDING_VERIFICATION",
                        "_pabal-verification.example.com",
                        "pabal-verification=abcdefghijklmnopqrstuvwxyzABCDEF",
                        VERIFICATION_EXPIRES_AT,
                        null,
                        CREATED_AT
                ));

        mockMvc.perform(
                        post("/api/v1/tenant-registrations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "tenantName": "Acme",
                                          "domainName": "Example.COM"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.registrationId").value(registrationId.toString()))
                .andExpect(jsonPath("$.tenantName").value("Acme"))
                .andExpect(jsonPath("$.domainName").value("example.com"))
                .andExpect(jsonPath("$.status").value("PENDING_VERIFICATION"))
                .andExpect(jsonPath("$.verificationDnsName").value("_pabal-verification.example.com"))
                .andExpect(jsonPath("$.verificationTxtValue").value("pabal-verification=abcdefghijklmnopqrstuvwxyzABCDEF"))
                .andExpect(jsonPath("$.verificationExpiresAt").value(VERIFICATION_EXPIRES_AT.toString()))
                .andExpect(jsonPath("$.activationExpiresAt").doesNotExist())
                .andExpect(jsonPath("$.expiresAt").doesNotExist())
                .andExpect(jsonPath("$.createdAt").value(CREATED_AT.toString()));

        ArgumentCaptor<RequestTenantRegistrationCommand> commandCaptor =
                ArgumentCaptor.forClass(RequestTenantRegistrationCommand.class);
        verify(requestTenantRegistrationCommandHandler).handle(commandCaptor.capture());
        assertThat(commandCaptor.getValue().tenantName()).isEqualTo("Acme");
        assertThat(commandCaptor.getValue().domainName()).isEqualTo("Example.COM");
    }

    @Test
    void renewVerificationToken_maps_path_registration_id_to_command() throws Exception {
        UUID registrationId = UUID.randomUUID();
        when(renewTenantRegistrationTokenCommandHandler.handle(any(RenewTenantRegistrationTokenCommand.class)))
                .thenReturn(new TenantRegistrationResult(
                        registrationId,
                        "Acme",
                        "example.com",
                        "PENDING_VERIFICATION",
                        "_pabal-verification.example.com",
                        "pabal-verification=ABCDEFabcdefghijklmnopqrstuvwxyz",
                        VERIFICATION_EXPIRES_AT,
                        null,
                        CREATED_AT
                ));

        mockMvc.perform(post("/api/v1/tenant-registrations/{registrationId}/verification-token", registrationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationId").value(registrationId.toString()))
                .andExpect(jsonPath("$.verificationTxtValue").value("pabal-verification=ABCDEFabcdefghijklmnopqrstuvwxyz"))
                .andExpect(jsonPath("$.verificationExpiresAt").value(VERIFICATION_EXPIRES_AT.toString()))
                .andExpect(jsonPath("$.expiresAt").doesNotExist());

        ArgumentCaptor<RenewTenantRegistrationTokenCommand> commandCaptor =
                ArgumentCaptor.forClass(RenewTenantRegistrationTokenCommand.class);
        verify(renewTenantRegistrationTokenCommandHandler).handle(commandCaptor.capture());
        assertThat(commandCaptor.getValue().registrationId()).isEqualTo(registrationId);
    }

    @Test
    void requestTenantRegistration_rejects_blank_domain_before_handler_call() throws Exception {
        mockMvc.perform(
                        post("/api/v1/tenant-registrations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "tenantName": "Acme",
                                          "domainName": " "
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(
                requestTenantRegistrationCommandHandler,
                renewTenantRegistrationTokenCommandHandler,
                activateTenantRegistrationCommandHandler,
                reverifyTenantRegistrationCommandHandler
        );
    }

    /**
     * MockMvc (standalone, no controller advice) may either rethrow the handler's
     * exception directly or wrap it (servlet-container-dependent across Spring MVC
     * versions). This helper accepts either shape so the assertion is robust to that
     * detail while still failing if the expected exception type never appears anywhere
     * in the thrown/caused chain.
     */
    private void assertPropagates(Throwable thrown, Class<? extends Exception> expectedType) {
        Throwable current = thrown;
        while (current != null) {
            if (expectedType.isInstance(current)) {
                return;
            }
            current = current.getCause();
        }
        throw new AssertionError(
                "Expected " + expectedType.getName() + " somewhere in the exception chain of: " + thrown,
                thrown
        );
    }

    /**
     * Contract: POST .../activation on an open-window DOMAIN_VERIFIED registration
     * returns 200 with status = "ACTIVATED" and a non-null tenantId.
     */
    @Test
    void activateTenantRegistration_returns_200_with_activated_status_and_tenant_id() throws Exception {
        UUID registrationId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        when(activateTenantRegistrationCommandHandler.handle(any(ActivateTenantRegistrationCommand.class)))
                .thenReturn(new ActivateTenantRegistrationResult(
                        registrationId,
                        tenantId,
                        "Acme",
                        "example.com",
                        "ACTIVATED",
                        VERIFIED_AT,
                        ACTIVATED_AT
                ));

        mockMvc.perform(post("/api/v1/tenant-registrations/{registrationId}/activation", registrationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationId").value(registrationId.toString()))
                .andExpect(jsonPath("$.tenantId").value(tenantId.toString()))
                .andExpect(jsonPath("$.tenantName").value("Acme"))
                .andExpect(jsonPath("$.domainName").value("example.com"))
                .andExpect(jsonPath("$.status").value("ACTIVATED"))
                .andExpect(jsonPath("$.verifiedAt").value(VERIFIED_AT.toString()))
                .andExpect(jsonPath("$.activatedAt").value(ACTIVATED_AT.toString()));

        ArgumentCaptor<ActivateTenantRegistrationCommand> commandCaptor =
                ArgumentCaptor.forClass(ActivateTenantRegistrationCommand.class);
        verify(activateTenantRegistrationCommandHandler).handle(commandCaptor.capture());
        assertThat(commandCaptor.getValue().registrationId()).isEqualTo(registrationId);
    }

    /**
     * Contract: activation failure mapping - unknown id -> TenantRegistrationNotFoundException
     * (TenantErrorCode.TENANT_REGISTRATION_NOT_FOUND: code=TNT404002, httpStatus=404).
     *
     * <p>This module (pabal-tenant-api) does not depend on pabal-web, so
     * GlobalExceptionHandler is not wired here; the generic GlobalException ->
     * ApiError/HTTP-status mapping is covered by pabal-web's own
     * GlobalExceptionHandlerTest. This test verifies the controller lets the
     * domain/application exception propagate unmodified (not swallowed, not translated
     * to a different exception type) and that TenantErrorCode carries the contract's
     * code/status for that exception.
     */
    @Test
    void activateTenantRegistration_propagates_not_found_exception_with_TNT404002_and_404() throws Exception {
        UUID registrationId = UUID.randomUUID();
        when(activateTenantRegistrationCommandHandler.handle(any(ActivateTenantRegistrationCommand.class)))
                .thenThrow(new TenantRegistrationNotFoundException(registrationId));

        try {
            mockMvc.perform(post("/api/v1/tenant-registrations/{registrationId}/activation", registrationId));
            throw new AssertionError("Expected TenantRegistrationNotFoundException to propagate");
        } catch (Exception thrown) {
            assertPropagates(thrown, TenantRegistrationNotFoundException.class);
        }

        assertThat(TenantErrorCode.TENANT_REGISTRATION_NOT_FOUND.getCode()).isEqualTo("TNT404002");
        assertThat(TenantErrorCode.TENANT_REGISTRATION_NOT_FOUND.getHttpStatus()).isEqualTo(404);
    }

    /**
     * Contract: activation failure mapping - status != DOMAIN_VERIFIED -> 409 TNT409003.
     * See the not-found test above for why propagation (not GlobalExceptionHandler
     * response assertions) is this module's test boundary.
     */
    @Test
    void activateTenantRegistration_propagates_not_pending_exception_with_TNT409003_and_409() throws Exception {
        UUID registrationId = UUID.randomUUID();
        when(activateTenantRegistrationCommandHandler.handle(any(ActivateTenantRegistrationCommand.class)))
                .thenThrow(new TenantRegistrationNotPendingException(registrationId, TenantRegistrationStatus.REVERIFICATION_REQUIRED));

        try {
            mockMvc.perform(post("/api/v1/tenant-registrations/{registrationId}/activation", registrationId));
            throw new AssertionError("Expected TenantRegistrationNotPendingException to propagate");
        } catch (Exception thrown) {
            assertPropagates(thrown, TenantRegistrationNotPendingException.class);
        }

        assertThat(TenantErrorCode.TENANT_REGISTRATION_NOT_PENDING.getCode()).isEqualTo("TNT409003");
        assertThat(TenantErrorCode.TENANT_REGISTRATION_NOT_PENDING.getHttpStatus()).isEqualTo(409);
    }

    /**
     * Contract: activation failure mapping - status = DOMAIN_VERIFIED but window lapsed
     * -> 410 TNT410001.
     */
    @Test
    void activateTenantRegistration_propagates_expired_exception_with_TNT410001_and_410() throws Exception {
        UUID registrationId = UUID.randomUUID();
        when(activateTenantRegistrationCommandHandler.handle(any(ActivateTenantRegistrationCommand.class)))
                .thenThrow(new TenantRegistrationExpiredException(registrationId, ACTIVATION_EXPIRES_AT));

        try {
            mockMvc.perform(post("/api/v1/tenant-registrations/{registrationId}/activation", registrationId));
            throw new AssertionError("Expected TenantRegistrationExpiredException to propagate");
        } catch (Exception thrown) {
            assertPropagates(thrown, TenantRegistrationExpiredException.class);
        }

        assertThat(TenantErrorCode.TENANT_REGISTRATION_EXPIRED.getCode()).isEqualTo("TNT410001");
        assertThat(TenantErrorCode.TENANT_REGISTRATION_EXPIRED.getHttpStatus()).isEqualTo(410);
    }

    /**
     * Contract: POST .../reverification on REVERIFICATION_REQUIRED with matching DNS
     * TXT returns 200 with status = "DOMAIN_VERIFIED" and a new activationExpiresAt.
     */
    @Test
    void reverifyTenantRegistration_returns_200_with_domain_verified_status_and_new_activation_window() throws Exception {
        UUID registrationId = UUID.randomUUID();
        when(reverifyTenantRegistrationCommandHandler.handle(any(ReverifyTenantRegistrationCommand.class)))
                .thenReturn(new ReverifyTenantRegistrationResult(
                        registrationId,
                        "Acme",
                        "example.com",
                        "DOMAIN_VERIFIED",
                        VERIFIED_AT,
                        ACTIVATION_EXPIRES_AT
                ));

        mockMvc.perform(post("/api/v1/tenant-registrations/{registrationId}/reverification", registrationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationId").value(registrationId.toString()))
                .andExpect(jsonPath("$.tenantName").value("Acme"))
                .andExpect(jsonPath("$.domainName").value("example.com"))
                .andExpect(jsonPath("$.status").value("DOMAIN_VERIFIED"))
                .andExpect(jsonPath("$.verifiedAt").value(VERIFIED_AT.toString()))
                .andExpect(jsonPath("$.activationExpiresAt").value(ACTIVATION_EXPIRES_AT.toString()));

        ArgumentCaptor<ReverifyTenantRegistrationCommand> commandCaptor =
                ArgumentCaptor.forClass(ReverifyTenantRegistrationCommand.class);
        verify(reverifyTenantRegistrationCommandHandler).handle(commandCaptor.capture());
        assertThat(commandCaptor.getValue().registrationId()).isEqualTo(registrationId);
    }

    /**
     * Contract: reverification failure mapping - unknown id -> 404 TNT404002.
     */
    @Test
    void reverifyTenantRegistration_propagates_not_found_exception_with_TNT404002_and_404() throws Exception {
        UUID registrationId = UUID.randomUUID();
        when(reverifyTenantRegistrationCommandHandler.handle(any(ReverifyTenantRegistrationCommand.class)))
                .thenThrow(new TenantRegistrationNotFoundException(registrationId));

        try {
            mockMvc.perform(post("/api/v1/tenant-registrations/{registrationId}/reverification", registrationId));
            throw new AssertionError("Expected TenantRegistrationNotFoundException to propagate");
        } catch (Exception thrown) {
            assertPropagates(thrown, TenantRegistrationNotFoundException.class);
        }

        assertThat(TenantErrorCode.TENANT_REGISTRATION_NOT_FOUND.getCode()).isEqualTo("TNT404002");
        assertThat(TenantErrorCode.TENANT_REGISTRATION_NOT_FOUND.getHttpStatus()).isEqualTo(404);
    }

    /**
     * Contract: reverification failure mapping - status != REVERIFICATION_REQUIRED ->
     * 409 TNT409003.
     */
    @Test
    void reverifyTenantRegistration_propagates_not_pending_exception_with_TNT409003_and_409() throws Exception {
        UUID registrationId = UUID.randomUUID();
        when(reverifyTenantRegistrationCommandHandler.handle(any(ReverifyTenantRegistrationCommand.class)))
                .thenThrow(new TenantRegistrationNotPendingException(registrationId, TenantRegistrationStatus.DOMAIN_VERIFIED));

        try {
            mockMvc.perform(post("/api/v1/tenant-registrations/{registrationId}/reverification", registrationId));
            throw new AssertionError("Expected TenantRegistrationNotPendingException to propagate");
        } catch (Exception thrown) {
            assertPropagates(thrown, TenantRegistrationNotPendingException.class);
        }

        assertThat(TenantErrorCode.TENANT_REGISTRATION_NOT_PENDING.getCode()).isEqualTo("TNT409003");
        assertThat(TenantErrorCode.TENANT_REGISTRATION_NOT_PENDING.getHttpStatus()).isEqualTo(409);
    }

    /**
     * Contract: reverification DNS TXT mismatch/absence -> 409 TNT409002.
     */
    @Test
    void reverifyTenantRegistration_propagates_dns_verification_failed_exception_with_TNT409002_and_409() throws Exception {
        UUID registrationId = UUID.randomUUID();
        when(reverifyTenantRegistrationCommandHandler.handle(any(ReverifyTenantRegistrationCommand.class)))
                .thenThrow(new TenantDomainVerificationFailedException(
                        new com.polarishb.pabal.tenant.domain.model.vo.TenantDomainName("example.com"),
                        "_pabal-verification.example.com"
                ));

        try {
            mockMvc.perform(post("/api/v1/tenant-registrations/{registrationId}/reverification", registrationId));
            throw new AssertionError("Expected TenantDomainVerificationFailedException to propagate");
        } catch (Exception thrown) {
            assertPropagates(thrown, TenantDomainVerificationFailedException.class);
        }

        assertThat(TenantErrorCode.TENANT_DOMAIN_VERIFICATION_FAILED.getCode()).isEqualTo("TNT409002");
        assertThat(TenantErrorCode.TENANT_DOMAIN_VERIFICATION_FAILED.getHttpStatus()).isEqualTo(409);
    }
}
