import { create } from 'zustand'
import { getAccessToken } from '../../shared/security/session'
import {
  realtimeClient,
  type RealtimeClientError,
  type RealtimeConnectionStatus,
  type SubscriptionHandle,
} from '../../shared/realtime/realtimeClient'
import type { UUID } from '../../shared/types/api'
import type {
  RoomEventEnvelope,
  RoomSubscriptionRevokedRealtimePayload,
  TypingEventPayload,
  TypingRequest,
} from '../../shared/types/realtime'

type RealtimeState = {
  status: RealtimeConnectionStatus
  error: RealtimeClientError | null
  subscriptionIds: string[]
  connect: () => void
  disconnect: () => Promise<void>
  clearSubscriptions: () => void
  unsubscribe: (subscriptionId: string) => void
  subscribeRoomEvents: (
    tenantId: UUID,
    chatRoomId: UUID,
    handler: (payload: RoomEventEnvelope) => void,
  ) => SubscriptionHandle
  subscribeTyping: (
    tenantId: UUID,
    chatRoomId: UUID,
    handler: (payload: TypingEventPayload) => void,
  ) => SubscriptionHandle
  subscribeUserControl: (
    handler: (payload: RoomSubscriptionRevokedRealtimePayload) => void,
  ) => SubscriptionHandle
  sendTypingStart: (request: TypingRequest) => void
  sendTypingStop: (request: TypingRequest) => void
}

const setSubscriptionIds = () => {
  useRealtimeStore.setState({
    subscriptionIds: realtimeClient.getSubscriptionIds(),
  })
}

export const useRealtimeStore = create<RealtimeState>((set) => ({
  status: realtimeClient.getStatus(),
  error: null,
  subscriptionIds: realtimeClient.getSubscriptionIds(),

  connect: () => {
    const accessToken = getAccessToken()

    if (!accessToken) {
      set({
        status: 'error',
        error: {
          message: 'Missing local access token',
        },
      })
      return
    }

    set({ error: null })
    realtimeClient.connect({ accessToken })
  },

  disconnect: async () => {
    set({ error: null })
    await realtimeClient.disconnect()
  },

  clearSubscriptions: () => {
    realtimeClient.clearSubscriptions()
    setSubscriptionIds()
  },

  unsubscribe: (subscriptionId) => {
    realtimeClient.unsubscribe(subscriptionId)
    setSubscriptionIds()
  },

  subscribeRoomEvents: (tenantId, chatRoomId, handler) => {
    const subscription = realtimeClient.subscribeRoomEvents(tenantId, chatRoomId, (payload) =>
      handler(payload),
    )
    setSubscriptionIds()
    return subscription
  },

  subscribeTyping: (tenantId, chatRoomId, handler) => {
    const subscription = realtimeClient.subscribeTyping(tenantId, chatRoomId, (payload) =>
      handler(payload),
    )
    setSubscriptionIds()
    return subscription
  },

  subscribeUserControl: (handler) => {
    const subscription = realtimeClient.subscribeUserControl((payload) => handler(payload))
    setSubscriptionIds()
    return subscription
  },

  sendTypingStart: (request) => {
    realtimeClient.sendTypingStart(request)
  },

  sendTypingStop: (request) => {
    realtimeClient.sendTypingStop(request)
  },
}))

realtimeClient.onStatusChange((status) => {
  useRealtimeStore.setState((state) => ({
    status,
    error: status === 'connecting' || status === 'connected' ? null : state.error,
  }))
})

realtimeClient.onError((error) => {
  useRealtimeStore.setState({
    status: 'error',
    error,
  })
})
