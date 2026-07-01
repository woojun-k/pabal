package com.polarishb.pabal.messenger.application.port.out.persistence;

import com.polarishb.pabal.messenger.contract.persistence.message.MessageState;
import com.polarishb.pabal.messenger.contract.persistence.message.PersistedMessage;

public interface MessageWriteRepository {
    MessageState append(PersistedMessage persistedMessage);
    MessageState update(PersistedMessage persistedMessage);
}
