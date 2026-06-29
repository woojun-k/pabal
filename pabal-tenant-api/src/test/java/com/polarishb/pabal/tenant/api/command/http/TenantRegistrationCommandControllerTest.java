package com.polarishb.pabal.tenant.api.command.http;

import com.polarishb.pabal.tenant.api.command.mapper.TenantRegistrationCommandMapper;
import com.polarishb.pabal.tenant.application.command.handler.RenewTenantRegistrationTokenCommandHandler;
import com.polarishb.pabal.tenant.application.command.handler.RequestTenantRegistrationCommandHandler;
import com.polarishb.pabal.tenant.application.command.input.RenewTenantRegistrationTokenCommand;
import com.polarishb.pabal.tenant.application.command.input.RequestTenantRegistrationCommand;
import com.polarishb.pabal.tenant.application.command.output.TenantRegistrationResult;
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
    private static final Instant EXPIRES_AT = Instant.parse("2026-06-26T00:00:00Z");

    private final RequestTenantRegistrationCommandHandler requestTenantRegistrationCommandHandler =
            mock(RequestTenantRegistrationCommandHandler.class);
    private final RenewTenantRegistrationTokenCommandHandler renewTenantRegistrationTokenCommandHandler =
            mock(RenewTenantRegistrationTokenCommandHandler.class);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        TenantRegistrationCommandController controller = new TenantRegistrationCommandController(
                new TenantRegistrationCommandMapper(),
                requestTenantRegistrationCommandHandler,
                renewTenantRegistrationTokenCommandHandler
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
                        EXPIRES_AT,
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
                .andExpect(jsonPath("$.expiresAt").value(EXPIRES_AT.toString()))
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
                        EXPIRES_AT,
                        CREATED_AT
                ));

        mockMvc.perform(post("/api/v1/tenant-registrations/{registrationId}/verification-token", registrationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationId").value(registrationId.toString()))
                .andExpect(jsonPath("$.verificationTxtValue").value("pabal-verification=ABCDEFabcdefghijklmnopqrstuvwxyz"));

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
                renewTenantRegistrationTokenCommandHandler
        );
    }
}
