package com.polarishb.pabal.messenger.application.command.output;

import java.util.UUID;

public record GetOrCreateDirectRoomResult(
        UUID chatRoomId
) {}
