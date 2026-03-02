package com.polarishb.pabal.messenger.infrastructure.persistence.read;

import com.polarishb.pabal.messenger.domain.model.entity.ChatRoomMember;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomMemberReadRepository;
import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.entity.ChatRoomMemberEntity;
import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.read.ChatRoomMemberReadJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ChatRoomMemberReadRepositoryImpl implements ChatRoomMemberReadRepository {

    private final ChatRoomMemberReadJpaRepository jpaRepository;

    @Override
    public Optional<ChatRoomMember> findByChatRoomIdAndUserId(UUID chatRoomId, UUID userId) {
        return jpaRepository.findByChatRoomIdAndUserId(chatRoomId, userId)
                .map(ChatRoomMemberEntity::toDomain);
    }

    @Override
    public Optional<ChatRoomMember> findByTenantIdAndChatRoomIdAndUserId(UUID tenantId, UUID chatRoomId, UUID userId) {
        return jpaRepository.findByTenantIdAndChatRoomIdAndUserId(tenantId, chatRoomId, userId)
                .map(ChatRoomMemberEntity::toDomain);
    }
}
