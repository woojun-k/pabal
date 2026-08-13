import type { RoomResponse, UUID } from '../../../shared/types/api'
import { roomTitle } from '../roomTitle'

interface ContactsMainProps {
  directRooms: RoomResponse[]
  onOpenRoom: (roomId: UUID) => void
}

export function ContactsMain({ directRooms, onOpenRoom }: ContactsMainProps) {
  return (
    <section className="main contacts-main">
      <header className="chead">
        <div className="ttl">
          <span className="glyph">✉</span>
          <h1>연락처</h1>
        </div>
        <div className="topic">다이렉트 메시지와 그룹 대화를 빠르게 엽니다</div>
      </header>
      <div className="contact-grid">
        {directRooms.length === 0 && (
          <article className="empty-card">
            <div className="av lg">✉</div>
            <h2>연락처가 없습니다</h2>
            <p>메시지 탭에서 다이렉트 메시지를 만들면 여기에 표시됩니다.</p>
          </article>
        )}
        {directRooms.map((room) => (
          <article className="contact-card" key={room.roomId}>
            <span className="av">{room.type === 'GROUP' ? '그' : 'DM'}</span>
            <div>
              <h3>{roomTitle(room.name, room.type === 'GROUP' ? '그룹 메시지' : '다이렉트 메시지')}</h3>
              <p>{room.unreadCount > 0 ? `${room.unreadCount}개 안 읽음` : room.status}</p>
            </div>
            <button type="button" className="btn-ghost" onClick={() => onOpenRoom(room.roomId)}>
              메시지
            </button>
          </article>
        ))}
      </div>
    </section>
  )
}
