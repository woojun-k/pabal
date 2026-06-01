import type { StompHeaders } from '@stomp/stompjs'
import { tokenStorage, type StoredAuthSession } from './tokenStorage'

export const getStoredSession = () => tokenStorage.load()

export const getAccessToken = () => getStoredSession()?.accessToken ?? null

export const createBearerAuthorization = (accessToken: string) => `Bearer ${accessToken}`

export const createAuthHeaders = (session: StoredAuthSession | null = getStoredSession()) =>
  session
    ? {
        Authorization: createBearerAuthorization(session.accessToken),
      }
    : {}

export const createStompConnectHeaders = (accessToken: string): StompHeaders => ({
  Authorization: createBearerAuthorization(accessToken),
  access_token: accessToken,
})

export const clearLocalSession = () => {
  tokenStorage.clear()
}
