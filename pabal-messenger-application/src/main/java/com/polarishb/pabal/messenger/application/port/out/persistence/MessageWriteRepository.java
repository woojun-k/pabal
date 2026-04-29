package com.polarishb.pabal.messenger.application.port.out.persistence;

import com.polarishb.pabal.messenger.contract.persistence.message.PersistedMessage;

public interface MessageWriteRepository {
    PersistedMessage append(PersistedMessage persistedMessage);
    PersistedMessage update(PersistedMessage persistedMessage);
}