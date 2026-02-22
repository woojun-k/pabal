package com.polarishb.pabal.messenger.infrastructure.persistence.jpa.read;

import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.entity.ChatRoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ChatRoomReadJpaRepository extends JpaRepository<ChatRoomEntity, UUID> {
}
