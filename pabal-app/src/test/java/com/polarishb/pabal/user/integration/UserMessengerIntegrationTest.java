package com.polarishb.pabal.user.integration;

import com.polarishb.pabal.PabalApplication;
import com.polarishb.pabal.messenger.application.port.out.identity.RoomParticipantDirectoryPort;
import com.polarishb.pabal.security.authentication.PabalPrincipal;
import com.polarishb.pabal.support.AbstractPostgresIntegrationTest;
import com.polarishb.pabal.user.application.command.handler.CreateUserCommandHandler;
import com.polarishb.pabal.user.application.command.input.CreateUserCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = PabalApplication.class,
        properties = "pabal.test.context=user-messenger"
)
@ActiveProfiles("test")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UserMessengerIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private CreateUserCommandHandler createUserCommandHandler;

    @Autowired
    private RoomParticipantDirectoryPort roomParticipantDirectoryPort;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void roomParticipantDirectory_resolves_current_principal_and_tenant_users_from_user_module() {
        UUID tenantId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        UUID outsiderId = UUID.randomUUID();
        createUserCommandHandler.handle(new CreateUserCommand(requesterId, tenantId, "Requester"));
        createUserCommandHandler.handle(new CreateUserCommand(participantId, tenantId, "Participant"));
        authenticate(tenantId, requesterId);

        Set<UUID> memberIds = roomParticipantDirectoryPort.findTenantMemberIds(
                tenantId,
                Set.of(requesterId, participantId, outsiderId)
        );

        assertThat(memberIds).containsExactlyInAnyOrder(requesterId, participantId);
    }

    private void authenticate(UUID tenantId, UUID userId) {
        PabalPrincipal principal = new PabalPrincipal(userId, tenantId, userId.toString());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "", List.of())
        );
    }
}
