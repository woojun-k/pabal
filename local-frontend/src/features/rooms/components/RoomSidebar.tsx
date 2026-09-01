import { useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import type { RoomResponse, UUID } from '../../../shared/types/api'
import { cn } from '../../../shared/utils/cn'
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

const groupHeader =
  'flex items-center gap-[5px] px-[8px] pb-[4px] pt-[12px] text-[12px] font-semibold uppercase tracking-[0] text-text-faint'

const roomItem =
  'my-[1px] flex min-h-[32px] w-full cursor-pointer items-center gap-[9px] rounded-sm border-0 bg-transparent px-[10px] py-[7px] text-left'

const presence = 'size-[9px] flex-[0_0_9px] shadow-[0_0_0_2px_var(--color-sidebar)]'

const unreadBadge =
  'grid h-[18px] min-w-[18px] place-items-center rounded-full bg-accent-strong px-[5px] font-mono text-[11px] font-semibold text-accent-ink'

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
    <div>
      <div className={groupHeader}>
        <button
          type="button"
          className="flex min-w-0 cursor-pointer items-center gap-[5px] border-0 bg-transparent p-0 text-inherit"
          onClick={() => setIsCollapsed((value) => !value)}
        >
          <span
            className={cn(
              'text-[9px] transition-transform duration-150 ease-[ease]',
              isCollapsed && '-rotate-90',
            )}
          >
            ▾
          </span>
          {label}
        </button>
        {onOpenCreate && (
          <button
            type="button"
            className="ml-auto grid size-[20px] cursor-pointer place-items-center rounded-[5px] border-0 bg-transparent p-0 text-[15px] text-inherit hover:bg-surface hover:text-text"
            title="새 대화"
            onClick={onOpenCreate}
          >
            ＋
          </button>
        )}
      </div>

      {!isCollapsed && rooms.map((room) => (
        <button
          type="button"
          className={cn(
            roomItem,
            room.roomId === activeRoomId
              ? 'bg-(--accent-weak) font-semibold text-text'
              : 'text-text-muted hover:bg-surface hover:text-text',
            room.unreadCount > 0 && room.roomId !== activeRoomId && 'font-semibold text-text',
          )}
          key={room.roomId}
          onClick={() => onSelectRoom(room.roomId)}
        >
          {isDirectGroup ? (
            <span
              className={cn(
                presence,
                room.type === 'GROUP' ? 'rounded-[3px] bg-text-faint' : 'rounded-full bg-[#34b27b]',
              )}
            />
          ) : (
            <span
              className={cn(
                'w-[16px] flex-[0_0_16px] text-center text-[14px]',
                room.roomId === activeRoomId ? 'text-accent-strong' : 'text-text-faint',
              )}
            >
              #
            </span>
          )}
          <span className="min-w-0 flex-1 truncate">{displayRoomName(room)}</span>
          {room.unreadCount > 0 && <span className={unreadBadge}>{room.unreadCount}</span>}
        </button>
      ))}

      {!isCollapsed && rooms.length === 0 && (
        <p className="empty-text mx-[8px] my-[10px]">목록이 없습니다.</p>
      )}
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
    isFetching,
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
      /* 작업 완료 피드백이지 미확인 활동 알림이 아니므로 roomId 태그를 붙이지 않는다
         (붙이면 바로 아래 onSelectRoom → 방 진입 알림 정리에 즉시 지워진다) */
      addNotification({
        kind: 'success',
        title: '대화방이 준비되었습니다',
        message: roomId,
      })
    }
  }

  return (
    <section className="grid content-start gap-[4px]" aria-label="채팅방">
      <RoomGroup
        label="채널"
        rooms={channels}
        activeRoomId={activeRoomId}
        onSelectRoom={onSelectRoom}
      />

      <button
        type="button"
        className="flex min-h-[32px] w-full cursor-pointer items-center gap-[9px] rounded-sm border-0 bg-transparent px-[10px] py-[7px] text-left text-text-muted hover:bg-surface hover:text-text disabled:cursor-not-allowed disabled:opacity-[0.55]"
        disabled={isFetching}
        onClick={() => void loadRooms()}
      >
        <span className="grid size-[18px] flex-[0_0_18px] place-items-center rounded-[6px] border border-dashed border-border-strong text-[13px] text-text-faint">↻</span>
        {isFetching ? '새로고침 중' : '채널 새로고침'}
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
        <form className="mb-[4px] mt-[7px] grid gap-[7px] px-[8px]" onSubmit={handleCreateDirect}>
          <input
            className="w-full min-w-0 rounded-sm border border-solid border-border-strong bg-bg px-[9px] py-[8px] font-mono text-[12px] text-text focus:border-(--accent-line) focus:[outline:3px_solid_color-mix(in_srgb,var(--accent-strong)_16%,transparent)]"
            value={participantId}
            onChange={(event) => setParticipantId(event.target.value)}
            placeholder="Participant UUID"
            spellCheck={false}
          />
          <button
            type="submit"
            className="cursor-pointer rounded-sm border-0 bg-accent-strong px-[10px] py-[8px] font-semibold text-accent-ink disabled:cursor-not-allowed disabled:opacity-[0.55]"
            disabled={isMutating}
          >
            만들기
          </button>
        </form>
      )}

      {error && <p className="error-text mx-[8px] my-[10px]">{error.message}</p>}
    </section>
  )
}
