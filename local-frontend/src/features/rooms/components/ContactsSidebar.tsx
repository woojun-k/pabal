import type { RoomResponse, UUID } from '../../../shared/types/api'
import { cn } from '../../../shared/utils/cn'
import { roomTitle } from '../roomTitle'

const roomItem =
  'my-[1px] flex min-h-[32px] w-full cursor-pointer items-center gap-[9px] rounded-sm border-0 bg-transparent px-[10px] py-[7px] text-left text-text-muted hover:bg-surface hover:text-text'

const presence = 'size-[9px] flex-[0_0_9px] shadow-[0_0_0_2px_var(--color-sidebar)]'

const unreadBadge =
  'grid h-[18px] min-w-[18px] place-items-center rounded-full bg-accent-strong px-[5px] font-mono text-[11px] font-semibold text-accent-ink'

interface ContactsSidebarProps {
  directRooms: RoomResponse[]
  hasSession: boolean
  onOpenRoom: (roomId: UUID) => void
}

export function ContactsSidebar({ directRooms, hasSession, onOpenRoom }: ContactsSidebarProps) {
  return (
    <section className="grid content-start gap-[4px]" aria-label="연락처">
      <div className="flex items-center gap-[5px] px-[8px] pb-[4px] pt-[12px] text-[12px] font-semibold uppercase tracking-[0] text-text-faint">
        <span className="text-[9px] transition-transform duration-150 ease-[ease]">▾</span>
        다이렉트 메시지
        <span className="grid h-[18px] min-w-[18px] place-items-center rounded-full bg-surface px-[5px] font-mono text-[11px] font-semibold text-text-faint">
          {directRooms.length}
        </span>
      </div>
      {directRooms.length === 0 && (
        <p className="empty-text mx-[8px] my-[10px]">
          {hasSession ? '아직 연락처가 없습니다.' : '설정에서 로컬 토큰을 발급하세요.'}
        </p>
      )}
      {directRooms.map((room) => (
        <button
          type="button"
          className={roomItem}
          key={room.roomId}
          onClick={() => onOpenRoom(room.roomId)}
        >
          <span
            className={cn(
              presence,
              room.type === 'GROUP' ? 'rounded-[3px] bg-text-faint' : 'rounded-full bg-[#34b27b]',
            )}
          />
          <span className="min-w-0 flex-1 truncate">
            {roomTitle(room.name, room.type === 'GROUP' ? '그룹 메시지' : '다이렉트 메시지')}
          </span>
          {room.unreadCount > 0 && <span className={unreadBadge}>{room.unreadCount}</span>}
        </button>
      ))}
    </section>
  )
}
