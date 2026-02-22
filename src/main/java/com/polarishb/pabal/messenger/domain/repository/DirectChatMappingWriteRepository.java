package com.polarishb.pabal.messenger.domain.repository;

import com.polarishb.pabal.messenger.domain.model.entity.DirectChatMapping;
import com.polarishb.pabal.messenger.domain.repository.result.DirectChatMappingResult;

public interface DirectChatMappingWriteRepository {
    DirectChatMappingResult save(DirectChatMapping mapping);
}
