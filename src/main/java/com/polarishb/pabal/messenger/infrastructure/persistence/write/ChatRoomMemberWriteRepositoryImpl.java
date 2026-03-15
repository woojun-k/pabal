package com.polarishb.pabal.messenger.infrastructure.persistence.write;

import com.polarishb.pabal.messenger.contract.persistence.chatroommember.ChatRoomMemberPersistenceMapper;
import com.polarishb.pabal.messenger.contract.persistence.chatroommember.ChatRoomMemberState;
import com.polarishb.pabal.messenger.contract.persistence.chatroommember.PersistedChatRoomMember;
import com.polarishb.pabal.messenger.domain.model.entity.ChatRoomMember;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomMemberWriteRepository;
import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.entity.ChatRoomMemberEntity;
import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.write.ChatRoomMemberWriteJpaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Repository
@RequiredArgsConstructor
public class ChatRoomMemberWriteRepositoryImpl implements ChatRoomMemberWriteRepository {

    private final ChatRoomMemberWriteJpaRepository jpaRepository;

    @Override
    @Transactional
    public PersistedChatRoomMember append(PersistedChatRoomMember persistedMember) {
        ChatRoomMemberState state = persistedMember.state();
        ChatRoomMemberEntity saved = jpaRepository.save(ChatRoomMemberEntity.fromNewState(state));
        return ChatRoomMemberPersistenceMapper.toPersisted(saved.toState());
    }

    @Override
    @Transactional
    public List<PersistedChatRoomMember> appendAll(List<PersistedChatRoomMember> members) {
        List<ChatRoomMemberEntity> entities = members.stream()
                .map(m -> ChatRoomMemberEntity.fromNewState(m.state()))
                .toList();

        List<ChatRoomMemberEntity> saved = jpaRepository.saveAll(entities);

        return saved.stream()
                .map(e -> ChatRoomMemberPersistenceMapper.toPersisted(e.toState()))
                .toList();
    }

    @Override
    @Transactional
    public PersistedChatRoomMember update(PersistedChatRoomMember persistedMember) {
        ChatRoomMemberState currentState = persistedMember.state();
        ChatRoomMember member = persistedMember.member();

        ChatRoomMemberEntity entity = jpaRepository.findById(currentState.id())
                .orElseThrow(() -> new EntityNotFoundException("ChatRoomMember not found"));

        if (!Objects.equals(entity.getVersion(), currentState.version())) {
            throw new ObjectOptimisticLockingFailureException(
                    ChatRoomMemberEntity.class,
                    currentState.id()
            );
        }

        ChatRoomMemberState nextState = ChatRoomMemberPersistenceMapper.toState(
                member,
                currentState.version()
        );

        entity.apply(nextState);

        return ChatRoomMemberPersistenceMapper.toPersisted(entity.toState());
    }
}
