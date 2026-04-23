package com.polarishb.pabal.messenger.domain.repository;

import com.polarishb.pabal.messenger.contract.persistence.directchatmapping.PersistedDirectChatMapping;

public interface DirectChatMappingWriteRepository {
    PersistedDirectChatMapping append(PersistedDirectChatMapping persistedMapping);
    PersistedDirectChatMapping update(PersistedDirectChatMapping persistedMapping);
    void flush();
}
