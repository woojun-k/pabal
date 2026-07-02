package com.polarishb.pabal.messenger.realtime;

import com.polarishb.pabal.support.PabalSpringBootIntegrationTest;
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
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import javax.sql.DataSource;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

@PabalSpringBootIntegrationTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatWebSocketMvpIntegrationTest {

    private static final String MESSAGE_SEND_DESTINATION = "/app/chat.message.send";
    private static final String MESSAGE_EDIT_DESTINATION = "/app/chat.message.edit";
    private static final String MESSAGE_DELETE_DESTINATION = "/app/chat.message.delete";
    private static final int NO_EVENT_TIMEOUT_SECONDS = 2;

    @LocalServerPort
    private int port;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private SimpUserRegistry simpUserRegistry;

    @Test
    void stomp_send_edit_delete_broadcasts_events_to_sender_and_receiver() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();
        UUID userAId = UUID.randomUUID();
        UUID userBId = UUID.randomUUID();
        UUID clientMessageId = UUID.randomUUID();
        String originalContent = "hello over stomp";
        String editedContent = "edited over stomp";
        insertActiveRoomWithMembers(tenantId, chatRoomId, userAId, userBId);

        StompTestClient userAClient = stompClient();
        StompTestClient userBClient = stompClient();
        StompSession userASession = null;
        StompSession userBSession = null;

        try {
            userASession = connect(userAClient.client(), tokenFor(userAId, tenantId));
            userBSession = connect(userBClient.client(), tokenFor(userBId, tenantId));

            BlockingQueue<Map<String, Object>> userAEvents = new LinkedBlockingQueue<>();
            BlockingQueue<Map<String, Object>> userBEvents = new LinkedBlockingQueue<>();
            String roomEventsTopic = roomEventsTopic(tenantId, chatRoomId);
            userASession.subscribe(roomEventsTopic, mapFrameHandler(userAEvents));
            userBSession.subscribe(roomEventsTopic, mapFrameHandler(userBEvents));
            awaitSubscriptionCount(roomEventsTopic, 2);

            sendMessage(userASession, tenantId, chatRoomId, clientMessageId, originalContent);

            Map<String, Object> userASent = awaitRoomEvent(userAEvents, "MESSAGE_SENT", tenantId, chatRoomId);
            Map<String, Object> userBSent = awaitRoomEvent(userBEvents, "MESSAGE_SENT", tenantId, chatRoomId);
            Map<String, Object> sentPayload = payload(userASent);
            UUID messageId = UUID.fromString(sentPayload.get("messageId").toString());

            assertThat(userBSent.get("eventId")).isEqualTo(userASent.get("eventId"));
            assertThat(payload(userBSent).get("messageId")).isEqualTo(messageId.toString());
            assertThat(sentPayload.get("senderId")).isEqualTo(userAId.toString());
            assertThat(sentPayload.get("clientMessageId")).isEqualTo(clientMessageId.toString());
            assertThat(sentPayload.get("content")).isEqualTo(originalContent);
            assertThat(((Number) sentPayload.get("sequence")).longValue()).isEqualTo(1L);

            editMessage(userASession, tenantId, chatRoomId, messageId, editedContent);

            Map<String, Object> userAEdited = awaitRoomEvent(userAEvents, "MESSAGE_EDITED", tenantId, chatRoomId);
            Map<String, Object> userBEdited = awaitRoomEvent(userBEvents, "MESSAGE_EDITED", tenantId, chatRoomId);
            assertMessageEditedPayload(payload(userAEdited), messageId, chatRoomId, editedContent);
            assertThat(userBEdited.get("eventId")).isEqualTo(userAEdited.get("eventId"));
            assertMessageEditedPayload(payload(userBEdited), messageId, chatRoomId, editedContent);

            deleteMessage(userASession, tenantId, chatRoomId, messageId);

            Map<String, Object> userADeleted = awaitRoomEvent(userAEvents, "MESSAGE_DELETED", tenantId, chatRoomId);
            Map<String, Object> userBDeleted = awaitRoomEvent(userBEvents, "MESSAGE_DELETED", tenantId, chatRoomId);
            assertMessageDeletedPayload(payload(userADeleted), messageId, chatRoomId);
            assertThat(userBDeleted.get("eventId")).isEqualTo(userADeleted.get("eventId"));
            assertMessageDeletedPayload(payload(userBDeleted), messageId, chatRoomId);

            MessageRow messageRow = findMessage(tenantId, chatRoomId, messageId);
            assertThat(messageRow.status()).isEqualTo("DELETED");
            assertThat(messageRow.content()).isEqualTo("[deleted]");
            assertThat(messageRow.deletedAt()).isNotNull();
        } finally {
            disconnect(userASession);
            disconnect(userBSession);
            userAClient.close();
            userBClient.close();
        }
    }

    @Test
    void stomp_non_member_send_does_not_broadcast_or_persist_message() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();
        UUID userAId = UUID.randomUUID();
        UUID userBId = UUID.randomUUID();
        UUID nonMemberId = UUID.randomUUID();
        insertActiveRoomWithMembers(tenantId, chatRoomId, userAId, userBId);
        insertActiveTenantUser(tenantId, nonMemberId, "Non Member");

        StompTestClient subscriberClient = stompClient();
        StompTestClient nonMemberClient = stompClient();
        StompSession subscriberSession = null;
        StompSession nonMemberSession = null;

        try {
            subscriberSession = connect(subscriberClient.client(), tokenFor(userAId, tenantId));
            nonMemberSession = connect(nonMemberClient.client(), tokenFor(nonMemberId, tenantId));

            BlockingQueue<Map<String, Object>> roomEvents = new LinkedBlockingQueue<>();
            String roomEventsTopic = roomEventsTopic(tenantId, chatRoomId);
            subscriberSession.subscribe(roomEventsTopic, mapFrameHandler(roomEvents));
            awaitSubscriptionCount(roomEventsTopic, 1);

            sendMessage(
                    nonMemberSession,
                    tenantId,
                    chatRoomId,
                    UUID.randomUUID(),
                    "non member message"
            );

            assertNoRoomEvent(roomEvents);
            assertThat(countMessages(tenantId, chatRoomId)).isZero();
        } finally {
            disconnect(nonMemberSession);
            disconnect(subscriberSession);
            nonMemberClient.close();
            subscriberClient.close();
        }
    }

    @Test
    void stomp_non_sender_cannot_edit_or_delete_sender_message() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID otherMemberId = UUID.randomUUID();
        UUID clientMessageId = UUID.randomUUID();
        String originalContent = "owned message";
        insertActiveRoomWithMembers(tenantId, chatRoomId, senderId, otherMemberId);

        StompTestClient subscriberClient = stompClient();
        StompTestClient senderClient = stompClient();
        StompTestClient editAttemptClient = stompClient();
        StompTestClient deleteAttemptClient = stompClient();
        StompSession subscriberSession = null;
        StompSession senderSession = null;
        StompSession editAttemptSession = null;
        StompSession deleteAttemptSession = null;

        try {
            subscriberSession = connect(subscriberClient.client(), tokenFor(senderId, tenantId));
            senderSession = connect(senderClient.client(), tokenFor(senderId, tenantId));
            editAttemptSession = connect(editAttemptClient.client(), tokenFor(otherMemberId, tenantId));
            deleteAttemptSession = connect(deleteAttemptClient.client(), tokenFor(otherMemberId, tenantId));

            BlockingQueue<Map<String, Object>> roomEvents = new LinkedBlockingQueue<>();
            String roomEventsTopic = roomEventsTopic(tenantId, chatRoomId);
            subscriberSession.subscribe(roomEventsTopic, mapFrameHandler(roomEvents));
            awaitSubscriptionCount(roomEventsTopic, 1);

            sendMessage(senderSession, tenantId, chatRoomId, clientMessageId, originalContent);
            UUID messageId = UUID.fromString(payload(
                    awaitRoomEvent(roomEvents, "MESSAGE_SENT", tenantId, chatRoomId)
            ).get("messageId").toString());

            editMessage(editAttemptSession, tenantId, chatRoomId, messageId, "hijacked edit");

            assertNoRoomEvent(roomEvents);
            MessageRow afterEditAttempt = findMessage(tenantId, chatRoomId, messageId);
            assertThat(afterEditAttempt.status()).isEqualTo("ACTIVE");
            assertThat(afterEditAttempt.content()).isEqualTo(originalContent);

            deleteMessage(deleteAttemptSession, tenantId, chatRoomId, messageId);

            assertNoRoomEvent(roomEvents);
            MessageRow afterDeleteAttempt = findMessage(tenantId, chatRoomId, messageId);
            assertThat(afterDeleteAttempt.status()).isEqualTo("ACTIVE");
            assertThat(afterDeleteAttempt.content()).isEqualTo(originalContent);
            assertThat(afterDeleteAttempt.deletedAt()).isNull();
        } finally {
            disconnect(deleteAttemptSession);
            disconnect(editAttemptSession);
            disconnect(senderSession);
            disconnect(subscriberSession);
            deleteAttemptClient.close();
            editAttemptClient.close();
            senderClient.close();
            subscriberClient.close();
        }
    }

    @Test
    void stomp_deleted_message_cannot_be_edited_again() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID clientMessageId = UUID.randomUUID();
        insertActiveRoomWithMembers(tenantId, chatRoomId, senderId, receiverId);

        StompTestClient subscriberClient = stompClient();
        StompTestClient senderClient = stompClient();
        StompSession subscriberSession = null;
        StompSession senderSession = null;

        try {
            subscriberSession = connect(subscriberClient.client(), tokenFor(receiverId, tenantId));
            senderSession = connect(senderClient.client(), tokenFor(senderId, tenantId));

            BlockingQueue<Map<String, Object>> roomEvents = new LinkedBlockingQueue<>();
            String roomEventsTopic = roomEventsTopic(tenantId, chatRoomId);
            subscriberSession.subscribe(roomEventsTopic, mapFrameHandler(roomEvents));
            awaitSubscriptionCount(roomEventsTopic, 1);

            sendMessage(senderSession, tenantId, chatRoomId, clientMessageId, "message to delete");
            UUID messageId = UUID.fromString(payload(
                    awaitRoomEvent(roomEvents, "MESSAGE_SENT", tenantId, chatRoomId)
            ).get("messageId").toString());

            deleteMessage(senderSession, tenantId, chatRoomId, messageId);
            awaitRoomEvent(roomEvents, "MESSAGE_DELETED", tenantId, chatRoomId);

            editMessage(senderSession, tenantId, chatRoomId, messageId, "edit after delete");

            assertNoRoomEvent(roomEvents);
            MessageRow messageRow = findMessage(tenantId, chatRoomId, messageId);
            assertThat(messageRow.status()).isEqualTo("DELETED");
            assertThat(messageRow.content()).isEqualTo("[deleted]");
            assertThat(messageRow.deletedAt()).isNotNull();
        } finally {
            disconnect(senderSession);
            disconnect(subscriberSession);
            senderClient.close();
            subscriberClient.close();
        }
    }

    @Test
    void stomp_tenant_mismatch_command_does_not_broadcast_or_persist_message() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID otherTenantId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        insertActiveRoomWithMembers(tenantId, chatRoomId, senderId, receiverId);

        StompTestClient subscriberClient = stompClient();
        StompTestClient senderClient = stompClient();
        StompSession subscriberSession = null;
        StompSession senderSession = null;

        try {
            subscriberSession = connect(subscriberClient.client(), tokenFor(receiverId, tenantId));
            senderSession = connect(senderClient.client(), tokenFor(senderId, tenantId));

            BlockingQueue<Map<String, Object>> roomEvents = new LinkedBlockingQueue<>();
            String roomEventsTopic = roomEventsTopic(tenantId, chatRoomId);
            subscriberSession.subscribe(roomEventsTopic, mapFrameHandler(roomEvents));
            awaitSubscriptionCount(roomEventsTopic, 1);

            sendMessage(
                    senderSession,
                    otherTenantId,
                    chatRoomId,
                    UUID.randomUUID(),
                    "tenant mismatch"
            );

            assertNoRoomEvent(roomEvents);
            assertThat(countMessages(tenantId, chatRoomId)).isZero();
        } finally {
            disconnect(senderSession);
            disconnect(subscriberSession);
            senderClient.close();
            subscriberClient.close();
        }
    }

    @Test
    void cross_tenant_rest_and_stomp_access_to_foreign_room_is_blocked() throws Exception {
        UUID tenantAId = UUID.randomUUID();
        UUID tenantBId = UUID.randomUUID();
        UUID tenantAUserId = UUID.randomUUID();
        UUID tenantBUserId = UUID.randomUUID();
        UUID tenantBOtherMemberId = UUID.randomUUID();
        UUID tenantBRoomId = UUID.randomUUID();
        insertActiveTenantWithUser(tenantAId, tenantAUserId, "Tenant A User");
        insertActiveRoomWithMembers(tenantBId, tenantBRoomId, tenantBUserId, tenantBOtherMemberId);

        String tenantAAccessToken = tokenFor(tenantAUserId, tenantAId);
        HttpResponse<String> listMessagesResponse = authenticatedGet(
                "/api/v1/chat-rooms/" + tenantBRoomId + "/messages",
                tenantAAccessToken
        );
        assertThat(listMessagesResponse.statusCode()).isEqualTo(404);

        UUID clientMessageId = UUID.randomUUID();
        HttpResponse<String> sendMessageResponse = authenticatedPost(
                "/api/v1/chat-rooms/" + tenantBRoomId + "/messages",
                """
                {
                  "clientMessageId": "%s",
                  "content": "cross tenant rest attempt"
                }
                """.formatted(clientMessageId),
                tenantAAccessToken
        );
        assertThat(sendMessageResponse.statusCode()).isEqualTo(404);
        assertThat(countMessages(tenantBId, tenantBRoomId)).isZero();

        StompTestClient tenantBClient = stompClient();
        StompTestClient tenantASubscribeClient = stompClient();
        StompTestClient tenantASendClient = stompClient();
        StompSession tenantBSession = null;
        StompSession tenantASubscribeSession = null;
        StompSession tenantASendSession = null;

        try {
            tenantBSession = connect(tenantBClient.client(), tokenFor(tenantBUserId, tenantBId));
            tenantASubscribeSession = connect(tenantASubscribeClient.client(), tenantAAccessToken);
            tenantASendSession = connect(tenantASendClient.client(), tenantAAccessToken);

            BlockingQueue<Map<String, Object>> tenantBEvents = new LinkedBlockingQueue<>();
            String tenantBRoomEventsTopic = roomEventsTopic(tenantBId, tenantBRoomId);
            tenantBSession.subscribe(tenantBRoomEventsTopic, mapFrameHandler(tenantBEvents));
            awaitSubscriptionCount(tenantBRoomEventsTopic, 1);

            tenantASubscribeSession.subscribe(tenantBRoomEventsTopic, mapFrameHandler(new LinkedBlockingQueue<>()));
            assertSubscriptionCountRemains(tenantBRoomEventsTopic, 1);

            sendMessage(
                    tenantASendSession,
                    tenantBId,
                    tenantBRoomId,
                    UUID.randomUUID(),
                    "cross tenant stomp attempt"
            );

            assertNoRoomEvent(tenantBEvents);
            assertThat(countMessages(tenantBId, tenantBRoomId)).isZero();
        } finally {
            disconnect(tenantASendSession);
            disconnect(tenantASubscribeSession);
            disconnect(tenantBSession);
            tenantASendClient.close();
            tenantASubscribeClient.close();
            tenantBClient.close();
        }
    }

    @Test
    void stomp_non_member_subscribe_is_rejected() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();
        UUID memberAId = UUID.randomUUID();
        UUID memberBId = UUID.randomUUID();
        UUID nonMemberId = UUID.randomUUID();
        insertActiveRoomWithMembers(tenantId, chatRoomId, memberAId, memberBId);
        insertActiveTenantUser(tenantId, nonMemberId, "Non Member");

        StompTestClient nonMemberClient = stompClient();
        StompSession nonMemberSession = null;

        try {
            nonMemberSession = connect(nonMemberClient.client(), tokenFor(nonMemberId, tenantId));
            String roomEventsTopic = roomEventsTopic(tenantId, chatRoomId);

            nonMemberSession.subscribe(roomEventsTopic, mapFrameHandler(new LinkedBlockingQueue<>()));

            assertSubscriptionCountRemains(roomEventsTopic, 0);
        } finally {
            disconnect(nonMemberSession);
            nonMemberClient.close();
        }
    }

    @Test
    void stomp_member_subscribe_is_rejected_when_room_cannot_subscribe() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();
        UUID memberAId = UUID.randomUUID();
        UUID memberBId = UUID.randomUUID();
        insertActiveRoomWithMembers(tenantId, chatRoomId, memberAId, memberBId);
        updateRoomStatus(tenantId, chatRoomId, "PENDING_DELETION");

        StompTestClient memberClient = stompClient();
        StompSession memberSession = null;

        try {
            memberSession = connect(memberClient.client(), tokenFor(memberAId, tenantId));
            String roomEventsTopic = roomEventsTopic(tenantId, chatRoomId);

            memberSession.subscribe(roomEventsTopic, mapFrameHandler(new LinkedBlockingQueue<>()));

            assertSubscriptionCountRemains(roomEventsTopic, 0);
        } finally {
            disconnect(memberSession);
            memberClient.close();
        }
    }

    private void awaitSubscriptionCount(String destination, int expectedCount) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            if (subscriptionCount(destination) >= expectedCount) {
                return;
            }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(25));
        }

        assertThat(subscriptionCount(destination))
                .as("STOMP subscription count for %s", destination)
                .isGreaterThanOrEqualTo(expectedCount);
    }

    private long subscriptionCount(String destination) {
        return simpUserRegistry.findSubscriptions(subscription -> destination.equals(subscription.getDestination()))
                .size();
    }

    private void assertSubscriptionCountRemains(String destination, int expectedCount) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (System.nanoTime() < deadline) {
            assertThat(subscriptionCount(destination))
                    .as("STOMP subscription count for %s", destination)
                    .isLessThanOrEqualTo(expectedCount);
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(25));
        }

        assertThat(subscriptionCount(destination))
                .as("STOMP subscription count for %s", destination)
                .isEqualTo(expectedCount);
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

    private void sendMessage(
            StompSession session,
            UUID tenantId,
            UUID chatRoomId,
            UUID clientMessageId,
            String content
    ) {
        session.send(MESSAGE_SEND_DESTINATION, Map.of(
                "tenantId", tenantId.toString(),
                "chatRoomId", chatRoomId.toString(),
                "clientMessageId", clientMessageId.toString(),
                "content", content
        ));
    }

    private void editMessage(
            StompSession session,
            UUID tenantId,
            UUID chatRoomId,
            UUID messageId,
            String newContent
    ) {
        session.send(MESSAGE_EDIT_DESTINATION, Map.of(
                "tenantId", tenantId.toString(),
                "chatRoomId", chatRoomId.toString(),
                "messageId", messageId.toString(),
                "newContent", newContent
        ));
    }

    private void deleteMessage(
            StompSession session,
            UUID tenantId,
            UUID chatRoomId,
            UUID messageId
    ) {
        session.send(MESSAGE_DELETE_DESTINATION, Map.of(
                "tenantId", tenantId.toString(),
                "chatRoomId", chatRoomId.toString(),
                "messageId", messageId.toString()
        ));
    }

    private Map<String, Object> awaitRoomEvent(
            BlockingQueue<Map<String, Object>> events,
            String expectedType,
            UUID tenantId,
            UUID chatRoomId
    ) throws InterruptedException {
        Map<String, Object> event = events.poll(10, TimeUnit.SECONDS);

        assertThat(event).as("room event %s", expectedType).isNotNull();
        assertThat(event.get("type")).isEqualTo(expectedType);
        assertThat(event.get("tenantId")).isEqualTo(tenantId.toString());
        assertThat(event.get("chatRoomId")).isEqualTo(chatRoomId.toString());
        assertThat(((Number) event.get("sequence")).longValue()).isEqualTo(1L);

        return event;
    }

    private void assertNoRoomEvent(BlockingQueue<Map<String, Object>> events) throws InterruptedException {
        assertThat(events.poll(NO_EVENT_TIMEOUT_SECONDS, TimeUnit.SECONDS)).isNull();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> payload(Map<String, Object> event) {
        return (Map<String, Object>) event.get("payload");
    }

    private void assertMessageEditedPayload(
            Map<String, Object> payload,
            UUID messageId,
            UUID chatRoomId,
            String content
    ) {
        assertThat(payload.get("messageId")).isEqualTo(messageId.toString());
        assertThat(payload.get("chatRoomId")).isEqualTo(chatRoomId.toString());
        assertThat(((Number) payload.get("sequence")).longValue()).isEqualTo(1L);
        assertThat(payload.get("content")).isEqualTo(content);
        assertThat(payload.get("updatedAt")).isNotNull();
    }

    private void assertMessageDeletedPayload(
            Map<String, Object> payload,
            UUID messageId,
            UUID chatRoomId
    ) {
        assertThat(payload.get("messageId")).isEqualTo(messageId.toString());
        assertThat(payload.get("chatRoomId")).isEqualTo(chatRoomId.toString());
        assertThat(((Number) payload.get("sequence")).longValue()).isEqualTo(1L);
        assertThat(payload.get("deletedAt")).isNotNull();
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

    private HttpResponse<String> authenticatedGet(String path, String accessToken) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .GET()
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> authenticatedPost(String path, String body, String accessToken) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void insertActiveTenantWithUser(UUID tenantId, UUID userId, String name) throws Exception {
        OffsetDateTime now = OffsetDateTime.parse("2026-04-08T00:00:00Z");

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                insertActiveTenant(connection, tenantId, now);
                insertActiveUser(connection, tenantId, userId, name, now);
                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private void insertActiveTenantUser(UUID tenantId, UUID userId, String name) throws Exception {
        OffsetDateTime now = OffsetDateTime.parse("2026-04-08T00:00:00Z");

        try (Connection connection = dataSource.getConnection()) {
            insertActiveUser(connection, tenantId, userId, name, now);
        }
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

    private void updateRoomStatus(UUID tenantId, UUID chatRoomId, String status) throws Exception {
        OffsetDateTime now = OffsetDateTime.parse("2026-04-08T00:05:00Z");

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE chat_room
                     SET status = ?,
                         scheduled_deletion_at = CASE
                             WHEN ? = 'PENDING_DELETION' THEN ?
                             ELSE scheduled_deletion_at
                         END,
                         updated_at = ?
                     WHERE tenant_id = ?
                       AND id = ?
                     """)) {
            statement.setString(1, status);
            statement.setString(2, status);
            statement.setObject(3, now.plusDays(30));
            statement.setObject(4, now);
            statement.setObject(5, tenantId);
            statement.setObject(6, chatRoomId);

            assertThat(statement.executeUpdate()).isEqualTo(1);
        }
    }

    private MessageRow findMessage(UUID tenantId, UUID chatRoomId, UUID messageId) throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT content, status, deleted_at
                     FROM message
                     WHERE tenant_id = ?
                       AND chat_room_id = ?
                       AND id = ?
                     """)) {
            statement.setObject(1, tenantId);
            statement.setObject(2, chatRoomId);
            statement.setObject(3, messageId);

            try (ResultSet resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                return new MessageRow(
                        resultSet.getString("content"),
                        resultSet.getString("status"),
                        resultSet.getObject("deleted_at", OffsetDateTime.class)
                );
            }
        }
    }

    private long countMessages(UUID tenantId, UUID chatRoomId) throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT count(*)
                     FROM message
                     WHERE tenant_id = ?
                       AND chat_room_id = ?
                     """)) {
            statement.setObject(1, tenantId);
            statement.setObject(2, chatRoomId);

            try (ResultSet resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                return resultSet.getLong(1);
            }
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

    private record MessageRow(
            String content,
            String status,
            OffsetDateTime deletedAt
    ) {}

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
