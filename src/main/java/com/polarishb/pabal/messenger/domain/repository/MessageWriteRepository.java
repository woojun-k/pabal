package com.polarishb.pabal.messenger.domain.repository;

import com.polarishb.pabal.messenger.domain.model.entity.Message;
import com.polarishb.pabal.messenger.domain.repository.result.MessageResult;

public interface MessageWriteRepository {
    MessageResult save(Message message);
}