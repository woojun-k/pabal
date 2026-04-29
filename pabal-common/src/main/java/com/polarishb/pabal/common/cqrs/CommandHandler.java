package com.polarishb.pabal.common.cqrs;

public interface CommandHandler<C extends Command, R> {
    R handle(C command);
}
