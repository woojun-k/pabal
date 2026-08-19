import { useEffect } from 'react'
import { useNavigate, useParams } from 'react-router'
import { useRoomStore } from '../../features/rooms/roomStore'
import { roomsPath } from '../paths'

export function useRoomRouteSync() {
  const params = useParams()
  const navigate = useNavigate()
  const rooms = useRoomStore((state) => state.rooms)
  const loadStatus = useRoomStore((state) => state.loadStatus)
  const activeRoomId = useRoomStore((state) => state.activeRoomId)
  const selectRoom = useRoomStore((state) => state.selectRoom)
  const roomId = params.roomId ?? null

  useEffect(() => {
    if (!roomId) {
      if (activeRoomId !== null) {
        void selectRoom(null)
      }
      return
    }

    if (loadStatus !== 'ready') {
      return
    }

    if (!rooms.some((room) => room.roomId === roomId)) {
      navigate(roomsPath(), { replace: true })
      return
    }

    if (activeRoomId !== roomId) {
      void selectRoom(roomId)
    }
  }, [activeRoomId, loadStatus, navigate, roomId, rooms, selectRoom])
}
