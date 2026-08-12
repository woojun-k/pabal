import { useEffect } from 'react'
import { useNavigate, useParams } from 'react-router'
import { useRoomStore } from '../../features/rooms/roomStore'
import { roomsPath } from '../paths'

export function useRoomRouteSync() {
  const params = useParams()
  const navigate = useNavigate()
  const rooms = useRoomStore((state) => state.rooms)
  const hasLoadedRooms = useRoomStore((state) => state.hasLoadedRooms)
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

    if (!hasLoadedRooms) {
      return
    }

    if (!rooms.some((room) => room.roomId === roomId)) {
      navigate(roomsPath(), { replace: true })
      return
    }

    if (activeRoomId !== roomId) {
      void selectRoom(roomId)
    }
  }, [activeRoomId, hasLoadedRooms, navigate, roomId, rooms, selectRoom])
}
