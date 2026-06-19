package com.polarishb.pabal.messenger.realtime;

import com.polarishb.pabal.PabalApplication;
import com.polarishb.pabal.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = PabalApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@Testcontainers
class ChatWebSocketMvpIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String MESSAGE_SEND_DESTINATION = "/app/chat.message.send";

    @LocalServerPort
    private int port;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private SimpUserRegistry simpUserRegistry;

    @Test
    void stomp_send_message_broadcasts_message_sent_event_to_room_subscriber() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID clientMessageId = UUID.randomUUID();
        String content = "hello over stomp";
        insertActiveRoomWithMembers(tenantId, chatRoomId, senderId, receiverId);

        StompTestClient receiverClient = stompClient();
        StompTestClient senderClient = stompClient();
        StompSession receiverSession = null;
        StompSession senderSession = null;

        try {
            receiverSession = connect(receiverClient.client(), tokenFor(receiverId, tenantId));
            senderSession = connect(senderClient.client(), tokenFor(senderId, tenantId));

            BlockingQueue<Map<String, Object>> receivedEvents = new LinkedBlockingQueue<>();
            String roomEventsTopic = roomEventsTopic(tenantId, chatRoomId);
            receiverSession.subscribe(
                    roomEventsTopic,
                    mapFrameHandler(receivedEvents)
            );
            awaitSubscription(roomEventsTopic);

            senderSession.send(MESSAGE_SEND_DESTINATION, Map.of(
                    "tenantId", tenantId.toString(),
                    "chatRoomId", chatRoomId.toString(),
                    "clientMessageId", clientMessageId.toString(),
                    "content", content
            ));

            Map<String, Object> event = receivedEvents.poll(10, TimeUnit.SECONDS);

            assertThat(event).isNotNull();
            assertThat(event.get("type")).isEqualTo("MESSAGE_SENT");
            assertThat(event.get("tenantId")).isEqualTo(tenantId.toString());
            assertThat(event.get("chatRoomId")).isEqualTo(chatRoomId.toString());
            assertThat(((Number) event.get("sequence")).longValue()).isEqualTo(1L);

            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) event.get("payload");
            assertThat(payload.get("messageId")).isNotNull();
            assertThat(payload.get("chatRoomId")).isEqualTo(chatRoomId.toString());
            assertThat(((Number) payload.get("sequence")).longValue()).isEqualTo(1L);
            assertThat(payload.get("senderId")).isEqualTo(senderId.toString());
            assertThat(payload.get("clientMessageId")).isEqualTo(clientMessageId.toString());
            assertThat(payload.get("content")).isEqualTo(content);
        } finally {
            disconnect(senderSession);
            disconnect(receiverSession);
            senderClient.close();
            receiverClient.close();
        }
    }

    private void awaitSubscription(String destination) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            if (hasSubscription(destination)) {
                return;
            }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(25));
        }

        assertThat(hasSubscription(destination))
                .as("STOMP subscription registered for %s", destination)
                .isTrue();
    }

    private boolean hasSubscription(String destination) {
        return !simpUserRegistry.findSubscriptions(subscription -> destination.equals(subscription.getDestination()))
                .isEmpty();
    }

    private StompTestClient stompClient() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("stomp-test-");
        scheduler.initialize();

        SockJsClient sockJsClient = new SockJsClient(List.of(
                new WebSocketTransport(new StandardWebSocketClient())
        ));
        WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setTaskScheduler(scheduler);
        stompClient.setMessageConverter(new JacksonJsonMessageConverter());
        return new StompTestClient(stompClient, scheduler);
    }

    private StompSession connect(WebSocketStompClient stompClient, String accessToken) throws Exception {
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);

        return stompClient.connectAsync(
                "http://localhost:" + port + "/ws",
                new WebSocketHttpHeaders(),
                connectHeaders,
                new StompSessionHandlerAdapter() {
                }
        ).get(5, TimeUnit.SECONDS);
    }

    private StompFrameHandler mapFrameHandler(BlockingQueue<Map<String, Object>> queue) {
        return new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void handleFrame(StompHeaders headers, Object payload) {
                queue.offer((Map<String, Object>) payload);
            }
        };
    }

    private String tokenFor(UUID userId, UUID tenantId) throws Exception {
        URI uri = URI.create(
                "http://localhost:" + port + "/dev/token?userId=" + userId + "&tenantId=" + tenantId
        );
        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        return extractAccessToken(response.body());
    }

    private String extractAccessToken(String responseBody) {
        java.util.regex.Matcher matcher = Pattern.compile("\"accessToken\"\\s*:\\s*\"([^\"]+)\"")
                .matcher(responseBody);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private void insertActiveRoomWithMembers(UUID tenantId, UUID chatRoomId, UUID senderId, UUID receiverId)
            throws Exception {
        OffsetDateTime now = OffsetDateTime.parse("2026-04-08T00:00:00Z");

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                insertActiveTenant(connection, tenantId, now);
                insertActiveUser(connection, tenantId, senderId, "Sender", now);
                insertActiveUser(connection, tenantId, receiverId, "Receiver", now);
                insertActiveRoom(connection, tenantId, chatRoomId, senderId, now);
                insertActiveMember(connection, tenantId, chatRoomId, senderId, now);
                insertActiveMember(connection, tenantId, chatRoomId, receiverId, now);
                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private void insertActiveTenant(Connection connection, UUID tenantId, OffsetDateTime now) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO pabal_tenant (
                            id,
                            name,
                            status,
                            version,
                            created_at,
                            updated_at
                        )
                        VALUES (?, 'mvp-tenant', 'ACTIVE', 0, ?, ?)
                        """)) {
            statement.setObject(1, tenantId);
            statement.setObject(2, now);
            statement.setObject(3, now);
            statement.executeUpdate();
        }
    }

    private void insertActiveUser(
            Connection connection,
            UUID tenantId,
            UUID userId,
            String name,
            OffsetDateTime now
    ) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO tenant_user (
                            id,
                            tenant_id,
                            name,
                            status,
                            version,
                            created_at,
                            updated_at
                        )
                        VALUES (?, ?, ?, 'ACTIVE', 0, ?, ?)
                        """)) {
            statement.setObject(1, userId);
            statement.setObject(2, tenantId);
            statement.setString(3, name);
            statement.setObject(4, now);
            statement.setObject(5, now);
            statement.executeUpdate();
        }
    }

    private void insertActiveRoom(
            Connection connection,
            UUID tenantId,
            UUID chatRoomId,
            UUID senderId,
            OffsetDateTime now
    ) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO chat_room (
                            id,
                            type,
                            name,
                            created_by,
                            tenant_id,
                            is_private,
                            status,
                            last_message_sequence,
                            version,
                            created_at,
                            updated_at
                        )
                        VALUES (?, 'GROUP', 'mvp-room', ?, ?, false, 'ACTIVE', 0, 0, ?, ?)
                        """)) {
            statement.setObject(1, chatRoomId);
            statement.setObject(2, senderId);
            statement.setObject(3, tenantId);
            statement.setObject(4, now);
            statement.setObject(5, now);
            statement.executeUpdate();
        }
    }

    private void insertActiveMember(
            Connection connection,
            UUID tenantId,
            UUID chatRoomId,
            UUID userId,
            OffsetDateTime joinedAt
    )
            throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO chat_room_member (
                            id,
                            tenant_id,
                            chat_room_id,
                            user_id,
                            last_read_sequence,
                            joined_at,
                            version,
                            created_at,
                            updated_at
                        )
                        VALUES (?, ?, ?, ?, 0, ?, 0, ?, ?)
                        """)) {
            statement.setObject(1, UUID.randomUUID());
            statement.setObject(2, tenantId);
            statement.setObject(3, chatRoomId);
            statement.setObject(4, userId);
            statement.setObject(5, joinedAt);
            statement.setObject(6, joinedAt);
            statement.setObject(7, joinedAt);
            statement.executeUpdate();
        }
    }

    private String roomEventsTopic(UUID tenantId, UUID chatRoomId) {
        return "/topic/tenants/" + tenantId + "/chat-rooms/" + chatRoomId + "/events";
    }

    private void disconnect(StompSession session) {
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }

    private record StompTestClient(
            WebSocketStompClient client,
            ThreadPoolTaskScheduler scheduler
    ) implements AutoCloseable {

        @Override
        public void close() {
            client.stop();
            scheduler.shutdown();
        }
    }
}
