import { appPaths } from '../config/paths'

export const apiContract = {
  apiPrefix: appPaths.apiPrefix,
  message: {
    contentMinLength: 1,
    contentMaxLength: 5000,
    pageSizeMin: 1,
    pageSizeMax: 100,
    pageSizeDefault: 50,
    optimisticSequence: 0,
  },
  room: {
    nameMaxLength: 50,
    channelNameMaxLength: 50,
    descriptionMaxLength: 255,
  },
  roomTypes: ['DIRECT', 'GROUP', 'CHANNEL'],
  roomStatuses: ['ACTIVE', 'PENDING_DELETION', 'DELETED'],
  messageTypes: ['USER', 'SYSTEM'],
  messageStatuses: ['ACTIVE', 'DELETED', 'EDITED'],
  roomEventTypes: [
    'MESSAGE_SENT',
    'MESSAGE_EDITED',
    'MESSAGE_DELETED',
    'MESSAGE_READ',
    'MEMBER_JOINED',
    'MEMBER_LEFT',
  ],
  typingStatuses: ['STARTED', 'STOPPED'],
  localRoles: ['tenant-admin', 'workspace-admin', 'channel-owner', 'pabal-admin'],
} as const

export type RoomType = (typeof apiContract.roomTypes)[number]
export type RoomStatus = (typeof apiContract.roomStatuses)[number]
export type MessageType = (typeof apiContract.messageTypes)[number]
export type MessageStatus = (typeof apiContract.messageStatuses)[number]
export type LocalRole = (typeof apiContract.localRoles)[number]
