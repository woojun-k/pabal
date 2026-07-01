package com.polarishb.pabal.messenger.infrastructure.persistence.read;

import com.polarishb.pabal.messenger.contract.persistence.chatroommember.ChatRoomMemberState;
import com.polarishb.pabal.messenger.application.port.out.persistence.ChatRoomMemberReadRepository;
import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.entity.ChatRoomMemberEntity;
import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.read.ChatRoomMemberReadJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ChatRoomMemberReadRepositoryImpl implements ChatRoomMemberReadRepository {

    private final ChatRoomMemberReadJpaRepository jpaRepository;

    @Override
    public Optional<ChatRoomMemberState> findByTenantIdAndChatRoomIdAndUserId(UUID tenantId, UUID chatRoomId, UUID userId) {
        return jpaRepository.findByTenantIdAndChatRoomIdAndUserId(tenantId, chatRoomId, userId)
                .map(ChatRoomMemberEntity::toState);
    }

    @Override
    public List<ChatRoomMemberState> findAllActiveByTenantIdAndUserId(UUID tenantId, UUID userId) {
        return jpaRepository.findAllByTenantIdAndUserIdAndLeftAtIsNull(tenantId, userId).stream()
                .map(ChatRoomMemberEntity::toState)
                .toList();
    }
}
