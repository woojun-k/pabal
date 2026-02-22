package com.polarishb.pabal.messenger.infrastructure.persistence.read;

import com.polarishb.pabal.messenger.domain.model.entity.ChatRoom;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomReadRepository;
import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.entity.ChatRoomEntity;
import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.read.ChatRoomReadJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ChatRoomReadRepositoryImpl implements ChatRoomReadRepository {

    private final ChatRoomReadJpaRepository jpaRepository;

    @Override
    public Optional<ChatRoom> findById(UUID chatRoomId) {
        return jpaRepository.findById(chatRoomId).map(ChatRoomEntity::toDomain);
    }
}
