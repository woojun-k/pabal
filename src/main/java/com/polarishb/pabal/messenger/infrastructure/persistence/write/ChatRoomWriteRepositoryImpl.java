package com.polarishb.pabal.messenger.infrastructure.persistence.write;

import com.polarishb.pabal.messenger.contract.persistence.chatroom.ChatRoomPersistenceMapper;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.ChatRoomState;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.PersistedChatRoom;
import com.polarishb.pabal.messenger.domain.model.entity.ChatRoom;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomWriteRepository;
import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.entity.ChatRoomEntity;
import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.write.ChatRoomWriteJpaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Repository
@RequiredArgsConstructor
public class ChatRoomWriteRepositoryImpl implements ChatRoomWriteRepository {

    private final ChatRoomWriteJpaRepository jpaRepository;

    @Override
    @Transactional
    public PersistedChatRoom append(PersistedChatRoom persistedChatRoom) {
        ChatRoomState state = persistedChatRoom.state();
        ChatRoomEntity saved = jpaRepository.save(ChatRoomEntity.fromNewState(state));
        return ChatRoomPersistenceMapper.toPersisted(saved.toState());
    }

    @Override
    @Transactional
    public PersistedChatRoom update(PersistedChatRoom persistedChatRoom) {
        ChatRoomState currentState = persistedChatRoom.state();
        ChatRoom chatRoom = persistedChatRoom.chatRoom();

        ChatRoomEntity entity = jpaRepository.findById(currentState.id())
                .orElseThrow(() -> new EntityNotFoundException("ChatRoom not found"));

        if (!Objects.equals(entity.getVersion(), currentState.version())) {
            throw new ObjectOptimisticLockingFailureException(
                    ChatRoomEntity.class,
                    currentState.id()
            );
        }

        ChatRoomState nextState = ChatRoomPersistenceMapper.toState(
                chatRoom,
                currentState.version()
        );

        entity.apply(nextState);

        return ChatRoomPersistenceMapper.toPersisted(entity.toState());
    }
}
