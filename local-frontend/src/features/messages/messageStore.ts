import { create } from 'zustand'
import { toApiError, type ApiError } from '../../shared/api/apiError'
import {
  isMessageDeletedEvent,
  isMessageEditedEvent,
  isMessageSentEvent,
} from '../../shared/realtime/eventGuards'
import type { MessagePageResponse, UUID } from '../../shared/types/api'
import type { RoomEventEnvelope } from '../../shared/types/realtime'
import {
  applyMessageDeletedPayload,
  applyMessageEditedPayload,
  createOptimisticMessage,
  isOptimisticMessage,
  mapPageMessagesToSent,
  markMessageDeliveryFailed,
  mergeMessagesByClientMessageId,
  messageFromSentPayload,
  type MessageDeliveryStatus,
  type ReconciledMessage,
} from '../../shared/utils/messageReconciliation'
import { canLoadNextPage } from '../../shared/utils/pagination'
import { createUuid } from '../../shared/utils/uuid'
import {
  deleteMessage,
  editMessage,
  listMessages,
  markRead,
  sendMessage,
  sendReply,
} from './messagesApi'

export type { MessageDeliveryStatus }
export type MessageView = ReconciledMessage

type RoomMessageState = {
  messages: MessageView[]
  nextCursor: number | null
  hasNext: boolean
}

type MessageState = {
  rooms: Record<UUID, RoomMessageState>
  isLoading: boolean
  isSending: boolean
  error: ApiError | null
  loadMessages: (chatRoomId: UUID, reset?: boolean) => Promise<void>
  sendTextMessage: (chatRoomId: UUID, senderId: UUID, content: string) => Promise<void>
  replyTextMessage: (
    chatRoomId: UUID,
    senderId: UUID,
    replyToMessageId: UUID,
    content: string,
  ) => Promise<void>
  editTextMessage: (chatRoomId: UUID, messageId: UUID, newContent: string) => Promise<void>
  deleteTextMessage: (chatRoomId: UUID, messageId: UUID) => Promise<void>
  markRoomRead: (chatRoomId: UUID) => Promise<void>
  applyRoomEvent: (event: RoomEventEnvelope) => void
}

const emptyRoomState = (): RoomMessageState => ({
  messages: [],
  nextCursor: null,
  hasNext: false,
})

const messagePageToState = (
  previous: RoomMessageState | undefined,
  page: MessagePageResponse,
  reset: boolean,
) => {
  const pageMessages = mapPageMessagesToSent(page.messages)
  const previousMessages = previous?.messages ?? []
  const maxPageSequence = pageMessages.reduce(
    (max, message) => Math.max(max, message.sequence),
    0,
  )
  const baseMessages = reset
    ? previousMessages.filter(
        (message) =>
          isOptimisticMessage(message) ||
          maxPageSequence === 0 ||
          message.sequence > maxPageSequence,
      )
    : previousMessages

  return {
    messages: mergeMessagesByClientMessageId(baseMessages, pageMessages),
    nextCursor: page.nextCursor,
    hasNext: page.hasNext,
  }
}

const updateRoomMessages = (
  rooms: Record<UUID, RoomMessageState>,
  chatRoomId: UUID,
  update: (roomState: RoomMessageState) => MessageView[],
) => {
  const roomState = rooms[chatRoomId] ?? emptyRoomState()

  return {
    ...rooms,
    [chatRoomId]: {
      ...roomState,
      messages: update(roomState),
    },
  }
}

export const useMessageStore = create<MessageState>((set, get) => ({
  rooms: {},
  isLoading: false,
  isSending: false,
  error: null,

  loadMessages: async (chatRoomId, reset = true) => {
    const previous = get().rooms[chatRoomId]
    if (!reset && !canLoadNextPage(previous)) {
      return
    }

    set({ isLoading: true, error: null })

    try {
      const page = await listMessages(chatRoomId, reset ? null : previous?.nextCursor)
      set((state) => ({
        isLoading: false,
        rooms: {
          ...state.rooms,
          [chatRoomId]: messagePageToState(state.rooms[chatRoomId], page, reset),
        },
      }))
    } catch (error) {
      set({ isLoading: false, error: toApiError(error) })
    }
  },

  sendTextMessage: async (chatRoomId, senderId, content) => {
    const clientMessageId = createUuid()
    const optimisticMessage = createOptimisticMessage({
      chatRoomId,
      senderId,
      clientMessageId,
      content,
    })

    set((state) => ({
      isSending: true,
      error: null,
      rooms: updateRoomMessages(state.rooms, chatRoomId, (roomState) =>
        mergeMessagesByClientMessageId(roomState.messages, [optimisticMessage]),
      ),
    }))

    try {
      const response = await sendMessage(chatRoomId, { clientMessageId, content })
      const confirmed: MessageView = {
        ...optimisticMessage,
        messageId: response.messageId,
        sequence: response.sequence,
        createdAt: response.createdAt,
        deliveryStatus: 'sent' as const,
      }

      set((state) => ({
        isSending: false,
        rooms: updateRoomMessages(state.rooms, chatRoomId, (roomState) =>
          mergeMessagesByClientMessageId(roomState.messages, [confirmed]),
        ),
      }))
    } catch (error) {
      set((state) => ({
        isSending: false,
        error: toApiError(error),
        rooms: updateRoomMessages(state.rooms, chatRoomId, (roomState) =>
          markMessageDeliveryFailed(roomState.messages, clientMessageId),
        ),
      }))
    }
  },

  replyTextMessage: async (chatRoomId, senderId, replyToMessageId, content) => {
    const clientMessageId = createUuid()
    const optimisticMessage = createOptimisticMessage({
      chatRoomId,
      senderId,
      clientMessageId,
      content,
      replyToMessageId,
    })

    set((state) => ({
      isSending: true,
      error: null,
      rooms: updateRoomMessages(state.rooms, chatRoomId, (roomState) =>
        mergeMessagesByClientMessageId(roomState.messages, [optimisticMessage]),
      ),
    }))

    try {
      const response = await sendReply(chatRoomId, replyToMessageId, {
        clientMessageId,
        content,
      })
      const confirmed: MessageView = {
        ...optimisticMessage,
        messageId: response.messageId,
        sequence: response.sequence,
        createdAt: response.createdAt,
        deliveryStatus: 'sent' as const,
      }

      set((state) => ({
        isSending: false,
        rooms: updateRoomMessages(state.rooms, chatRoomId, (roomState) =>
          mergeMessagesByClientMessageId(roomState.messages, [confirmed]),
        ),
      }))
    } catch (error) {
      set((state) => ({
        isSending: false,
        error: toApiError(error),
        rooms: updateRoomMessages(state.rooms, chatRoomId, (roomState) =>
          markMessageDeliveryFailed(roomState.messages, clientMessageId),
        ),
      }))
    }
  },

  editTextMessage: async (chatRoomId, messageId, newContent) => {
    set({ error: null })

    try {
      const response = await editMessage(chatRoomId, messageId, { newContent })
      set((state) => ({
        rooms: updateRoomMessages(state.rooms, chatRoomId, (roomState) =>
          roomState.messages.map((message) =>
            message.messageId === response.messageId
              ? {
                  ...message,
                  content: response.content,
                  sequence: response.sequence,
                  status: 'EDITED' as const,
                  updatedAt: response.updatedAt,
                }
              : message,
          ),
        ),
      }))
    } catch (error) {
      set({ error: toApiError(error) })
    }
  },

  deleteTextMessage: async (chatRoomId, messageId) => {
    set({ error: null })

    try {
      const response = await deleteMessage(chatRoomId, messageId)
      set((state) => ({
        rooms: updateRoomMessages(state.rooms, chatRoomId, (roomState) =>
          roomState.messages.map((message) =>
            message.messageId === response.messageId
              ? {
                  ...message,
                  sequence: response.sequence,
                  status: 'DELETED' as const,
                  deletedAt: response.deletedAt,
                }
              : message,
          ),
        ),
      }))
    } catch (error) {
      set({ error: toApiError(error) })
    }
  },

  markRoomRead: async (chatRoomId) => {
    const lastMessage = [...(get().rooms[chatRoomId]?.messages ?? [])]
      .filter((message) => message.sequence > 0)
      .at(-1)

    if (!lastMessage) {
      return
    }

    try {
      await markRead(chatRoomId, { lastReadMessageId: lastMessage.messageId })
    } catch (error) {
      set({ error: toApiError(error) })
    }
  },

  applyRoomEvent: (event) => {
    if (isMessageSentEvent(event)) {
      const message = messageFromSentPayload(event.payload)
      set((state) => ({
        rooms: updateRoomMessages(state.rooms, event.chatRoomId, (roomState) =>
          mergeMessagesByClientMessageId(roomState.messages, [message]),
        ),
      }))
      return
    }

    if (isMessageEditedEvent(event)) {
      set((state) => ({
        rooms: updateRoomMessages(state.rooms, event.chatRoomId, (roomState) =>
          applyMessageEditedPayload(roomState.messages, event.payload),
        ),
      }))
      return
    }

    if (isMessageDeletedEvent(event)) {
      set((state) => ({
        rooms: updateRoomMessages(state.rooms, event.chatRoomId, (roomState) =>
          applyMessageDeletedPayload(roomState.messages, event.payload),
        ),
      }))
    }
  },
}))
