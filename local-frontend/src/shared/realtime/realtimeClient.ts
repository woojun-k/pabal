import { env } from '../config/env'
import { mockRealtimeClient } from '../../mocks/realtime/mockRealtimeClient'
import { stompClient } from './stompClient'

export type {
  RealtimeClientError,
  RealtimeConnectionStatus,
  SubscriptionHandle,
} from './stompClient'

export const realtimeClient: typeof stompClient =
  env.backendMode === 'mock' ? mockRealtimeClient as unknown as typeof stompClient : stompClient
