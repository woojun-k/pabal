import type { IMessage } from '@stomp/stompjs'
import { subscriptionDestinations } from '../../shared/realtime/destinations'
import type {
  RealtimeClientError,
  RealtimeConnectionStatus,
  SubscriptionHandle,
} from '../../shared/realtime/stompClient'
import type { UUID } from '../../shared/types/api'
import type {
  RoomEventEnvelope,
  RoomSubscriptionRevokedRealtimePayload,
  TypingEventPayload,
  TypingRequest,
} from '../../shared/types/realtime'
import { principalFromToken } from '../mockAuth'
import type { MockPrincipal } from '../mockAuth'

type JsonHandler<TPayload> = (payload: TPayload, message: IMessage) => void

type ManagedSubscription = {
  destination: string
  callback: (payload: unknown) => void
}

const mockMessage = (payload: unknown): IMessage => ({
  ack: () => undefined,
  binaryBody: new Uint8Array(),
  body: JSON.stringify(payload),
  command: 'MESSAGE',
  headers: {},
  isBinaryBody: false,
  nack: () => undefined,
})

class MockRealtimeClient {
  private currentPrincipal: MockPrincipal | null = null
  private desiredSubscriptions = new Map<string, ManagedSubscription>()
  private status: RealtimeConnectionStatus = 'idle'
  private statusListeners = new Set<(status: RealtimeConnectionStatus) => void>()
  private errorListeners = new Set<(error: RealtimeClientError) => void>()

  getStatus() {
    return this.status
  }

  getSubscriptionIds() {
    return [...this.desiredSubscriptions.keys()]
  }

  onStatusChange(listener: (status: RealtimeConnectionStatus) => void) {
    this.statusListeners.add(listener)
    listener(this.status)

    return () => {
      this.statusListeners.delete(listener)
    }
  }

  onError(listener: (error: RealtimeClientError) => void) {
    this.errorListeners.add(listener)

    return () => {
      this.errorListeners.delete(listener)
    }
  }

  connect(options: { accessToken: string }) {
    this.currentPrincipal = principalFromToken(options.accessToken)

    if (!this.currentPrincipal) {
      this.emitError({ message: 'Invalid mock access token' })
      this.setStatus('error')
      return
    }

    this.setStatus('connecting')
    queueMicrotask(() => this.setStatus('connected'))
  }

  async disconnect() {
    this.currentPrincipal = null
    this.setStatus('disconnecting')
    queueMicrotask(() => this.setStatus('disconnected'))
  }

  clearSubscriptions() {
    this.desiredSubscriptions.clear()
  }

  unsubscribe(id: string) {
    this.desiredSubscriptions.delete(id)
  }

  subscribeRoomEvents(
    tenantId: UUID,
    chatRoomId: UUID,
    handler: JsonHandler<RoomEventEnvelope>,
  ) {
    return this.subscribe(subscriptionDestinations.roomEvents(tenantId, chatRoomId), (payload) => {
      handler(payload as RoomEventEnvelope, mockMessage(payload))
    })
  }

  subscribeTyping(
    tenantId: UUID,
    chatRoomId: UUID,
    handler: JsonHandler<TypingEventPayload>,
  ) {
    return this.subscribe(subscriptionDestinations.roomTyping(tenantId, chatRoomId), (payload) => {
      handler(payload as TypingEventPayload, mockMessage(payload))
    })
  }

  subscribeUserControl(handler: JsonHandler<RoomSubscriptionRevokedRealtimePayload>) {
    return this.subscribe(subscriptionDestinations.userControl, (payload) => {
      handler(payload as RoomSubscriptionRevokedRealtimePayload, mockMessage(payload))
    })
  }

  sendTypingStart(request: TypingRequest) {
    this.publishTyping(request, 'STARTED')
  }

  sendTypingStop(request: TypingRequest) {
    this.publishTyping(request, 'STOPPED')
  }

  publishRoomEvent(event: RoomEventEnvelope) {
    this.publish(subscriptionDestinations.roomEvents(event.tenantId, event.chatRoomId), event)
  }

  publishUserControl(payload: RoomSubscriptionRevokedRealtimePayload) {
    this.publish(subscriptionDestinations.userControl, payload)
  }

  private publishTyping(request: TypingRequest, status: TypingEventPayload['status']) {
    const userId = this.currentPrincipal?.userId

    if (!userId) {
      this.emitError({ message: 'Cannot publish typing event before mock realtime is connected' })
      return
    }

    this.publish(subscriptionDestinations.roomTyping(request.tenantId, request.chatRoomId), {
      userId,
      status,
      occurredAt: new Date().toISOString(),
    } satisfies TypingEventPayload)
  }

  private publish(destination: string, payload: unknown) {
    if (this.status !== 'connected') {
      return
    }

    for (const subscription of this.desiredSubscriptions.values()) {
      if (subscription.destination === destination) {
        subscription.callback(payload)
      }
    }
  }

  private subscribe(
    destination: string,
    callback: (payload: unknown) => void,
  ): SubscriptionHandle {
    const id = `${destination}:${crypto.randomUUID()}`
    this.desiredSubscriptions.set(id, { destination, callback })

    return {
      id,
      destination,
      unsubscribe: () => this.unsubscribe(id),
    }
  }

  private setStatus(status: RealtimeConnectionStatus) {
    this.status = status
    for (const listener of this.statusListeners) {
      listener(status)
    }
  }

  private emitError(error: RealtimeClientError) {
    for (const listener of this.errorListeners) {
      listener(error)
    }
  }
}

export const mockRealtimeClient = new MockRealtimeClient()
