import type { UUID } from '../shared/types/api'
import type { AppTab } from './tabs'

export const settingsPath = () => '/settings'

export const roomsPath = () => '/rooms'

export const contactsPath = () => '/contacts'

export const roomPath = (roomId: UUID) => `/rooms/${roomId}`

export const deriveTab = (pathname: string): AppTab => {
  if (pathname.startsWith('/settings')) {
    return 'etc'
  }

  if (pathname.startsWith('/contacts')) {
    return 'contacts'
  }

  return 'messages'
}
