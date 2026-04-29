package com.polarishb.pabal.messenger.infrastructure.persistence;

import com.polarishb.pabal.messenger.domain.repository.ChatRoomSequenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ChatRoomSequenceRepositoryImpl implements ChatRoomSequenceRepository {

    private static final String ALLOCATE_NEXT_SEQUENCE_SQL = """
            update chat_room
               set last_message_sequence = coalesce(last_message_sequence, 0) + 1
             where tenant_id = :tenantId
               and id = :chatRoomId
         returning last_message_sequence
        """;

    private static final String UPDATE_LAST_MESSAGE_SNAPSHOT_SQL = """
            update chat_room
               set last_message_id = :messageId,
                   last_message_at = :messageAt,
                   updated_at = :messageAt
             where tenant_id = :tenantId
               and id = :chatRoomId
               and last_message_sequence = :messageSequence
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public long allocateNextMessageSequence(UUID tenantId, UUID chatRoomId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("chatRoomId", chatRoomId);

        Long sequence = jdbcTemplate.query(
                ALLOCATE_NEXT_SEQUENCE_SQL,
                parameters,
                rs -> rs.next() ? readSequence(rs) : null
        );

        if (sequence == null) {
            throw new IllegalStateException("Failed to allocate message sequence for room: " + chatRoomId);
        }

        return sequence;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void updateLastMessageSnapshot(
            UUID tenantId,
            UUID chatRoomId,
            UUID messageId,
            long messageSequence,
            Instant messageAt
    ) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("chatRoomId", chatRoomId)
                .addValue("messageId", messageId)
                .addValue("messageSequence", messageSequence)
                .addValue("messageAt", messageAt);

        jdbcTemplate.update(UPDATE_LAST_MESSAGE_SNAPSHOT_SQL, parameters);
    }

    private long readSequence(ResultSet rs) throws SQLException {
        return rs.getLong("last_message_sequence");
    }

}
