# PR1 Frontend Componentization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `App.tsx`(238줄)를 스펙의 2-계층 구조(`app/layout` + `features` + `shared`)로 분해한다. 동작·외관 완전 불변.

**Architecture:** 앱 셸(rail, sidebar frame)은 `src/app/layout/`으로, 도메인 탭 콘텐츠(연락처, 설정 사이드바)는 `features/rooms|auth/components/`로 추출한다. `App.tsx`는 `src/app/`으로 이동해 상태 접합과 조립만 담당한다. CSS는 내용 무변경 — `App.css` import 위치만 `main.tsx`로 옮긴다.

**Tech Stack:** React 19 + TypeScript 6 + Vite 8 + zustand 5. 테스트 인프라 없음 — 검증은 baseline 스크린샷 비교 + `tsc` + `vite build` + `eslint`.

**Spec:** `local-frontend/docs/specs/2026-08-06-frontend-architecture-design.md`

## Global Constraints

- 작업 디렉토리: `/home/jmchoi/project/pabal-dev-auth-onboarding/local-frontend` (워크트리, 브랜치 `refactor/frontend-componentization`)
- `src/App.css`와 `src/index.css`의 **내용 수정 금지** (import 구문 위치 이동만 허용)
- 신규 npm 의존성 추가 금지
- 동작·외관 불변: JSX 마크업(클래스명, 텍스트, 어트리뷰트)을 바이트 단위로 보존해 추출
- import 방향: `app` → `features` → `shared` 단방향 (shared는 features를 import 금지)
- `git push` 금지 (front 브랜치 정책상 push는 메인 개발자 승인 후)
- 커밋 메시지는 conventional commits 한국어, 말미에 `Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>` trailer
- 스크린샷 저장 경로: `/tmp/claude-1000/-home-jmchoi-project-pabal/3081f723-64a7-4df2-8cab-d687b1faef5c/scratchpad/pr1-baseline/` 및 `pr1-after/`
- 고정 테스트 계정(스크린샷 재현성): User ID `11111111-1111-4111-8111-111111111111`, Tenant ID `22222222-2222-4222-8222-222222222222`, role 기본값 유지

---

### Task 1: Baseline 스크린샷 캡처 (커밋 없음)

**Files:** 없음 (검증 아티팩트만 생성)

**Interfaces:**
- Consumes: 없음
- Produces: `pr1-baseline/{01-no-token,02-settings,03-messages,04-contacts}.png` — Task 7이 비교 기준으로 사용

- [ ] **Step 1: dev server 기동 (mock 모드)**

```bash
cd /home/jmchoi/project/pabal-dev-auth-onboarding/local-frontend
npm run dev  # background로 실행, http://localhost:5173 응답 확인
```

- [ ] **Step 2: Playwright로 브라우저 초기화**

Playwright MCP 도구 사용 (`ToolSearch`로 `browser_navigate`, `browser_resize`, `browser_click`, `browser_type`, `browser_take_screenshot`, `browser_snapshot`, `browser_close` 로드).

1. `browser_navigate` → `http://localhost:5173`
2. `browser_resize` → width 1440, height 900 (고정 뷰포트 — after 캡처와 동일해야 함)

- [ ] **Step 3: 토큰 없음 상태 캡처**

`browser_take_screenshot` → `pr1-baseline/01-no-token.png`
기대 화면: "메시지를 시작하려면 토큰이 필요합니다" empty state

- [ ] **Step 4: 설정 탭에서 고정 UUID로 토큰 발급**

1. `button[title="설정"]` 클릭 (왼쪽 rail의 ⚙)
2. "User ID" 라벨의 input을 비우고 `11111111-1111-4111-8111-111111111111` 입력
3. "Tenant ID" 라벨의 input을 비우고 `22222222-2222-4222-8222-222222222222` 입력
4. "Issue local token" 버튼 클릭
5. status pill이 "Token ready"로 바뀔 때까지 대기
6. `browser_take_screenshot` → `pr1-baseline/02-settings.png`

- [ ] **Step 5: 메시지·연락처 탭 캡처**

1. `button[title="메시지"]` 클릭 → seed 채팅방 목록 로드 대기 → `pr1-baseline/03-messages.png`
2. `button[title="연락처"]` 클릭 → `pr1-baseline/04-contacts.png`

- [ ] **Step 6: 브라우저·dev server 종료**

`browser_close` 후 dev server 프로세스 kill. 4개 PNG 존재 확인:

```bash
ls /tmp/claude-1000/-home-jmchoi-project-pabal/3081f723-64a7-4df2-8cab-d687b1faef5c/scratchpad/pr1-baseline/
```

---

### Task 2: App.tsx를 src/app/으로 이동 (pure move)

**Files:**
- Move: `src/App.tsx` → `src/app/App.tsx`
- Modify: `src/main.tsx`

**Interfaces:**
- Consumes: 없음
- Produces: `src/app/App.tsx` (default export `App` 유지) — 이후 태스크 전부 이 파일을 수정

- [ ] **Step 1: git mv로 이동**

```bash
cd /home/jmchoi/project/pabal-dev-auth-onboarding/local-frontend
mkdir -p src/app && git mv src/App.tsx src/app/App.tsx
```

- [ ] **Step 2: src/app/App.tsx의 import 경로 수정**

파일 상단 import 블록에서 (1) 모든 `'./features/…'` → `'../features/…'`, `'./shared/…'` → `'../shared/…'`로 변경, (2) `import './App.css'` 줄 **삭제**:

```tsx
import { useMemo, useState } from 'react'
import { DevAuthPanel } from '../features/auth/DevAuthPanel'
import { useAuthStore } from '../features/auth/authStore'
import { MessagePanel } from '../features/messages/MessagePanel'
import { NotificationTray } from '../features/notifications/NotificationTray'
import { useNotificationStore } from '../features/notifications/notificationStore'
import { RealtimeBridge } from '../features/realtime/RealtimeBridge'
import { RealtimeStatusPanel } from '../features/realtime/RealtimeStatusPanel'
import { RoomSidebar } from '../features/rooms/RoomSidebar'
import { useRoomStore } from '../features/rooms/roomStore'
import { BackendModeBadge } from '../shared/config/BackendModeBadge'
import { displayRole } from '../shared/security/roles'
import type { UUID } from '../shared/types/api'
```

- [ ] **Step 3: src/main.tsx 수정 (App 경로 + App.css 인계)**

```tsx
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import './App.css'
import App from './app/App.tsx'
import { env } from './shared/config/env.ts'
```

(CSS cascade 순서 보존: 기존에도 index.css → App.css 순서로 로드됐다)

- [ ] **Step 4: 타입·빌드·린트 검증**

```bash
npm run build && npm run lint
```
Expected: 둘 다 성공 (build 스크립트가 `tsc -b`를 포함)

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "refactor: App 진입점을 app 레이어로 이동

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 3: 앱 셸 추출 — WorkspaceRail + SidebarFrame

**Files:**
- Create: `src/app/layout/WorkspaceRail.tsx`
- Create: `src/app/layout/SidebarFrame.tsx`
- Modify: `src/app/App.tsx`

**Interfaces:**
- Consumes: 없음 (셸은 store를 모름 — props만 받음)
- Produces:
  - `WorkspaceRail.tsx`: `export type AppTab = 'messages' | 'contacts' | 'etc'`, `export function WorkspaceRail(props: { activeTab: AppTab; unreadTotal: number; sessionReady: boolean; onSelectTab: (tab: AppTab) => void })`
  - `SidebarFrame.tsx`: `export function SidebarFrame(props: { sessionReady: boolean; onOpenSettings: () => void; children: ReactNode })`

- [ ] **Step 1: WorkspaceRail.tsx 생성** (마크업은 App.tsx의 `<aside className="rail">` 블록 그대로, `accessToken`→`sessionReady`, `setActiveTab`→`onSelectTab`만 치환)

```tsx
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
```

- [ ] **Step 2: SidebarFrame.tsx 생성** (App.tsx의 `<aside className="sidebar">` 블록 그대로, `sb-scroll` 내부만 `children`으로)

```tsx
import type { ReactNode } from 'react'

interface SidebarFrameProps {
  sessionReady: boolean
  onOpenSettings: () => void
  children: ReactNode
}

export function SidebarFrame({ sessionReady, onOpenSettings, children }: SidebarFrameProps) {
  return (
    <aside className="sidebar" aria-label="워크스페이스 사이드바">
      <div className="ws-head">
        <span className="av sm">우</span>
        <span className="nm display">아이누리</span>
        <button type="button" className="icobtn" title="새 메시지">✎</button>
        <span className="chev">▾</span>
      </div>
      <div className="sb-search">
        <span>🔍</span>
        <span>검색 또는 점프</span>
        <kbd>⌘K</kbd>
      </div>
      <div className="sb-scroll">{children}</div>
      <div className="me-bar">
        <div className="av-pos">
          <span className="av sm">우</span>
          <span className="presence p-online" />
        </div>
        <div className="me-text">
          <div className="nm">정우</div>
          <div className="st">{sessionReady ? '집중 모드' : '오프라인'}</div>
        </div>
        <button type="button" className="icobtn" onClick={onOpenSettings} title="설정">
          ⚙
        </button>
      </div>
    </aside>
  )
}
```

- [ ] **Step 3: App.tsx에서 셸 마크업을 컴포넌트 호출로 치환**

`App.tsx`의 return 블록을 다음으로 교체 (`renderSidebar`/`renderMain`은 아직 유지):

```tsx
  return (
    <main className="app-shell" data-theme="light">
      <RealtimeBridge />
      <NotificationTray />
      <WorkspaceRail
        activeTab={activeTab}
        unreadTotal={unreadTotal}
        sessionReady={Boolean(accessToken)}
        onSelectTab={setActiveTab}
      />
      <SidebarFrame sessionReady={Boolean(accessToken)} onOpenSettings={() => setActiveTab('etc')}>
        {renderSidebar()}
      </SidebarFrame>
      {renderMain()}
    </main>
  )
```

import 추가 및 정리:
- 추가: `import { SidebarFrame } from './layout/SidebarFrame'`, `import { WorkspaceRail, type AppTab } from './layout/WorkspaceRail'`
- 삭제: 파일 상단의 `type AppTab = …` 선언과 `const railTabs = […]` 배열 (WorkspaceRail로 이사함)

- [ ] **Step 4: 검증**

```bash
npm run build && npm run lint
```
Expected: 성공. `noUnusedLocals` 덕에 잔여 미사용 import가 있으면 여기서 에러로 드러난다.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "refactor: 앱 셸을 WorkspaceRail과 SidebarFrame으로 분리

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 4: 도메인 탭 콘텐츠 추출 — rooms/auth feature 컴포넌트

**Files:**
- Create: `src/features/rooms/roomTitle.ts`
- Create: `src/features/rooms/components/ContactsSidebar.tsx`
- Create: `src/features/rooms/components/ContactsMain.tsx`
- Create: `src/features/auth/components/SettingsSidebar.tsx`
- Modify: `src/app/App.tsx`

**Interfaces:**
- Consumes: Task 3의 `AppTab`, `WorkspaceRail`, `SidebarFrame`
- Produces:
  - `roomTitle.ts`: `export const roomTitle = (roomName: string, fallback: string) => string`
  - `ContactsSidebar.tsx`: `export function ContactsSidebar(props: { directRooms: RoomResponse[]; hasSession: boolean; onOpenRoom: (roomId: UUID) => void })`
  - `ContactsMain.tsx`: `export function ContactsMain(props: { directRooms: RoomResponse[]; onOpenRoom: (roomId: UUID) => void })` — 원본 마크업이 토큰 여부를 참조하지 않으므로 `hasSession` 없음
  - `SettingsSidebar.tsx`: `export function SettingsSidebar()` — props 없음, `useAuthStore` 직접 구독

- [ ] **Step 1: roomTitle.ts 생성** (App.tsx의 헬퍼 이사 — ContactsSidebar/ContactsMain 공용)

```ts
export const roomTitle = (roomName: string, fallback: string) => roomName || fallback
```

- [ ] **Step 2: ContactsSidebar.tsx 생성** (App.tsx `renderSidebar`의 contacts 분기 그대로)

```tsx
import type { RoomResponse, UUID } from '../../../shared/types/api'
import { roomTitle } from '../roomTitle'

interface ContactsSidebarProps {
  directRooms: RoomResponse[]
  hasSession: boolean
  onOpenRoom: (roomId: UUID) => void
}

export function ContactsSidebar({ directRooms, hasSession, onOpenRoom }: ContactsSidebarProps) {
  return (
    <section className="contact-sidebar" aria-label="연락처">
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
```

- [ ] **Step 3: ContactsMain.tsx 생성** (App.tsx `renderMain`의 contacts 분기 그대로)

```tsx
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
```

- [ ] **Step 4: SettingsSidebar.tsx 생성** (App.tsx `renderSidebar`의 설정 분기 그대로 — auth 도메인이므로 store 직접 구독)

```tsx
import { BackendModeBadge } from '../../../shared/config/BackendModeBadge'
import { displayRole } from '../../../shared/security/roles'
import { useAuthStore } from '../authStore'

export function SettingsSidebar() {
  const userId = useAuthStore((state) => state.userId)
  const tenantId = useAuthStore((state) => state.tenantId)
  const roles = useAuthStore((state) => state.roles)

  return (
    <section className="settings-sidebar" aria-label="설정">
      <div className="settings-nav-item is-active">
        <span>연결</span>
        <BackendModeBadge />
      </div>
      <dl className="session-summary">
        <div>
          <dt>User</dt>
          <dd>{userId ?? '-'}</dd>
        </div>
        <div>
          <dt>Tenant</dt>
          <dd>{tenantId ?? '-'}</dd>
        </div>
        <div>
          <dt>Role</dt>
          <dd>{displayRole(roles)}</dd>
        </div>
      </dl>
    </section>
  )
}
```

- [ ] **Step 5: App.tsx를 최종 조립형으로 교체**

`src/app/App.tsx` 전체를 다음으로 교체:

```tsx
import { useMemo, useState } from 'react'
import { DevAuthPanel } from '../features/auth/DevAuthPanel'
import { useAuthStore } from '../features/auth/authStore'
import { SettingsSidebar } from '../features/auth/components/SettingsSidebar'
import { MessagePanel } from '../features/messages/MessagePanel'
import { NotificationTray } from '../features/notifications/NotificationTray'
import { useNotificationStore } from '../features/notifications/notificationStore'
import { RealtimeBridge } from '../features/realtime/RealtimeBridge'
import { RealtimeStatusPanel } from '../features/realtime/RealtimeStatusPanel'
import { RoomSidebar } from '../features/rooms/RoomSidebar'
import { ContactsMain } from '../features/rooms/components/ContactsMain'
import { ContactsSidebar } from '../features/rooms/components/ContactsSidebar'
import { useRoomStore } from '../features/rooms/roomStore'
import type { UUID } from '../shared/types/api'
import { SidebarFrame } from './layout/SidebarFrame'
import { WorkspaceRail, type AppTab } from './layout/WorkspaceRail'

function App() {
  const [activeTab, setActiveTab] = useState<AppTab>('messages')
  const accessToken = useAuthStore((state) => state.accessToken)
  const userId = useAuthStore((state) => state.userId)
  const rooms = useRoomStore((state) => state.rooms)
  const activeRoomId = useRoomStore((state) => state.activeRoomId)
  const selectRoom = useRoomStore((state) => state.selectRoom)
  const clearNotificationsForRoom = useNotificationStore((state) => state.clearNotificationsForRoom)
  const directRooms = useMemo(
    () => rooms.filter((room) => room.type === 'DIRECT' || room.type === 'GROUP'),
    [rooms],
  )
  const unreadTotal = rooms.reduce((total, room) => total + room.unreadCount, 0)

  const openRoom = (roomId: UUID) => {
    setActiveTab('messages')
    clearNotificationsForRoom(roomId)
    void selectRoom(roomId)
  }

  const renderSidebar = () => {
    if (activeTab === 'messages') {
      if (!accessToken) {
        return <p className="empty-text sb-empty">설정에서 로컬 토큰을 발급하세요.</p>
      }

      return <RoomSidebar />
    }

    if (activeTab === 'contacts') {
      return (
        <ContactsSidebar
          directRooms={directRooms}
          hasSession={Boolean(accessToken)}
          onOpenRoom={openRoom}
        />
      )
    }

    return <SettingsSidebar />
  }

  const renderMain = () => {
    if (activeTab === 'messages') {
      if (!accessToken) {
        return (
          <section className="main empty-main">
            <div className="empty-chat">
              <div className="av lg">#</div>
              <h2>메시지를 시작하려면 토큰이 필요합니다</h2>
              <p>왼쪽 레일의 설정에서 로컬 토큰을 발급하면 채팅방 목록이 표시됩니다.</p>
            </div>
          </section>
        )
      }

      return <MessagePanel activeRoomId={activeRoomId} currentUserId={userId} />
    }

    if (activeTab === 'contacts') {
      return <ContactsMain directRooms={directRooms} onOpenRoom={openRoom} />
    }

    return (
      <section className="main settings-main">
        <DevAuthPanel />
        <RealtimeStatusPanel />
      </section>
    )
  }

  return (
    <main className="app-shell" data-theme="light">
      <RealtimeBridge />
      <NotificationTray />
      <WorkspaceRail
        activeTab={activeTab}
        unreadTotal={unreadTotal}
        sessionReady={Boolean(accessToken)}
        onSelectTab={setActiveTab}
      />
      <SidebarFrame sessionReady={Boolean(accessToken)} onOpenSettings={() => setActiveTab('etc')}>
        {renderSidebar()}
      </SidebarFrame>
      {renderMain()}
    </main>
  )
}

export default App
```

- [ ] **Step 6: 검증**

```bash
npm run build && npm run lint
```
Expected: 성공 (`tenantId`/`roles`/`displayRole`/`BackendModeBadge`/`roomTitle` 관련 미사용 import가 App.tsx에 남아 있으면 여기서 에러)

- [ ] **Step 7: Commit**

```bash
git add -A && git commit -m "refactor: 탭 콘텐츠를 feature 컴포넌트로 추출

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 5: dead code 제거 — ChatWorkspace

**Files:**
- Delete: `src/features/chat/ChatWorkspace.tsx` (디렉토리째)

**Interfaces:**
- Consumes: 없음
- Produces: 없음

- [ ] **Step 1: 참조 없음 재확인**

```bash
grep -rn "ChatWorkspace" src/ | grep -v "features/chat/"
```
Expected: 출력 없음 (있으면 STOP — 계획 재검토)

- [ ] **Step 2: 삭제 및 검증**

```bash
git rm -r src/features/chat && npm run build && npm run lint
```
Expected: 성공

- [ ] **Step 3: Commit**

```bash
git commit -m "refactor: 미사용 ChatWorkspace 제거

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 6: tsconfig strict 활성화

**Files:**
- Modify: `local-frontend/tsconfig.app.json`

**Interfaces:**
- Consumes: 없음
- Produces: strict 컴파일 보장 (이후 모든 PR의 타입 안전선)

사전 검증됨: 2026-08-06 기준 `npx tsc -p tsconfig.app.json --strict --noEmit` 에러 0건 — 코드 수정 불필요.

- [ ] **Step 1: strict 플래그 추가**

`tsconfig.app.json`의 `compilerOptions`에서 `"skipLibCheck": true,` 다음 줄에 추가:

```json
    "strict": true,
```

- [ ] **Step 2: 검증**

```bash
npm run build && npm run lint
```
Expected: 성공. 에러가 나오면 타입 표기만으로 수정 (런타임 코드 변경 금지 — 로직 변경이 필요해 보이면 STOP 후 보고)

- [ ] **Step 3: Commit**

```bash
git add tsconfig.app.json && git commit -m "chore: tsconfig strict 모드 활성화

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 7: After 스크린샷 캡처 및 baseline 비교 (최종 검증)

**Files:** 없음 (검증만)

**Interfaces:**
- Consumes: Task 1의 `pr1-baseline/*.png`
- Produces: 통과/실패 판정 (실패 시 원인 파일 지목)

- [ ] **Step 1: after 스크린샷 캡처** (Task 1과 동일 조건 — 절차 전체 재기술)

1. `cd /home/jmchoi/project/pabal-dev-auth-onboarding/local-frontend && npm run dev` (background, `http://localhost:5173` 응답 확인)
2. Playwright: `browser_navigate` → `http://localhost:5173`, `browser_resize` → 1440x900
3. `browser_take_screenshot` → `pr1-after/01-no-token.png`
4. `button[title="설정"]` 클릭 → "User ID" input에 `11111111-1111-4111-8111-111111111111`, "Tenant ID" input에 `22222222-2222-4222-8222-222222222222` 입력 → "Issue local token" 클릭 → "Token ready" 대기 → `pr1-after/02-settings.png`
5. `button[title="메시지"]` 클릭 → 방 목록 로드 대기 → `pr1-after/03-messages.png`
6. `button[title="연락처"]` 클릭 → `pr1-after/04-contacts.png`
7. `browser_close` 후 dev server kill

- [ ] **Step 2: 4쌍 비교**

baseline과 after 이미지를 각각 열어 시각 비교 (Read 도구로 PNG 열람):
- `01-no-token`, `02-settings`, `03-messages`, `04-contacts` 각 쌍이 시각적으로 동일해야 통과
- **허용 차이**: 메시지·토큰 만료의 시간 표기 (mock seed가 실행 시각 기준 상대 시간)
- **불허 차이**: 레이아웃, 클래스 적용 결과(색/간격/폰트), 텍스트 내용, 요소 유무

- [ ] **Step 3: 결과 보고**

차이 발견 시: 해당 컴포넌트 추출 커밋을 지목해 수정 후 Step 1부터 재실행.
통과 시: PR1 구현 완료 — push는 하지 않고 사용자에게 보고 (front 브랜치 push 정책).
