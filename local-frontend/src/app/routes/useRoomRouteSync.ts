import { useEffect } from 'react'
import { useNavigate, useParams } from 'react-router'
import { useNotificationStore } from '../../features/notifications/notificationStore'
import { useRoomStore } from '../../features/rooms/roomStore'
import { roomsPath } from '../paths'

export function useRoomRouteSync() {
  const params = useParams()
  const navigate = useNavigate()
  const clearNotificationsForRoom = useNotificationStore(
    (state) => state.clearNotificationsForRoom,
  )
  const rooms = useRoomStore((state) => state.rooms)
  const loadStatus = useRoomStore((state) => state.loadStatus)
  const selectRoom = useRoomStore((state) => state.selectRoom)
  const roomId = params.roomId ?? null

  /* URL이 방 진입의 single source of truth이므로 알림 정리도 여기서 —
     클릭뿐 아니라 딥링크·뒤로가기로 들어와도 해당 방 알림이 지워진다 */
  useEffect(() => {
    if (roomId) {
      clearNotificationsForRoom(roomId)
    }
  }, [clearNotificationsForRoom, roomId])

  useEffect(() => {
    if (!roomId) {
      void selectRoom(null)
      return
    }

    if (loadStatus !== 'ready') {
      return
    }

    if (!rooms.some((room) => room.roomId === roomId)) {
      navigate(roomsPath(), { replace: true })
      return
    }

    void selectRoom(roomId)
  }, [loadStatus, navigate, roomId, rooms, selectRoom])
}
