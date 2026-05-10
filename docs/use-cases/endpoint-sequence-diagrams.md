---
tags:
  - pabal
  - sequence
  - endpoint
---

# Pabal 엔드포인트 시퀀스 다이어그램

> 상위 문서: [Pabal 상세 설계 허브](../design/design-hub.md)
> 관련 문서: [Pabal Command-Query 유스케이스 카탈로그](command-query-catalog.md), [Pabal HTTP API 예시와 오류 매핑](http-api-and-error-mapping.md), [Pabal 런타임 흐름](../architecture/runtime-flow.md), [Pabal Realtime 이벤트 스키마](../realtime/event-schema.md)

## SendMessage

Layer: API → Application → Domain → Application Port → Infrastructure Adapter

```mermaid
sequenceDiagram
    participant C as Client
    participant API as ChatCommandController
    participant M as ChatCommandMapper
    participant H as SendMessageCommandHandler
    participant A as ChatRoomAccessSupport
    participant S as MessageSendSupportAdapter
    participant R as MessageRepository
    participant Q as ChatRoomSequenceRepository
    participant E as SpringDomainEventPublisher
    participant L as MessageSentEventListener
    participant WS as StompChatRealtimeAdapter

    C->>API: POST /api/v1/chat-rooms/{id}/messages
    API->>M: toSendMessageCommand(authentication)
    M-->>API: SendMessageCommand(tenantId, senderId, chatRoomId, clientMessageId, content)
    API->>H: handle(command)
    H->>A: loadSendableActiveMember
    A-->>H: ChatRoomAccess
    H->>S: findDuplicate(command)
    S-->>H: empty or PersistedMessage
    H->>S: send(room, Message.create(...))
    S->>Q: allocateNextMessageSequence
    S->>R: append(PersistedMessage)
    S->>Q: updateLastMessageSnapshot
    S->>E: publishAfterCommit(MessageSentEvent)
    H-->>API: SendMessageResult
    API-->>C: SendMessageResponse
    E-->>L: after commit
    L->>WS: publishRoomEvent(MESSAGE_SENT)
```

## GetOrCreateDirectRoom

Layer: API → Application → Domain → Application Port → Infrastructure Adapter

```mermaid
sequenceDiagram
    participant C as Client
    participant API as ChatCommandController
    participant H as GetOrCreateDirectRoomCommandHandler
    participant R as DirectChatMappingRepository
    participant S as DirectRoomCreationService
    participant CR as ChatRoomRepository
    participant MR as ChatRoomMemberRepository

    C->>API: POST /api/v1/chat-rooms/direct
    API->>H: handle(GetOrCreateDirectRoomCommand)
    H->>R: findByTenantIdAndUserIds
    alt mapping exists
        R-->>H: PersistedDirectChatMapping
        H-->>API: existing chatRoomId
    else mapping missing
        H->>S: create(command)
        S->>CR: append(ChatRoom.createDirect)
        S->>MR: append(member requester)
        S->>MR: append(member participant)
        S->>R: append(DirectChatMapping)
        S->>R: flush()
        S-->>H: new chatRoomId
    end
    API-->>C: GetOrCreateDirectRoomResponse
```

## ListMessages

Layer: API → Application → Application Port → Infrastructure Adapter

```mermaid
sequenceDiagram
    participant C as Client
    participant API as ChatQueryController
    participant M as ChatQueryMapper
    participant H as ListMessagesHandler
    participant A as ChatRoomReadAccessSupport
    participant R as MessageReadRepository
    participant QM as MessageQueryMapper

    C->>API: GET /api/v1/chat-rooms/{id}/messages?cursor=&size=50
    API->>M: toListMessagesQuery(authentication)
    API->>H: handle(query)
    H->>A: loadReadableActiveMember
    H->>R: findByTenantIdAndChatRoomIdBeforeSequence(limit=size+1)
    R-->>H: PersistedMessage desc list
    H->>QM: toMessageDtosOldestFirst
    H-->>API: MessagePageDto
    API-->>C: MessagePageResponse
```

## MarkRead

Layer: API → Application → Domain → Application Port → Infrastructure Adapter

```mermaid
sequenceDiagram
    participant C as Client
    participant API as ChatCommandController
    participant H as MarkReadCommandHandler
    participant A as ChatRoomAccessSupport
    participant MR as MessageRepository
    participant MBR as ChatRoomMemberRepository
    participant E as DomainEventPublisher

    C->>API: PUT /api/v1/chat-rooms/{id}/read-state
    API->>H: handle(MarkReadCommand)
    H->>A: loadReadableActiveMember
    H->>MR: findByTenantIdAndChatRoomIdAndId(lastReadMessageId)
    H->>H: compare lastReadSequence
    alt cursor updated
        H->>MBR: update(member)
        H->>E: publishAfterCommit(MessageReadEvent)
    else stale cursor
        H-->>API: no-op
    end
    API-->>C: 204 No Content
```

## STOMP CONNECT + SUBSCRIBE

Layer: Infrastructure Security → Security → Infrastructure Authorization

```mermaid
sequenceDiagram
    participant C as STOMP Client
    participant I as StompConnectAuthenticationInterceptor
    participant AM as WebSocketAuthenticationManagerConfig
    participant J as JwtDecoder
    participant CV as PabalJwtAuthenticationConverter
    participant AZ as RoomSubscriptionAuthorizationManager
    participant RR as ChatRoomReadRepository
    participant MR as ChatRoomMemberRepository

    C->>I: CONNECT Authorization/access_token
    I->>AM: authenticate(BearerTokenAuthenticationToken)
    AM->>J: decode/validate JWT
    AM->>CV: convert JWT to PabalJwtAuthenticationToken
    I-->>C: authenticated STOMP session
    C->>AZ: SUBSCRIBE /topic/tenants/{tenantId}/chat-rooms/{roomId}/events
    AZ->>AZ: compare destination tenant with principal tenant
    AZ->>RR: findByTenantIdAndId
    AZ->>MR: findByTenantIdAndChatRoomIdAndUserId
    AZ-->>C: grant or deny
```
