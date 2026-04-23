package com.polarishb.pabal.messenger.infrastructure.persistence.write;

import com.polarishb.pabal.messenger.contract.persistence.directchatmapping.DirectChatMappingState;
import com.polarishb.pabal.messenger.contract.persistence.directchatmapping.PersistedDirectChatMapping;
import com.polarishb.pabal.messenger.domain.exception.DuplicateDirectChatMappingException;
import com.polarishb.pabal.messenger.domain.model.entity.DirectChatMapping;
import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.write.DirectChatMappingWriteJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DirectChatMappingWriteRepositoryImplTest {

    @Mock
    private DirectChatMappingWriteJpaRepository jpaRepository;

    @InjectMocks
    private DirectChatMappingWriteRepositoryImpl repository;

    @Test
    void append_translates_unique_violation_to_domain_exception_and_preserves_cause() {
        DataIntegrityViolationException cause = new DataIntegrityViolationException("uk_direct_chat_mapping");
        when(jpaRepository.save(any())).thenThrow(cause);

        assertThatThrownBy(() -> repository.append(draftMapping()))
                .isInstanceOf(DuplicateDirectChatMappingException.class)
                .hasCause(cause);
    }

    @Test
    void flush_translates_unique_violation_to_domain_exception() {
        DataIntegrityViolationException cause = new DataIntegrityViolationException("uk_direct_chat_mapping");
        doThrow(cause)
                .when(jpaRepository)
                .flush();

        assertThatThrownBy(repository::flush)
                .isInstanceOf(DuplicateDirectChatMappingException.class)
                .hasCause(cause);
    }

    private PersistedDirectChatMapping draftMapping() {
        UUID mappingId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        Instant now = Instant.parse("2026-04-02T12:00:00Z");

        DirectChatMapping mapping = DirectChatMapping.reconstitute(
                mappingId,
                tenantId,
                chatRoomId,
                userId1.compareTo(userId2) < 0 ? userId1 : userId2,
                userId1.compareTo(userId2) < 0 ? userId2 : userId1,
                now,
                now
        );

        return new PersistedDirectChatMapping(
                mapping,
                new DirectChatMappingState(
                        mappingId,
                        tenantId,
                        chatRoomId,
                        mapping.getUserIdMin(),
                        mapping.getUserIdMax(),
                        now,
                        now,
                        null
                )
        );
    }
}
