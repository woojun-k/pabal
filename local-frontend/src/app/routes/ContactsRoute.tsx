import { ContactsMain } from '../../features/rooms/components/ContactsMain'
import { useDirectRooms } from '../../features/rooms/roomStore'
import { useAppNavigation } from '../navigation'

export function ContactsRoute() {
  const { goToRoom } = useAppNavigation()
  const directRooms = useDirectRooms()

  return <ContactsMain directRooms={directRooms} onOpenRoom={goToRoom} />
}
