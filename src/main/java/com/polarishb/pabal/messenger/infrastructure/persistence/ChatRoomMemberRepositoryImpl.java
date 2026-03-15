package com.polarishb.pabal.messenger.infrastructure.persistence;

import com.polarishb.pabal.messenger.contract.persistence.chatroommember.PersistedChatRoomMember;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomMemberReadRepository;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomMemberRepository;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomMemberWriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ChatRoomMemberRepositoryImpl implements ChatRoomMemberRepository {

    private final ChatRoomMemberWriteRepository writeRepository;
    private final ChatRoomMemberReadRepository readRepository;

    @Override
    public PersistedChatRoomMember append(PersistedChatRoomMember member) {
        return writeRepository.append(member);
    }

    @Override
    public List<PersistedChatRoomMember> appendAll(List<PersistedChatRoomMember> members) {
        return writeRepository.appendAll(members);
    }

    @Override
    public PersistedChatRoomMember update(PersistedChatRoomMember member) {
        return writeRepository.update(member);
    }

    @Override
    public Optional<PersistedChatRoomMember> findByChatRoomIdAndUserId(UUID chatRoomId, UUID userId) {
        return readRepository.findByChatRoomIdAndUserId(chatRoomId, userId);
    }

    @Override
    public Optional<PersistedChatRoomMember> findByTenantIdAndChatRoomIdAndUserId(UUID tenantId, UUID chatRoomId, UUID userId) {
        return readRepository.findByTenantIdAndChatRoomIdAndUserId(tenantId, chatRoomId, userId);
    }
}
