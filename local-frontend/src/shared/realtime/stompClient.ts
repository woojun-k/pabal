import {
  Client,
  type IFrame,
  type IMessage,
  type IStompSocket,
  type StompHeaders,
  type StompSubscription,
} from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { env } from '../config/env'
import {
  isRoomSubscriptionRevokedPayload,
  parseRoomEvent,
  parseTypingEvent,
} from './eventGuards'
import { createStompConnectHeaders } from '../security/session'
import type { UUID } from '../types/api'
import type {
  RoomEventEnvelope,
  RoomSubscriptionRevokedRealtimePayload,
  TypingEventPayload,
  TypingRequest,
} from '../types/realtime'
import { appDestinations, subscriptionDestinations } from './destinations'

export type RealtimeConnectionStatus =
  | 'idle'
  | 'connecting'
  | 'connected'
  | 'disconnecting'
  | 'disconnected'
  | 'error'

export type RealtimeClientError = {
  message: string
  frame?: IFrame
  cause?: unknown
}

export type SubscriptionHandle = {
  id: string
  destination: string
  unsubscribe: () => void
}

type ConnectOptions = {
  accessToken: string
  reconnectDelayMs?: number
}

type JsonHandler<TPayload> = (payload: TPayload, message: IMessage) => void

type ManagedSubscription = {
  destination: string
  callback: (message: IMessage) => void
  headers?: StompHeaders
}

const defaultReconnectDelayMs = 5_000
const heartbeatMs = 10_000

class PabalStompClient {
  private client: Client | null = null
  private currentAccessToken: string | null = null
  private desiredSubscriptions = new Map<string, ManagedSubscription>()
  private activeSubscriptions = new Map<string, StompSubscription>()
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

  connect(options: ConnectOptions) {
    if (this.client?.active || this.client?.connected) {
      if (this.currentAccessToken !== options.accessToken) {
        void this.reconnect(options)
      }
      return
    }

    this.activate(options)
  }

  async disconnect() {
    if (!this.client) {
      this.currentAccessToken = null
      this.setStatus('disconnected')
      return
    }

    this.currentAccessToken = null
    this.setStatus('disconnecting')
    this.activeSubscriptions.clear()
    await this.client.deactivate()
    this.setStatus('disconnected')
  }

  clearSubscriptions() {
    for (const subscription of this.activeSubscriptions.values()) {
      subscription.unsubscribe()
    }

    this.activeSubscriptions.clear()
    this.desiredSubscriptions.clear()
  }

  private activate(options: ConnectOptions) {
    this.currentAccessToken = options.accessToken
    this.setStatus('connecting')
    this.client = new Client({
      webSocketFactory: () => new SockJS(env.wsUrl) as unknown as IStompSocket,
      connectHeaders: createStompConnectHeaders(options.accessToken),
      reconnectDelay: options.reconnectDelayMs ?? defaultReconnectDelayMs,
      heartbeatIncoming: heartbeatMs,
      heartbeatOutgoing: heartbeatMs,
      onConnect: () => {
        this.setStatus('connected')
        this.resubscribeAll()
      },
      onDisconnect: () => {
        this.activeSubscriptions.clear()
        this.setStatus('disconnected')
      },
      onStompError: (frame) => {
        this.emitError({
          message: frame.headers.message || 'STOMP broker error',
          frame,
        })
        this.setStatus('error')
      },
      onWebSocketClose: () => {
        this.activeSubscriptions.clear()
        if (this.status !== 'disconnecting') {
          this.setStatus('disconnected')
        }
      },
      onWebSocketError: (cause) => {
        this.emitError({
          message: 'WebSocket connection failed',
          cause,
        })
        this.setStatus('error')
      },
    })

    this.client.activate()
  }

  subscribe(
    destination: string,
    callback: (message: IMessage) => void,
    headers?: StompHeaders,
  ): SubscriptionHandle {
    const id = this.createSubscriptionId(destination)
    this.desiredSubscriptions.set(id, { destination, callback, headers })

    if (this.client?.connected) {
      this.subscribeNow(id)
    }

    return {
      id,
      destination,
      unsubscribe: () => this.unsubscribe(id),
    }
  }

  subscribeJson<TPayload>(
    destination: string,
    handler: JsonHandler<TPayload>,
    headers?: StompHeaders,
  ) {
    return this.subscribe(
      destination,
      (message) => {
        try {
          handler(JSON.parse(message.body) as TPayload, message)
        } catch (cause) {
          this.emitError({
            message: `Failed to parse STOMP JSON payload from ${destination}`,
            cause,
          })
        }
      },
      headers,
    )
  }

  unsubscribe(id: string) {
    this.activeSubscriptions.get(id)?.unsubscribe()
    this.activeSubscriptions.delete(id)
    this.desiredSubscriptions.delete(id)
  }

  subscribeRoomEvents(
    tenantId: UUID,
    chatRoomId: UUID,
    handler: JsonHandler<RoomEventEnvelope>,
  ) {
    const destination = subscriptionDestinations.roomEvents(tenantId, chatRoomId)
    return this.subscribe(
      destination,
      (message) => {
        try {
          handler(parseRoomEvent(message.body), message)
        } catch (cause) {
          this.emitError({
            message: `Failed to parse room event from ${destination}`,
            cause,
          })
        }
      },
    )
  }

  subscribeTyping(
    tenantId: UUID,
    chatRoomId: UUID,
    handler: JsonHandler<TypingEventPayload>,
  ) {
    const destination = subscriptionDestinations.roomTyping(tenantId, chatRoomId)
    return this.subscribe(
      destination,
      (message) => {
        try {
          handler(parseTypingEvent(message.body), message)
        } catch (cause) {
          this.emitError({
            message: `Failed to parse typing event from ${destination}`,
            cause,
          })
        }
      },
    )
  }

  subscribeUserControl(handler: JsonHandler<RoomSubscriptionRevokedRealtimePayload>) {
    return this.subscribeJson(subscriptionDestinations.userControl, (payload, message) => {
      if (!isRoomSubscriptionRevokedPayload(payload)) {
        this.emitError({
          message: `Invalid user control payload from ${subscriptionDestinations.userControl}`,
        })
        return
      }
      handler(payload, message)
    })
  }

  subscribeRawRoomEvents(
    tenantId: UUID,
    chatRoomId: UUID,
    handler: JsonHandler<RoomEventEnvelope>,
  ) {
    return this.subscribeJson(
      subscriptionDestinations.roomEvents(tenantId, chatRoomId),
      handler,
    )
  }

  subscribeRawTyping(
    tenantId: UUID,
    chatRoomId: UUID,
    handler: JsonHandler<TypingEventPayload>,
  ) {
    return this.subscribeJson(
      subscriptionDestinations.roomTyping(tenantId, chatRoomId),
      handler,
    )
  }

  sendTypingStart(request: TypingRequest) {
    this.publishJson(appDestinations.typingStart, request)
  }

  sendTypingStop(request: TypingRequest) {
    this.publishJson(appDestinations.typingStop, request)
  }

  publishJson(destination: string, body: unknown, headers?: StompHeaders) {
    if (!this.client?.connected) {
      this.emitError({
        message: `Cannot publish to ${destination} before STOMP is connected`,
      })
      return
    }

    this.client.publish({
      destination,
      body: JSON.stringify(body),
      headers: {
        'content-type': 'application/json',
        ...headers,
      },
    })
  }

  private async reconnect(options: ConnectOptions) {
    this.currentAccessToken = options.accessToken
    this.setStatus('disconnecting')
    this.activeSubscriptions.clear()
    await this.client?.deactivate()
    this.client = null
    this.activate(options)
  }

  private resubscribeAll() {
    this.activeSubscriptions.clear()

    for (const id of this.desiredSubscriptions.keys()) {
      this.subscribeNow(id)
    }
  }

  private subscribeNow(id: string) {
    const managed = this.desiredSubscriptions.get(id)

    if (!managed || !this.client?.connected) {
      return
    }

    this.activeSubscriptions.get(id)?.unsubscribe()
    const subscription = this.client.subscribe(
      managed.destination,
      managed.callback,
      managed.headers,
    )
    this.activeSubscriptions.set(id, subscription)
  }

  private createSubscriptionId(destination: string) {
    return `${destination}:${crypto.randomUUID()}`
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

export const stompClient = new PabalStompClient()
