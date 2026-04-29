package com.polarishb.pabal.messenger.infrastructure.persistence.jpa.write;

import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.entity.ChatRoomMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ChatRoomMemberWriteJpaRepository extends JpaRepository<ChatRoomMemberEntity, UUID> {
}
