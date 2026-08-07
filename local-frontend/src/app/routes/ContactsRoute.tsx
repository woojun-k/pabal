import { useMemo } from 'react'
import { ContactsMain } from '../../features/rooms/components/ContactsMain'
import { useRoomStore } from '../../features/rooms/roomStore'
import { useAppNavigation } from '../navigation'

export function ContactsRoute() {
  const rooms = useRoomStore((state) => state.rooms)
  const { goToRoom } = useAppNavigation()
  const directRooms = useMemo(
    () => rooms.filter((room) => room.type === 'DIRECT' || room.type === 'GROUP'),
    [rooms],
  )

  return <ContactsMain directRooms={directRooms} onOpenRoom={goToRoom} />
}
