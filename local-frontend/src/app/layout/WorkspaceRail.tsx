export type AppTab = 'messages' | 'contacts' | 'etc'

const railTabs: Array<{ id: AppTab; label: string; icon: string }> = [
  { id: 'messages', label: '메시지', icon: '#' },
  { id: 'contacts', label: '연락처', icon: '✉' },
  { id: 'etc', label: '설정', icon: '⚙' },
]

interface WorkspaceRailProps {
  activeTab: AppTab
  unreadTotal: number
  sessionReady: boolean
  onSelectTab: (tab: AppTab) => void
}

export function WorkspaceRail({ activeTab, unreadTotal, sessionReady, onSelectTab }: WorkspaceRailProps) {
  return (
    <aside className="rail" aria-label="워크스페이스 내비게이션">
      <button type="button" className="rail-ws active" title="아이누리">
        아
      </button>
      <button type="button" className="rail-ws" title="사이드 프로젝트">
        SP
      </button>
      <button type="button" className="rail-ws" title="스터디">
        스
      </button>
      <div className="rail-nav" title="워크스페이스 추가">＋</div>
      <div className="rail-div" />
      {railTabs.map((tab) => (
        <button
          type="button"
          className={activeTab === tab.id ? 'rail-nav is-active' : 'rail-nav'}
          key={tab.id}
          title={tab.label}
          onClick={() => onSelectTab(tab.id)}
          aria-pressed={activeTab === tab.id}
        >
          {tab.icon}
          {tab.id === 'messages' && unreadTotal > 0 && <span className="rail-pip">{unreadTotal}</span>}
        </button>
      ))}
      <span className={sessionReady ? 'nav-session is-ready' : 'nav-session'}>
        {sessionReady ? 'on' : 'off'}
      </span>
    </aside>
  )
}
