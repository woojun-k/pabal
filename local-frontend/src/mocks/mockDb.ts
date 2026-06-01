import type {
  CreateChannelRoomRequest,
  CreateGroupRoomRequest,
  DeleteMessageResponse,
  EditMessageResponse,
  GetOrCreateDirectRoomResponse,
  Instant,
  MessagePageResponse,
  MessageResponse,
  RoomResponse,
  SendMessageResponse,
  SendMessageRequest,
  UUID,
} from '../shared/types/api'
import type {
  MessageDeletedRealtimePayload,
  MessageEditedRealtimePayload,
  MessageReadRealtimePayload,
  MessageSentRealtimePayload,
  RoomEventEnvelope,
  RoomEventPayload,
  RoomEventType,
} from '../shared/types/realtime'
import type { MockPrincipal } from './mockAuth'

type MockRoom = Omit<RoomResponse, 'joinedAt' | 'unreadCount'> & {
  tenantId: UUID
  memberIds: UUID[]
  joinedAtByUser: Record<UUID, Instant>
  unreadByUser: Record<UUID, number>
}

type SendMessageResult = {
  response: SendMessageResponse
  event: RoomEventEnvelope<MessageSentRealtimePayload> | null
}

type EditMessageResult = {
  response: EditMessageResponse
  event: RoomEventEnvelope<MessageEditedRealtimePayload>
}

type DeleteMessageResult = {
  response: DeleteMessageResponse
  event: RoomEventEnvelope<MessageDeletedRealtimePayload>
}

type MarkReadResult = {
  event: RoomEventEnvelope<MessageReadRealtimePayload> | null
}

const rooms = new Map<UUID, MockRoom>()
const messagesByRoomId = new Map<UUID, MessageResponse[]>()
const seededPrincipals = new Set<string>()

let idCounter = 100
let eventCounter = 1

const now = () => new Date().toISOString()

const stableUuid = (value: number): UUID =>
  `00000000-0000-4000-8000-${value.toString().padStart(12, '0')}`

const nextUuid = (): UUID => stableUuid(idCounter++)

const principalKey = (principal: MockPrincipal) => `${principal.tenantId}:${principal.userId}`

const sequenceForRoom = (chatRoomId: UUID) => (messagesByRoomId.get(chatRoomId)?.length ?? 0) + 1

const roomMessages = (chatRoomId: UUID) => messagesByRoomId.get(chatRoomId) ?? []

const addMessage = (
  principal: MockPrincipal,
  chatRoomId: UUID,
  params: SendMessageRequest & {
    replyToMessageId?: UUID | null
  },
) => {
  const room = rooms.get(chatRoomId)

  if (!room || room.tenantId !== principal.tenantId || !room.memberIds.includes(principal.userId)) {
    return null
  }

  const existing = roomMessages(chatRoomId).find(
    (message) =>
      message.senderId === principal.userId &&
      message.clientMessageId === params.clientMessageId,
  )

  if (existing) {
    return {
      response: {
        messageId: existing.messageId,
        sequence: existing.sequence,
        clientMessageId: existing.clientMessageId,
        createdAt: existing.createdAt,
        duplicated: true,
      },
      event: null,
    } satisfies SendMessageResult
  }

  const createdAt = now()
  const message: MessageResponse = {
    messageId: nextUuid(),
    chatRoomId,
    senderId: principal.userId,
    clientMessageId: params.clientMessageId,
    sequence: sequenceForRoom(chatRoomId),
    content: params.content,
    status: 'ACTIVE',
    replyToMessageId: params.replyToMessageId ?? null,
    createdAt,
    updatedAt: null,
    deletedAt: null,
  }

  const messages = roomMessages(chatRoomId)
  messagesByRoomId.set(chatRoomId, [...messages, message])
  room.lastMessageId = message.messageId
  room.lastMessageAt = message.createdAt

  for (const memberId of room.memberIds) {
    if (memberId !== principal.userId) {
      room.unreadByUser[memberId] = (room.unreadByUser[memberId] ?? 0) + 1
    }
  }

  return {
    response: {
      messageId: message.messageId,
      sequence: message.sequence,
      clientMessageId: message.clientMessageId,
      createdAt: message.createdAt,
      duplicated: false,
    },
    event: roomEvent('MESSAGE_SENT', principal.tenantId, chatRoomId, message.sequence, {
      messageId: message.messageId,
      chatRoomId,
      sequence: message.sequence,
      senderId: message.senderId,
      clientMessageId: message.clientMessageId,
      content: message.content,
      createdAt: message.createdAt,
    }),
  } satisfies SendMessageResult
}

const roomEvent = <TPayload extends RoomEventPayload>(
  type: RoomEventType,
  tenantId: UUID,
  chatRoomId: UUID,
  sequence: number,
  payload: TPayload,
): RoomEventEnvelope<TPayload> => ({
  eventId: stableUuid(900_000 + eventCounter++),
  schemaVersion: 1,
  type,
  tenantId,
  chatRoomId,
  sequence,
  aggregateVersion: sequence,
  occurredAt: now(),
  payload,
})

const createRoom = (params: {
  tenantId: UUID
  memberIds: UUID[]
  name: string
  type: MockRoom['type']
  joinedAt?: Instant
}) => {
  const joinedAt = params.joinedAt ?? now()
  const room: MockRoom = {
    roomId: nextUuid(),
    tenantId: params.tenantId,
    name: params.name,
    type: params.type,
    status: 'ACTIVE',
    lastMessageId: null,
    lastMessageAt: null,
    memberIds: [...new Set(params.memberIds)],
    joinedAtByUser: Object.fromEntries(
      [...new Set(params.memberIds)].map((memberId) => [memberId, joinedAt]),
    ),
    unreadByUser: Object.fromEntries(
      [...new Set(params.memberIds)].map((memberId) => [memberId, 0]),
    ),
  }

  rooms.set(room.roomId, room)
  messagesByRoomId.set(room.roomId, [])

  return room
}

const appendSeedMessage = (
  principal: MockPrincipal,
  chatRoomId: UUID,
  senderId: UUID,
  content: string,
  createdAt: Instant,
) => {
  const room = rooms.get(chatRoomId)

  if (!room) {
    return
  }

  const message: MessageResponse = {
    messageId: nextUuid(),
    chatRoomId,
    senderId,
    clientMessageId: nextUuid(),
    sequence: sequenceForRoom(chatRoomId),
    content,
    status: 'ACTIVE',
    replyToMessageId: null,
    createdAt,
    updatedAt: null,
    deletedAt: null,
  }

  messagesByRoomId.set(chatRoomId, [...roomMessages(chatRoomId), message])
  room.lastMessageId = message.messageId
  room.lastMessageAt = message.createdAt

  if (senderId !== principal.userId) {
    room.unreadByUser[principal.userId] = (room.unreadByUser[principal.userId] ?? 0) + 1
  }
}

const ensureSeeded = (principal: MockPrincipal) => {
  const key = principalKey(principal)

  if (seededPrincipals.has(key)) {
    return
  }

  seededPrincipals.add(key)

  const adaId = stableUuid(101)
  const linusId = stableUuid(102)
  const workspaceBotId = stableUuid(103)
  const baseTime = Date.now() - 1000 * 60 * 45
  const at = (minutes: number) => new Date(baseTime + minutes * 60_000).toISOString()

  const direct = createRoom({
    tenantId: principal.tenantId,
    memberIds: [principal.userId, adaId],
    name: '',
    type: 'DIRECT',
    joinedAt: at(1),
  })
  appendSeedMessage(principal, direct.roomId, adaId, 'Mock direct room is ready.', at(2))
  appendSeedMessage(principal, direct.roomId, principal.userId, 'I can keep building without backend data.', at(3))
  appendSeedMessage(principal, direct.roomId, adaId, 'Realtime mock messages will merge into this room.', at(4))

  const group = createRoom({
    tenantId: principal.tenantId,
    memberIds: [principal.userId, adaId, linusId],
    name: 'Frontend mock group',
    type: 'GROUP',
    joinedAt: at(5),
  })
  appendSeedMessage(principal, group.roomId, principal.userId, 'Group room fixture loaded.', at(6))
  appendSeedMessage(principal, group.roomId, linusId, 'Pagination can be tested here.', at(7))

  const channel = createRoom({
    tenantId: principal.tenantId,
    memberIds: [principal.userId, adaId, linusId, workspaceBotId],
    name: 'frontend-updates',
    type: 'CHANNEL',
    joinedAt: at(8),
  })
  appendSeedMessage(principal, channel.roomId, workspaceBotId, 'Channel fixture is available.', at(9))
  appendSeedMessage(principal, channel.roomId, adaId, 'Unread badges are backed by mock state.', at(10))
}

const toRoomResponse = (room: MockRoom, principal: MockPrincipal): RoomResponse => ({
  roomId: room.roomId,
  name: room.name,
  type: room.type,
  status: room.status,
  lastMessageId: room.lastMessageId,
  lastMessageAt: room.lastMessageAt,
  unreadCount: room.unreadByUser[principal.userId] ?? 0,
  joinedAt: room.joinedAtByUser[principal.userId] ?? now(),
})

export const mockDb = {
  createChannelRoom(principal: MockPrincipal, request: CreateChannelRoomRequest) {
    ensureSeeded(principal)
    const room = createRoom({
      tenantId: principal.tenantId,
      memberIds: [principal.userId, ...(request.participantIds ?? [])],
      name: request.channelName,
      type: 'CHANNEL',
    })

    return {
      chatRoomId: room.roomId,
      roomName: room.name,
    }
  },

  createGroupRoom(principal: MockPrincipal, request: CreateGroupRoomRequest) {
    ensureSeeded(principal)
    const room = createRoom({
      tenantId: principal.tenantId,
      memberIds: [principal.userId, ...request.participantIds],
      name: request.roomName ?? 'Mock group',
      type: 'GROUP',
    })

    return {
      chatRoomId: room.roomId,
      roomName: room.name,
    }
  },

  deleteMessage(
    principal: MockPrincipal,
    chatRoomId: UUID,
    messageId: UUID,
  ): DeleteMessageResult | null {
    ensureSeeded(principal)
    const room = rooms.get(chatRoomId)
    const messages = roomMessages(chatRoomId)
    const message = messages.find((candidate) => candidate.messageId === messageId)

    if (!room || !message) {
      return null
    }

    const deletedAt = now()
    message.status = 'DELETED'
    message.deletedAt = deletedAt

    return {
      response: {
        messageId,
        sequence: message.sequence,
        deletedAt,
      },
      event: roomEvent('MESSAGE_DELETED', principal.tenantId, chatRoomId, message.sequence, {
        messageId,
        chatRoomId,
        sequence: message.sequence,
        deletedAt,
      }),
    }
  },

  editMessage(
    principal: MockPrincipal,
    chatRoomId: UUID,
    messageId: UUID,
    newContent: string,
  ): EditMessageResult | null {
    ensureSeeded(principal)
    const message = roomMessages(chatRoomId).find((candidate) => candidate.messageId === messageId)

    if (!message) {
      return null
    }

    const updatedAt = now()
    message.content = newContent
    message.status = 'EDITED'
    message.updatedAt = updatedAt

    return {
      response: {
        messageId,
        sequence: message.sequence,
        content: newContent,
        updatedAt,
      },
      event: roomEvent('MESSAGE_EDITED', principal.tenantId, chatRoomId, message.sequence, {
        messageId,
        chatRoomId,
        sequence: message.sequence,
        content: newContent,
        updatedAt,
      }),
    }
  },

  getMessage(principal: MockPrincipal, chatRoomId: UUID, messageId: UUID) {
    ensureSeeded(principal)
    return roomMessages(chatRoomId).find((message) => message.messageId === messageId) ?? null
  },

  getOrCreateDirectRoom(
    principal: MockPrincipal,
    participantId: UUID,
  ): GetOrCreateDirectRoomResponse {
    ensureSeeded(principal)
    const existing = [...rooms.values()].find(
      (room) =>
        room.tenantId === principal.tenantId &&
        room.type === 'DIRECT' &&
        room.memberIds.includes(principal.userId) &&
        room.memberIds.includes(participantId),
    )

    if (existing) {
      return { chatRoomId: existing.roomId }
    }

    const room = createRoom({
      tenantId: principal.tenantId,
      memberIds: [principal.userId, participantId],
      name: '',
      type: 'DIRECT',
    })

    return { chatRoomId: room.roomId }
  },

  joinRoom(principal: MockPrincipal, chatRoomId: UUID) {
    ensureSeeded(principal)
    const room = rooms.get(chatRoomId)

    if (!room) {
      return false
    }

    if (!room.memberIds.includes(principal.userId)) {
      room.memberIds.push(principal.userId)
      room.joinedAtByUser[principal.userId] = now()
      room.unreadByUser[principal.userId] = 0
    }

    return true
  },

  leaveRoom(principal: MockPrincipal, chatRoomId: UUID) {
    ensureSeeded(principal)
    const room = rooms.get(chatRoomId)

    if (!room) {
      return false
    }

    room.memberIds = room.memberIds.filter((memberId) => memberId !== principal.userId)
    return true
  },

  listMessages(principal: MockPrincipal, chatRoomId: UUID, cursor: number | null, size: number) {
    ensureSeeded(principal)
    const messages = roomMessages(chatRoomId)
      .filter((message) => cursor === null || message.sequence < cursor)
      .sort((a, b) => b.sequence - a.sequence)
    const page = messages.slice(0, size)
    const response: MessagePageResponse = {
      messages: [...page].reverse(),
      nextCursor: page.at(-1)?.sequence ?? null,
      hasNext: messages.length > size,
    }

    return response
  },

  listRooms(principal: MockPrincipal) {
    ensureSeeded(principal)
    return [...rooms.values()]
      .filter(
        (room) =>
          room.tenantId === principal.tenantId &&
          room.memberIds.includes(principal.userId) &&
          room.status === 'ACTIVE',
      )
      .map((room) => toRoomResponse(room, principal))
      .sort((a, b) => {
        const aTime = a.lastMessageAt ?? a.joinedAt
        const bTime = b.lastMessageAt ?? b.joinedAt
        return bTime.localeCompare(aTime)
      })
  },

  markRead(
    principal: MockPrincipal,
    chatRoomId: UUID,
    lastReadMessageId: UUID,
  ): MarkReadResult | null {
    ensureSeeded(principal)
    const room = rooms.get(chatRoomId)
    const message = roomMessages(chatRoomId).find(
      (candidate) => candidate.messageId === lastReadMessageId,
    )

    if (!room || !message) {
      return null
    }

    room.unreadByUser[principal.userId] = 0

    return {
      event: roomEvent('MESSAGE_READ', principal.tenantId, chatRoomId, message.sequence, {
        userId: principal.userId,
        chatRoomId,
        lastReadMessageId,
        sequence: message.sequence,
        readAt: now(),
      }),
    }
  },

  scheduleRoomDeletion(principal: MockPrincipal, chatRoomId: UUID) {
    ensureSeeded(principal)
    const room = rooms.get(chatRoomId)

    if (!room) {
      return false
    }

    room.status = 'PENDING_DELETION'
    return true
  },

  sendMessage(principal: MockPrincipal, chatRoomId: UUID, request: SendMessageRequest) {
    ensureSeeded(principal)
    return addMessage(principal, chatRoomId, request)
  },

  sendReply(
    principal: MockPrincipal,
    chatRoomId: UUID,
    replyToMessageId: UUID,
    request: SendMessageRequest,
  ) {
    ensureSeeded(principal)
    return addMessage(principal, chatRoomId, {
      ...request,
      replyToMessageId,
    })
  },

  unreadCount(principal: MockPrincipal, chatRoomId: UUID) {
    ensureSeeded(principal)
    const room = rooms.get(chatRoomId)
    return room ? room.unreadByUser[principal.userId] ?? 0 : null
  },
}
