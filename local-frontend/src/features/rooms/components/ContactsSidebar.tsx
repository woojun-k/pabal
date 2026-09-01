import type { RoomResponse, UUID } from '../../../shared/types/api'
import { roomTitle } from '../roomTitle'

interface ContactsSidebarProps {
  directRooms: RoomResponse[]
  hasSession: boolean
  onOpenRoom: (roomId: UUID) => void
}

export function ContactsSidebar({ directRooms, hasSession, onOpenRoom }: ContactsSidebarProps) {
  return (
    <section className="grid content-start gap-[4px]" aria-label="연락처">
      <div className="grp">
        <span className="tri">▾</span>
        다이렉트 메시지
        <span className="pip muted">{directRooms.length}</span>
      </div>
      {directRooms.length === 0 && (
        <p className="empty-text sb-empty">
          {hasSession ? '아직 연락처가 없습니다.' : '설정에서 로컬 토큰을 발급하세요.'}
        </p>
      )}
      {directRooms.map((room) => (
        <button
          type="button"
          className="nav-item"
          key={room.roomId}
          onClick={() => onOpenRoom(room.roomId)}
        >
          <span className={room.type === 'GROUP' ? 'presence p-group' : 'presence p-online'} />
          <span className="nm">{roomTitle(room.name, room.type === 'GROUP' ? '그룹 메시지' : '다이렉트 메시지')}</span>
          {room.unreadCount > 0 && <span className="pip">{room.unreadCount}</span>}
        </button>
      ))}
    </section>
  )
}
