import { useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import type { RoomResponse, UUID } from '../../../shared/types/api'
import { isUuid } from '../../../shared/utils/uuid'
import { useNotificationStore } from '../../notifications/notificationStore'
import { useDirectRooms, useRoomStore } from '../roomStore'

type RoomGroupProps = {
  label: string
  rooms: RoomResponse[]
  activeRoomId: UUID | null
  isDirectGroup?: boolean
  onSelectRoom: (roomId: UUID) => void
  onOpenCreate?: () => void
}

const displayRoomName = (room: RoomResponse) => {
  if (room.name) {
    return room.name
  }

  if (room.type === 'DIRECT') {
    return '다이렉트 메시지'
  }

  if (room.type === 'GROUP') {
    return '그룹 메시지'
  }

  return room.roomId
}

function RoomGroup({
  label,
  rooms,
  activeRoomId,
  isDirectGroup = false,
  onSelectRoom,
  onOpenCreate,
}: RoomGroupProps) {
  const [isCollapsed, setIsCollapsed] = useState(false)

  return (
    <div className="room-group">
      <div className={isCollapsed ? 'grp collapsed' : 'grp'}>
        <button type="button" className="grp-main" onClick={() => setIsCollapsed((value) => !value)}>
          <span className="tri">▾</span>
          {label}
        </button>
        {onOpenCreate && (
          <button type="button" className="grp-add" title="새 대화" onClick={onOpenCreate}>
            ＋
          </button>
        )}
      </div>

      {!isCollapsed && rooms.map((room) => (
        <button
          type="button"
          className={[
            'nav-item',
            room.roomId === activeRoomId ? 'active' : '',
            room.unreadCount > 0 && room.roomId !== activeRoomId ? 'unread' : '',
          ].filter(Boolean).join(' ')}
          key={room.roomId}
          onClick={() => onSelectRoom(room.roomId)}
        >
          {isDirectGroup ? (
            <span className={room.type === 'GROUP' ? 'presence p-group' : 'presence p-online'} />
          ) : (
            <span className="glyph">#</span>
          )}
          <span className="nm">{displayRoomName(room)}</span>
          {room.unreadCount > 0 && <span className="pip">{room.unreadCount}</span>}
        </button>
      ))}

      {!isCollapsed && rooms.length === 0 && <p className="empty-text sb-empty">목록이 없습니다.</p>}
    </div>
  )
}

interface RoomSidebarProps {
  onSelectRoom: (roomId: UUID) => void
}

export function RoomSidebar({ onSelectRoom }: RoomSidebarProps) {
  const {
    rooms,
    activeRoomId,
    isLoading,
    isMutating,
    error,
    loadRooms,
    createDirectRoom,
  } = useRoomStore()
  const addNotification = useNotificationStore((state) => state.addNotification)
  const [participantId, setParticipantId] = useState('')
  const [isCreateOpen, setIsCreateOpen] = useState(false)
  const channels = useMemo(() => rooms.filter((room) => room.type === 'CHANNEL'), [rooms])
  const directRooms = useDirectRooms()

  const handleCreateDirect = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    if (!isUuid(participantId)) {
      addNotification({
        kind: 'warning',
        title: '잘못된 사용자 ID',
        message: '로컬 개발에서는 UUID 값을 입력하세요.',
      })
      return
    }

    const roomId = await createDirectRoom({ participantId })

    if (roomId) {
      setParticipantId('')
      setIsCreateOpen(false)
      onSelectRoom(roomId)
      addNotification({
        kind: 'success',
        title: '대화방이 준비되었습니다',
        message: roomId,
        roomId,
      })
    }
  }

  return (
    <section className="room-sidebar" aria-label="채팅방">
      <RoomGroup
        label="채널"
        rooms={channels}
        activeRoomId={activeRoomId}
        onSelectRoom={onSelectRoom}
      />

      <button type="button" className="create-row" onClick={() => void loadRooms()}>
        <span className="plus">↻</span>
        {isLoading ? '새로고침 중' : '채널 새로고침'}
      </button>

      <RoomGroup
        label="다이렉트 메시지"
        rooms={directRooms}
        activeRoomId={activeRoomId}
        isDirectGroup
        onSelectRoom={onSelectRoom}
        onOpenCreate={() => setIsCreateOpen((value) => !value)}
      />

      {isCreateOpen && (
        <form className="inline-create" onSubmit={handleCreateDirect}>
          <input
            value={participantId}
            onChange={(event) => setParticipantId(event.target.value)}
            placeholder="Participant UUID"
            spellCheck={false}
          />
          <button type="submit" disabled={isMutating}>
            만들기
          </button>
        </form>
      )}

      {error && <p className="error-text sb-error">{error.message}</p>}
    </section>
  )
}
