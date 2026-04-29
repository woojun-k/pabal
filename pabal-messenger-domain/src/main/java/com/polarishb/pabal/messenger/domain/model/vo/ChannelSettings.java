package com.polarishb.pabal.messenger.domain.model.vo;

import java.util.UUID;

public record ChannelSettings(
        UUID workspaceId,
        boolean isPrivate,
        String description
) {

    public static ChannelSettings create(UUID workspaceId) {
        return new ChannelSettings(workspaceId, false, null);
    }

    public ChannelSettings withDescription(String newDescription) {
        return new ChannelSettings(
                this.workspaceId,
                this.isPrivate,
                newDescription
        );
    }

    public ChannelSettings withPrivacy(boolean isPrivate) {
        return new ChannelSettings(
                this.workspaceId,
                isPrivate,
                this.description
        );
    }
}