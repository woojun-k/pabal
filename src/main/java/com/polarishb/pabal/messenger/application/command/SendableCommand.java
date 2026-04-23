package com.polarishb.pabal.messenger.application.command;

import com.polarishb.pabal.common.cqrs.Command;

import java.util.UUID;

public interface SendableCommand extends Command {
    UUID tenantId();
    UUID chatRoomId();
    UUID senderId();
    UUID clientMessageId();
    String content();
}
