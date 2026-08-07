import type { UUID } from '../shared/types/api'
import type { AppTab } from './tabs'

export const settingsPath = () => '/settings'

export const clientPath = (tenantId: UUID) => `/client/${tenantId}`

export const contactsPath = (tenantId: UUID) => `/client/${tenantId}/contacts`

export const roomPath = (tenantId: UUID, roomId: UUID) => `/client/${tenantId}/${roomId}`

export const deriveTab = (pathname: string): AppTab => {
  if (pathname.startsWith('/settings')) {
    return 'etc'
  }

  if (pathname.endsWith('/contacts')) {
    return 'contacts'
  }

  return 'messages'
}
