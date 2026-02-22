package com.polarishb.pabal.messenger.domain.repository;

import com.polarishb.pabal.messenger.domain.model.entity.ChatRoomMember;

import java.util.Optional;
import java.util.UUID;

public interface ChatRoomMemberReadRepository {
    Optional<ChatRoomMember> findByChatRoomIdAndUserId(UUID chatRoomId, UUID userId);
}