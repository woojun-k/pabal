package com.polarishb.pabal.workspace.api.command.http.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateWorkspaceRequest(
        @NotBlank
        @Size(max = 100)
        String name
) {
}
