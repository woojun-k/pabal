import { useEffect, useMemo, useRef } from 'react'
import { useAuthStore } from '../auth/authStore'
import { useMessageStore } from '../messages/messageStore'
import { useNotificationStore } from '../notifications/notificationStore'
import { useRoomStore } from '../rooms/roomStore'
import { useRealtimeStore } from './realtimeStore'
import { isMessageSentEvent } from '../../shared/realtime/eventGuards'

export function RealtimeBridge() {
  const accessToken = useAuthStore((state) => state.accessToken)
  const userId = useAuthStore((state) => state.userId)
  const tenantId = useAuthStore((state) => state.tenantId)
  const activeRoomId = useRoomStore((state) => state.activeRoomId)
  const roomIdsKey = useRoomStore((state) =>
    state.rooms.map((room) => room.roomId).sort().join('|'),
  )
  const connect = useRealtimeStore((state) => state.connect)
  const disconnect = useRealtimeStore((state) => state.disconnect)
  const clearSubscriptions = useRealtimeStore((state) => state.clearSubscriptions)
  const subscribeRoomEvents = useRealtimeStore((state) => state.subscribeRoomEvents)
  const subscribeTyping = useRealtimeStore((state) => state.subscribeTyping)
  const subscribeUserControl = useRealtimeStore((state) => state.subscribeUserControl)
  const applyMessageEvent = useMessageStore((state) => state.applyRoomEvent)
  const applyRoomEvent = useRoomStore((state) => state.applyRoomEvent)
  const loadRooms = useRoomStore((state) => state.loadRooms)
  const addNotification = useNotificationStore((state) => state.addNotification)
  const roomIds = useMemo(() => (roomIdsKey ? roomIdsKey.split('|') : []), [roomIdsKey])
  const activeRoomIdRef = useRef(activeRoomId)

  useEffect(() => {
    activeRoomIdRef.current = activeRoomId
  }, [activeRoomId])

  useEffect(() => {
    if (!accessToken) {
      clearSubscriptions()
      void disconnect()
      return
    }

    connect()
    void loadRooms()
  }, [accessToken, clearSubscriptions, connect, disconnect, loadRooms])

  useEffect(() => {
    if (!accessToken) {
      return
    }

    const subscription = subscribeUserControl((payload) => {
      addNotification({
        kind: 'warning',
        title: 'Room subscription revoked',
        message: payload.chatRoomId,
      })
      void loadRooms()
    })

    return () => subscription.unsubscribe()
  }, [accessToken, addNotification, loadRooms, subscribeUserControl])

  useEffect(() => {
    if (!accessToken || !tenantId || roomIds.length === 0) {
      return
    }

    const subscriptions = roomIds.map((roomId) =>
      subscribeRoomEvents(tenantId, roomId, (event) => {
        applyMessageEvent(event)
        applyRoomEvent(event, userId)

        if (isMessageSentEvent(event) && event.payload.senderId !== userId) {
          addNotification({
            kind: 'info',
            title:
              event.chatRoomId === activeRoomIdRef.current ? 'New message' : 'New room message',
            message: event.chatRoomId,
          })
        }
      }),
    )

    return () => {
      subscriptions.forEach((subscription) => subscription.unsubscribe())
    }
  }, [
    accessToken,
    addNotification,
    applyMessageEvent,
    applyRoomEvent,
    roomIds,
    subscribeRoomEvents,
    tenantId,
    userId,
  ])

  useEffect(() => {
    if (!accessToken || !tenantId || !activeRoomId) {
      return
    }

    const subscription = subscribeTyping(tenantId, activeRoomId, (payload) => {
      if (payload.userId === userId) {
        return
      }

      addNotification({
        kind: 'info',
        title: payload.status === 'STARTED' ? 'Typing started' : 'Typing stopped',
        message: payload.userId,
      })
    })

    return () => {
      subscription.unsubscribe()
    }
  }, [
    activeRoomId,
    accessToken,
    addNotification,
    subscribeTyping,
    tenantId,
    userId,
  ])

  return null
}
