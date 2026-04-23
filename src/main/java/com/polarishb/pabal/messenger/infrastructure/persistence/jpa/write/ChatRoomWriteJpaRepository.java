package com.polarishb.pabal.messenger.infrastructure.persistence.jpa.write;

import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.entity.ChatRoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ChatRoomWriteJpaRepository extends JpaRepository<ChatRoomEntity, UUID> {
}
