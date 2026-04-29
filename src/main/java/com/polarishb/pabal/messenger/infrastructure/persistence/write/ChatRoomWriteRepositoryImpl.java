package com.polarishb.pabal.messenger.infrastructure.persistence.write;

import com.polarishb.pabal.messenger.contract.persistence.chatroom.ChatRoomPersistenceMapper;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.ChatRoomState;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.PersistedChatRoom;
import com.polarishb.pabal.messenger.domain.exception.DuplicateChannelNameException;
import com.polarishb.pabal.messenger.domain.model.entity.ChatRoom;
import com.polarishb.pabal.messenger.domain.model.type.RoomType;
import com.polarishb.pabal.messenger.domain.model.vo.ChannelName;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomWriteRepository;
import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.entity.ChatRoomEntity;
import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.write.ChatRoomWriteJpaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Repository
@RequiredArgsConstructor
public class ChatRoomWriteRepositoryImpl implements ChatRoomWriteRepository {

    private final ChatRoomWriteJpaRepository jpaRepository;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public PersistedChatRoom append(PersistedChatRoom persistedChatRoom) {
        ChatRoomState state = persistedChatRoom.state();
        try {
            ChatRoomEntity saved = jpaRepository.save(ChatRoomEntity.fromNewState(state));
            return ChatRoomPersistenceMapper.toPersisted(saved.toState());
        } catch (DataIntegrityViolationException e) {
            throw translateAppendViolation(state, e);
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
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

    private RuntimeException translateAppendViolation(ChatRoomState state, DataIntegrityViolationException cause) {
        if (state.type() == RoomType.CHANNEL && isChannelNameUniqueViolation(cause)) {
            return new DuplicateChannelNameException(
                    state.channelSettings() == null ? null : state.channelSettings().workspaceId(),
                    state.name() == null ? null : new ChannelName(state.name())
            );
        }
        return cause;
    }

    private boolean isChannelNameUniqueViolation(DataIntegrityViolationException cause) {
        Throwable current = cause;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains("uq_chat_room_channel_name_alive")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
