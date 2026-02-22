package com.polarishb.pabal.messenger.infrastructure.persistence.write;

import com.polarishb.pabal.messenger.domain.exception.ChatRoomNotFoundException;
import com.polarishb.pabal.messenger.domain.model.entity.ChatRoom;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomWriteRepository;
import com.polarishb.pabal.messenger.domain.repository.result.ChatRoomResult;
import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.entity.ChatRoomEntity;
import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.write.ChatRoomWriteJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ChatRoomWriteRepositoryImpl implements ChatRoomWriteRepository {

    private final ChatRoomWriteJpaRepository jpaRepository;

    @Override
    public ChatRoomResult save(ChatRoom chatRoom) {
        ChatRoomEntity entity = ChatRoomEntity.from(chatRoom);
        ChatRoomEntity saved = jpaRepository.save(entity);
        return new ChatRoomResult(saved.getId());
    }

    @Override
    public void remove(UUID chatRoomId) {
        ChatRoomEntity entity = jpaRepository.findById(chatRoomId)
                .orElseThrow(() -> new ChatRoomNotFoundException(chatRoomId));

        entity.delete();
        jpaRepository.save(entity);
    }
}
