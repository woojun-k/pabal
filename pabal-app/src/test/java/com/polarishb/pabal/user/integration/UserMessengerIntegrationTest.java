package com.polarishb.pabal.user.integration;

import com.polarishb.pabal.PabalApplication;
import com.polarishb.pabal.messenger.application.port.out.identity.RoomParticipantDirectoryPort;
import com.polarishb.pabal.support.AbstractPostgresIntegrationTest;
import com.polarishb.pabal.tenant.application.command.handler.CreateTenantCommandHandler;
import com.polarishb.pabal.tenant.application.command.input.CreateTenantCommand;
import com.polarishb.pabal.tenant.application.command.output.CreateTenantResult;
import com.polarishb.pabal.user.application.command.handler.CreateUserCommandHandler;
import com.polarishb.pabal.user.application.command.input.CreateUserCommand;
import com.polarishb.pabal.workspace.application.command.handler.CreateWorkspaceCommandHandler;
import com.polarishb.pabal.workspace.application.command.input.CreateWorkspaceCommand;
import com.polarishb.pabal.workspace.application.command.output.CreateWorkspaceResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Testcontainers;

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
    private CreateTenantCommandHandler createTenantCommandHandler;

    @Autowired
    private CreateUserCommandHandler createUserCommandHandler;

    @Autowired
    private CreateWorkspaceCommandHandler createWorkspaceCommandHandler;

    @Autowired
    private RoomParticipantDirectoryPort roomParticipantDirectoryPort;

    @Test
    void roomParticipantDirectory_resolves_active_tenant_users_from_user_module() {
        UUID tenantId = createTenant("Tenant").tenantId();
        UUID requesterId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        UUID outsiderId = UUID.randomUUID();
        createUserCommandHandler.handle(new CreateUserCommand(requesterId, tenantId, "Requester"));
        createUserCommandHandler.handle(new CreateUserCommand(participantId, tenantId, "Participant"));

        Set<UUID> memberIds = roomParticipantDirectoryPort.findTenantMemberIds(
                tenantId,
                Set.of(requesterId, participantId, outsiderId)
        );

        assertThat(memberIds).containsExactlyInAnyOrder(requesterId, participantId);
    }

    @Test
    void roomParticipantDirectory_resolves_workspace_members_from_workspace_module() {
        UUID tenantId = createTenant("Tenant").tenantId();
        UUID ownerId = UUID.randomUUID();
        UUID tenantUserId = UUID.randomUUID();
        UUID outsiderId = UUID.randomUUID();
        createUserCommandHandler.handle(new CreateUserCommand(ownerId, tenantId, "Owner"));
        createUserCommandHandler.handle(new CreateUserCommand(tenantUserId, tenantId, "Tenant User"));
        CreateWorkspaceResult workspace = createWorkspaceCommandHandler.handle(
                new CreateWorkspaceCommand(tenantId, ownerId, "Workspace")
        );

        Set<UUID> memberIds = roomParticipantDirectoryPort.findWorkspaceMemberIds(
                tenantId,
                workspace.workspaceId(),
                Set.of(ownerId, tenantUserId, outsiderId)
        );

        assertThat(memberIds).containsExactly(ownerId);
    }

    private CreateTenantResult createTenant(String name) {
        return createTenantCommandHandler.handle(new CreateTenantCommand(name));
    }
}
