package com.polarishb.pabal.messenger.infrastructure.persistence.write;

import com.polarishb.pabal.messenger.domain.model.entity.ChatRoomMember;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomMemberWriteRepository;
import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.entity.ChatRoomMemberEntity;
import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.write.ChatRoomMemberWriteJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ChatRoomMemberWriteRepositoryImpl implements ChatRoomMemberWriteRepository {

    private final ChatRoomMemberWriteJpaRepository jpaRepository;

    @Override
    public void save(ChatRoomMember member) {
        ChatRoomMemberEntity entity = ChatRoomMemberEntity.from(member);
        jpaRepository.save(entity);
    }

    @Override
    public void saveAll(List<ChatRoomMember> members) {
        jpaRepository.saveAll(
                members.stream()
                        .map(ChatRoomMemberEntity::from)
                        .toList()
        );
    }
}
