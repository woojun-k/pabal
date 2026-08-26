import type { AppTab } from '../tabs'
import { cn } from '../../shared/utils/cn'

const railTabs: Array<{ id: AppTab; label: string; icon: string }> = [
  { id: 'messages', label: '메시지', icon: '#' },
  { id: 'contacts', label: '연락처', icon: '✉' },
  { id: 'etc', label: '설정', icon: '⚙' },
]

/* preflight 미도입 상태라 border 유틸은 width·style·color를 모두 명시해야 한다
   (기본 border-style이 none, 기본 border-width가 medium) */
const railItem =
  'relative grid size-[40px] cursor-pointer place-items-center border border-solid border-transparent max-[680px]:size-[36px]'

/* 활성 워크스페이스 버튼은 hover 클래스를 붙이지 않는다 — 원본 CSS에서
   .rail-ws.active가 :hover와 동일 특이성·후순위라 hover를 이겼던 동작 보존 */
const railWs = cn(railItem, 'text-[14px] font-bold')

const railNav = cn(railItem, 'rounded-md text-[17px] hover:bg-surface hover:text-text')

interface WorkspaceRailProps {
  activeTab: AppTab
  unreadTotal: number
  hasSession: boolean
  onSelectTab: (tab: AppTab) => void
}

export function WorkspaceRail({ activeTab, unreadTotal, hasSession, onSelectTab }: WorkspaceRailProps) {
  return (
    <aside
      className="flex w-[62px] flex-[0_0_62px] flex-col items-center gap-[6px] border-0 border-r border-solid border-border bg-rail py-[14px] max-[980px]:row-span-full max-[980px]:h-svh max-[680px]:w-[52px] max-[680px]:basis-[52px]"
      aria-label="워크스페이스 내비게이션"
    >
      <button
        type="button"
        className={cn(
          railWs,
          'rounded-[12px] bg-accent-strong font-display text-accent-ink',
          "before:absolute before:left-[-12px] before:top-1/2 before:h-[22px] before:w-[4px] before:-translate-y-1/2 before:rounded-r-[4px] before:bg-text before:content-['']",
        )}
        title="아이누리"
      >
        아
      </button>
      <button
        type="button"
        className={cn(railWs, 'rounded-[13px] bg-surface text-text-muted hover:bg-surface hover:text-text')}
        title="사이드 프로젝트"
      >
        SP
      </button>
      <button
        type="button"
        className={cn(railWs, 'rounded-[13px] bg-surface text-text-muted hover:bg-surface hover:text-text')}
        title="스터디"
      >
        스
      </button>
      <div className={cn(railNav, 'bg-transparent text-text-faint')} title="워크스페이스 추가">
        ＋
      </div>
      <div className="my-[6px] h-px w-[24px] bg-border-strong" />
      {railTabs.map((tab) => (
        <button
          type="button"
          className={cn(
            railNav,
            activeTab === tab.id ? 'bg-surface text-text' : 'bg-transparent text-text-faint',
          )}
          key={tab.id}
          title={tab.label}
          onClick={() => onSelectTab(tab.id)}
          aria-pressed={activeTab === tab.id}
        >
          {tab.icon}
          {tab.id === 'messages' && unreadTotal > 0 && (
            <span className="absolute right-[2px] top-[2px] grid h-[18px] min-w-[18px] place-items-center rounded-full bg-accent-strong px-[5px] font-mono text-[11px] text-white">
              {unreadTotal}
            </span>
          )}
        </button>
      ))}
      <span
        className={cn(
          'mt-auto grid h-[24px] w-[34px] place-items-center rounded-full border border-solid text-[10px] font-bold uppercase',
          hasSession
            ? 'border-(--accent-line) bg-accent-strong text-accent-ink'
            : 'border-border-strong bg-surface text-text-faint',
        )}
      >
        {hasSession ? 'on' : 'off'}
      </span>
    </aside>
  )
}
