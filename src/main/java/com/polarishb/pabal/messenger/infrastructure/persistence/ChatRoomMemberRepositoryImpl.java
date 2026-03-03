package com.polarishb.pabal.messenger.infrastructure.persistence;

import com.polarishb.pabal.messenger.domain.model.entity.ChatRoomMember;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomMemberReadRepository;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomMemberRepository;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomMemberWriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ChatRoomMemberRepositoryImpl
        implements ChatRoomMemberRepository {

    private final ChatRoomMemberWriteRepository writeRepository;
    private final ChatRoomMemberReadRepository readRepository;

    @Override
    public void save(ChatRoomMember member) {
        writeRepository.save(member);
    }

    @Override
    public void saveAll(Iterable<ChatRoomMember> members) { writeRepository.saveAll(members); }

    @Override
    public Optional<ChatRoomMember> findByChatRoomIdAndUserId(UUID chatRoomId, UUID userId) {
        return readRepository.findByChatRoomIdAndUserId(chatRoomId, userId);
    }

    @Override
    public Optional<ChatRoomMember> findByTenantIdAndChatRoomIdAndUserId(UUID tenantId, UUID chatRoomId, UUID userId) {
        return readRepository.findByTenantIdAndChatRoomIdAndUserId(tenantId, chatRoomId, userId);
    }
}