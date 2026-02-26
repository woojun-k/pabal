package com.polarishb.pabal.messenger.infrastructure.persistence.jpa.read;

import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.entity.ChatRoomMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ChatRoomMemberReadJpaRepository extends JpaRepository<ChatRoomMemberEntity, UUID> {

    Optional<ChatRoomMemberEntity> findByChatRoomIdAndUserId(UUID chatRoomId, UUID userId);
}
