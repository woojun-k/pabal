import { useState } from 'react'
import type { FormEvent } from 'react'
import { isUuid } from '../../shared/utils/uuid'
import { formatDateTime } from '../../shared/utils/dateTime'
import { useNotificationStore } from '../notifications/notificationStore'
import { useRoomStore } from './roomStore'

export function RoomSidebar() {
  const {
    rooms,
    activeRoomId,
    isLoading,
    isMutating,
    error,
    loadRooms,
    selectRoom,
    createDirectRoom,
    leaveActiveRoom,
  } = useRoomStore()
  const addNotification = useNotificationStore((state) => state.addNotification)
  const [participantId, setParticipantId] = useState('')

  const handleCreateDirect = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    if (!isUuid(participantId)) {
      addNotification({
        kind: 'warning',
        title: 'Invalid participant ID',
        message: 'Use a UUID value for local development.',
      })
      return
    }

    const roomId = await createDirectRoom({ participantId })

    if (roomId) {
      setParticipantId('')
      addNotification({
        kind: 'success',
        title: 'Room ready',
        message: roomId,
      })
    }
  }

  return (
    <section className="room-sidebar" aria-label="Chat rooms">
      <div className="sidebar-section-header">
        <span>Rooms</span>
        <button type="button" className="secondary compact" onClick={() => void loadRooms()}>
          Refresh
        </button>
      </div>

      <form className="stacked-form" onSubmit={handleCreateDirect}>
        <label>
          <span>Participant UUID</span>
          <input
            value={participantId}
            onChange={(event) => setParticipantId(event.target.value)}
            placeholder="00000000-0000-4000-8000-000000000000"
            spellCheck={false}
          />
        </label>
        <button type="submit" disabled={isMutating}>
          Direct room
        </button>
      </form>

      {error && <p className="error-text">{error.message}</p>}

      <div className="room-list" aria-busy={isLoading}>
        {rooms.length === 0 && (
          <p className="empty-text">{isLoading ? 'Loading rooms...' : 'No rooms yet'}</p>
        )}
        {rooms.map((room) => (
          <button
            type="button"
            className={room.roomId === activeRoomId ? 'room-item is-active' : 'room-item'}
            key={room.roomId}
            onClick={() => void selectRoom(room.roomId)}
          >
            <span>
              <strong>{room.name || room.roomId}</strong>
              <small>{room.type} · {room.status}</small>
              <small>{formatDateTime(room.lastMessageAt ?? room.joinedAt)}</small>
            </span>
            {room.unreadCount > 0 && <em>{room.unreadCount}</em>}
          </button>
        ))}
      </div>

      <button
        type="button"
        className="secondary danger compact full-width"
        disabled={!activeRoomId || isMutating}
        onClick={() => void leaveActiveRoom()}
      >
        Leave selected room
      </button>
    </section>
  )
}
