# Frontend Routing Design — react-router 도입과 URL 승격 (PR2)

- 날짜: 2026-08-06
- 상태: Approved (구현 전 설계)
- 선행: [2026-08-06-frontend-architecture-design.md](2026-08-06-frontend-architecture-design.md) §6 로드맵의 PR2
- 범위: URL 스킴, 라우터 셋업, tab/activeRoom 상태의 URL 승격, 가드/히스토리 의미론, 검증 시나리오
- 범위 외: workspace 실계층 URL(하위 경로로 추후 확장), 라우트 코드 스플리팅, 스크롤 복원, 배포용 SPA fallback 설정(배포 인프라 트랙)

## 1. 배경

PR1 컴포넌트화 이후에도 화면 상태는 `App.tsx`의 `activeTab` useState와 roomStore의
`activeRoomId`로만 존재한다. URL은 항상 `/`이므로 새로고침 복원, 딥링크, 뒤로가기,
URL 공유가 불가능하다. 이 PR은 URL을 tab/activeRoom의 source of truth로 승격한다.

**백엔드 무접점**: 이 설계는 순수 클라이언트 사이드 라우팅이다. URL 경로는 History API로만
동작하며 서버로 전송되지 않는다. REST(`/api/v1/*`)·STOMP(`/websocket`) 계약 무변경.
라우트 prefix(`/client`, `/settings`)는 Vite dev proxy 대상(`/api`, `/dev`, `/actuator`,
`/websocket`)과 겹치지 않음을 확인했다. URL의 tenantId는 표시/내비게이션용이고 인가는
여전히 JWT가 결정한다.

## 2. 결정 요약

| # | 결정 | 근거 |
|---|---|---|
| R1 | URL 스킴 = Slack형 `/client/:tenantId/...`, `:ws` 자리에 **tenantId** | Pabal 도메인에서 Slack의 workspace에 해당하는 실체는 tenant이고, tenantId는 authStore에 실존하는 데이터 — 가짜 세그먼트 없이 Slack형 구조 구현 가능. 미래 workspace 계층은 하위 경로로 확장 |
| R2 | 동기화 모델 = **URL master + store mirror** | `applyRoomEvent`(unread 카운팅)가 store 내부에서 `activeRoomId`를 읽으므로 store에서 값 제거는 주입 통로만 늘림. mirror가 변경 면적 최소 |
| R3 | react-router v7 **declarative(library) 모드** (`BrowserRouter`) | 데이터 소유권이 zustand에 있어 data router(loader/action)는 이중 데이터 레이어. 신규 의존성은 `react-router` 1개 |
| R4 | 히스토리 = 사용자 이동 push / 정리·가드 replace | 아래 §6 표 참조. 뒤로가기 = 방 이력 탐색 (Slack 동일) |

## 3. 라우트 트리

```
/                           RootRedirect — 토큰 있으면 /client/:tenantId, 없으면 /settings (replace)
/settings                   설정 화면 (DevAuthPanel + RealtimeStatusPanel). 무토큰 접근 가능 —
                            토큰 발급 수단이 이 화면에 있으므로 가드 대상이 아니다
/client/:tenantId           ClientGuard 하위. messages 탭, 방 미선택
/client/:tenantId/contacts  연락처 탭 (정적 세그먼트가 :roomId 동적 매칭보다 우선)
/client/:tenantId/:roomId   방 선택됨 (roomId는 UUID — 'contacts'와 충돌 없음)
*                           → / redirect
```

라우트 정의는 `src/app/`에 위치한다 (선행 스펙 §3). `AppLayout`(layout route)이
`WorkspaceRail` + `SidebarFrame` + `<Outlet/>`(main)을 렌더한다.

## 4. 상태 승격

- **`activeTab` useState 제거** — 탭은 URL에서 파생. 경로↔탭 매핑은 `app/paths.ts`의
  `deriveTab`이 담당하고(`app/tabs.ts`에는 `AppTab` 타입만 있다), `WorkspaceRail.onSelectTab`은
  navigate 콜백으로 연결.
- **`activeRoomId`는 read-only mirror** — mirror를 쓰는 경로는 두 곳이다: RouteSync
  (→`selectRoom`, URL의 `:roomId`를 반영)와 `AppLayout`의 세션 변경 리셋
  (→`resetRooms`, `accessToken` 변경 시 `rooms`/`activeRoomId`/`hasLoadedRooms`를 초기화).
  **규칙: store의 `activeRoomId`를 직접 set하는 코드 금지, 이동은 navigate로만.** (리뷰 기준)

## 5. ClientGuard / RouteSync 시퀀스

`/client/*` 진입·URL 변경 시 순서대로:
1. 무토큰 → `/settings` replace
2. URL tenantId ≠ 토큰 tenantId → 토큰 tenant 경로로 replace
3. roomId 있으면: rooms 로딩 완료 대기(로딩 중엔 기존 로딩 UI) → 목록에 있으면
   `selectRoom(roomId)`(mirror 갱신 + 읽음처리), 없으면 `/client/:tenantId` replace
4. URL 변경마다 3 재실행. 방 상태 리셋은 RouteSync가 아니라 `AppLayout`의 세션 변경
   effect가 담당한다 — `accessToken`이 바뀔 때마다(로그인/로그아웃/테넌트 재발급 포함)
   `resetRooms()`를 호출해 `rooms`/`activeRoomId`/`hasLoadedRooms`를 전부 초기화한다.
   `RealtimeBridge`(자식 컴포넌트)의 `loadRooms()`보다 이 리셋이 먼저 반영되어야 하므로
   컴포넌트 트리 순서(자식 effect 우선 실행)에 의존한다 — 그렇지 않으면 세션 전환 직후
   낡은 tenant의 `rooms`로 새 URL의 `roomId`를 오인해 잘못된 요청이 나갈 수 있다

## 6. Selection 변경 지점 전환 매핑

| 동작 | 현재 (store 직접 set) | 전환 후 | 히스토리 |
|---|---|---|---|
| 방 클릭 (RoomSidebar/Contacts) | `selectRoom(id)` | `navigate(/client/:tid/:roomId)` | push |
| 탭 전환 (rail, me-bar ⚙) | `setActiveTab` | navigate | push |
| 방 생성 3종 성공 | `activeRoomId` set | 새 방으로 navigate | push |
| leave / delete | `activeRoomId: null` | `/client/:tid`로 navigate | replace |
| 가드 redirect (무토큰/tenant 불일치/404 방) | — | replace | replace |

store 함수들은 생성된 roomId를 반환만 하고(이미 반환함) 호출자가 navigate한다.

**현재 UI 호출처 현황**: `createDirectRoom`은 `RoomSidebar`에서 이미 성공 시
`onSelectRoom`(→navigate)으로 연결돼 있다. `createGroupRoom`·`createChannelRoom`·
`leaveActiveRoom`·`deleteActiveRoom`은 아직 호출하는 UI가 없다 — 향후 이 액션들을 호출하는
UI를 추가할 때는 반드시 위 표대로 성공 콜백에서 navigate로 이동해야 하며, store의
`activeRoomId`를 직접 set해서는 안 된다.

## 7. 검증 시나리오 (Playwright 수동 시나리오)

1. 방 선택 → 새로고침 → 같은 방 복원 (URL 유지)
2. 방 A → 방 B → 뒤로가기 → 방 A 복원
3. 무토큰 상태 `/client/<tid>/<roomId>` 딥링크 → `/settings` 착지
4. 토큰 발급 후 `/` 진입 → `/client/:tenantId` 착지
5. 존재하지 않는 roomId 딥링크 → `/client/:tenantId`로 정리 (replace)
6. leave 후 URL이 방 미선택 상태로 정리
7. 각 탭 화면은 PR1 baseline 스크린샷과 비주얼 동일 (라우팅은 외관 무변경)

## 8. 브랜치 / 의존

- 브랜치: `feature/frontend-routing` — PR1 브랜치(`refactor/frontend-componentization`,
  `18a663b`) 위에 스택. PR1이 front에 머지된 뒤 rebase 또는 base 변경으로 PR 생성.
- 신규 의존성: `react-router` v7 1개.
