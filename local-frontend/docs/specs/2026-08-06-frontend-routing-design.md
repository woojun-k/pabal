# Frontend Routing Design — react-router 도입과 URL 승격 (PR2)

- 날짜: 2026-08-06 (2026-08-12 개정 — R1 재평가, R5 추가)
- 상태: Approved
- 선행: [2026-08-06-frontend-architecture-design.md](2026-08-06-frontend-architecture-design.md) §6 로드맵의 PR2
- 범위: URL 스킴, 라우터 셋업, tab/activeRoom 상태의 URL 승격, 가드/히스토리 의미론, 검증 시나리오
- 범위 외: 라우트 코드 스플리팅, 스크롤 복원, 딥링크 공유 UI("링크 복사" — §10), 다중 워크스페이스 URL(§2.1)
- 배포 산출물에 포함할 것: SPA rewrite (§9) — 배포 경로가 생기는 시점의 작업이며 지금 할 일은 없다

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
| R1 | URL 스킴 = 평탄한 리소스 경로 `/rooms/:roomId` · `/contacts` · `/settings`. **테넌트 세그먼트 없음** | URL에는 사용자가 실제로 선택하는 것만 담는다. tenantId는 토큰이 정하는 상수라 URL에 넣으면 선택자가 아니라 **검증 대상**이 되고, 그 검증만을 위한 가드가 새로 필요해진다 (§2.1) |
| R2 | 동기화 모델 = **URL master + store mirror** | `applyRoomEvent`(unread 카운팅)가 store 내부에서 `activeRoomId`를 읽으므로 store에서 값 제거는 주입 통로만 늘림. mirror가 변경 면적 최소 |
| R3 | react-router v7 **declarative(library) 모드** | 데이터 소유권이 zustand에 있어 data router(loader/action)는 이중 데이터 레이어. 신규 의존성은 `react-router` 1개 |
| R4 | 히스토리 = 사용자 이동 push / 정리·가드 replace | 아래 §6 표 참조. 뒤로가기 = 방 이력 탐색 |
| R5 | 라우터 모드 = **history**(`BrowserRouter`), hash 아님 | 기능은 hash와 동일하다(둘 다 `pushState`/`popstate` 기반). 갈리는 것은 제품 방향이다 — `#`을 메시지 permalink 자리로 남겨야 하고, 같은 오리진에 색인 대상 공개 페이지가 붙을 수 있으며, `BrowserRouter`가 기여자에게 표준이다. 대가인 SPA rewrite는 배포 산출물에 포함해 self-host 사용자에게 전가하지 않는다 (§2.2, §9) |

### 2.1 R1 개정 기록 — 왜 테넌트 세그먼트를 쓰지 않는가

초판 R1은 `/client/:tenantId/:roomId`였다. 2026-08-12 재평가에서 근거가 성립하지 않아 뒤집었다.

- **URL로 테넌트를 고를 수가 없다.** 세션은 `tokenStorage` 하나이고 토큰당 tenant가 1개다. 다른
  tenant의 URL을 열어도 그 tenant로 갈 방법이 없어 리다이렉트될 뿐이다. 즉 이 세그먼트는 선택자가
  아니라 **선택지가 하나뿐인 값의 표시**였다. 계층형 URL은 그 자리에 실제 선택지가 여럿일 때 값을
  하는 구조이고, Pabal에는 그 조건이 없다.
- **세그먼트가 자기 자신을 지키는 가드를 낳았다.** 초판 구현에서 `params.tenantId`의 유일한 독자는
  `ClientGuard`의 "URL tenantId ≠ 토큰 tenantId → redirect" 분기 하나였다. 실제 동작(STOMP
  destination, REST 호출, 인가)은 전부 `authStore.tenantId`를 직접 읽는다. 즉 세그먼트는 라우팅
  결정에 참여하지 않으면서, 자신이 만든 불변식을 지킬 가드만 추가했다. 세그먼트를 빼면 불변식도
  가드 분기도 함께 사라진다.
- **미래 대비도 되지 못한다.** 다중 워크스페이스가 도입되면 선택자는 `workspaceId`이고 tenantId는
  그때도 토큰이 정하는 상수다. 그 시점에 `/w/:workspaceId/...`를 새로 얹는 것이 맞고, 지금의
  tenantId 세그먼트는 그 설계에 재사용되지 않는다.
- **UUID 노출은 판단 근거가 아니다.** 인가는 JWT가 결정하고 백엔드가 tenant 스코프를 강제하므로
  URL의 UUID는 권한 없는 식별자다. 제거 근거는 보안이 아니라 "라우팅에 기여하지 않는다"는 것이다.

### 2.2 R5 — 왜 hash가 아닌가

`BrowserRouter`(history) 대신 `HashRouter`를 쓰면 URL이 `/#/rooms/:roomId`가 되고 배포 시
SPA fallback 설정이 아예 필요 없어진다. 그 이점을 알면서도 history를 택했다.

**먼저 기능은 둘이 같다.** react-router의 `createHashHistory`는 `createBrowserHistory`와 동일한
엔진(`getUrlBasedHistory`)에 위임하며 `pushState`/`replaceState` + `popstate`를 그대로 쓴다
(`hashchange`를 쓰지 않는다). 따라서 새로고침 복원, 뒤로/앞으로, 딥링크, 멀티탭, 북마크,
그리고 이 스펙이 정의한 push/replace 의미론(R4)이 hash에서도 동일하게 성립한다. 차이는
URL 모양과 `#` 뒤가 서버로 전송되지 않는다는 점뿐이다. 판단 근거는 기능 차이가 아니라
제품이 갈 방향이다.

- **`#`은 메시지 permalink가 쓸 자리다.** 특정 메시지로 가는 링크는 `/rooms/:roomId#message-<id>`가
  자연스러운 형태이고 §10에 후속 과제로 잡혀 있다. hash 라우팅은 그 자리를 라우트가 차지한다.
  react-router는 `#/rooms/abc#message-1`을 파싱할 수 있지만(두 번째 `#`부터 fragment로 읽음)
  브라우저가 보는 fragment는 `/rooms/abc#message-1` 전체라 네이티브 앵커 동작이 사라지고,
  링크를 받는 쪽 파서가 두 번째 `#`에서 절단할 위험도 생긴다.
- **공개 페이지가 같은 오리진에 붙을 가능성이 있다.** 오픈소스 업무용 메신저는 소개·가입·문서
  페이지가 함께 서빙되는 경우가 흔하다. 그 페이지들은 색인되어야 하는데 hash 뒤 경로는 색인되지
  않는다. 나중에 붙이려면 라우팅 구조를 갈라야 한다.
- **`BrowserRouter`가 생태계 기본값이다.** 오픈소스라 새 기여자의 진입 비용이 실제 비용이며,
  표준 형태면 별도 설명이 필요 없다.
- **서버 설정이 self-host 사용자에게 전가되지 않는다.** 배포를 Docker 이미지로 제공하면 SPA rewrite를
  이미지 안에 넣어두면 되고 설치하는 쪽은 아무것도 하지 않는다 (§9).

**Tauri 로드맵은 이 결정의 근거가 아니다.** Tauri v2의 딥링크는 웹뷰가 해당 URL로 이동하는 방식이
아니라 `onOpenUrl` 이벤트(앱 시작 시 `getCurrent()`)로 문자열이 전달되고, 그것을 파싱해
`navigate()`를 호출하는 구조다. 라우터 모드와 무관하다.

전환이 필요해지면 `App.tsx` import 한 줄이고 `paths.ts`는 무변경이다. 다만 위 근거들 때문에
**전환할 이유가 생기려면 제품 방향이 바뀌어야 한다.**

## 3. 라우트 트리

```
/                  RootRedirect — 세션 있으면 /rooms, 없으면 /settings (replace)
/settings          설정 화면 (DevAuthPanel + RealtimeStatusPanel). 무세션 접근 가능 —
                   토큰 발급 수단이 이 화면에 있으므로 가드 대상이 아니다
                   ─── 아래 세 경로는 SessionGuard 하위 ───
/rooms             messages 탭, 방 미선택
/rooms/:roomId     방 선택됨
/contacts          연락처 탭
*                  → / redirect
```

`/contacts`와 `/rooms/:roomId`는 서로 다른 최상위 경로이므로, 초판에 있던 "정적 세그먼트가
동적 `:roomId`보다 먼저 매칭돼야 한다"는 암묵적 제약이 없다.

라우트 정의는 `src/app/`에 위치한다 (선행 스펙 §3). `AppLayout`(layout route)이
`WorkspaceRail` + `SidebarFrame` + `<Outlet/>`(main)을 렌더한다. `SessionGuard`는 경로 없는
layout route로 세 경로를 감싼다.

## 4. 상태 승격

- **`activeTab` useState 제거** — 탭은 URL에서 파생. 경로↔탭 매핑은 `app/paths.ts`의
  `deriveTab`이 담당하고(`app/tabs.ts`에는 `AppTab` 타입만 있다), `WorkspaceRail.onSelectTab`은
  navigate 콜백으로 연결.
- **`activeRoomId`는 read-only mirror** — mirror를 쓰는 경로는 두 곳이다: RouteSync
  (→`selectRoom`, URL의 `:roomId`를 반영)와 `AppLayout`의 세션 변경 리셋
  (→`resetRooms`, `accessToken` 변경 시 `rooms`/`activeRoomId`/`hasLoadedRooms`를 초기화).
  **규칙: store의 `activeRoomId`를 직접 set하는 코드 금지, 이동은 navigate로만.** (리뷰 기준)

## 5. SessionGuard / RouteSync 시퀀스

`/rooms`·`/contacts` 진입·URL 변경 시 순서대로:
1. 무세션(`accessToken` 없음) → `/settings` replace
2. roomId 있으면: rooms 로딩 완료 대기(로딩 중엔 기존 로딩 UI) → 목록에 있으면
   `selectRoom(roomId)`(mirror 갱신 + 읽음처리), 없으면 `/rooms` replace
3. URL 변경마다 2 재실행. 방 상태 리셋은 RouteSync가 아니라 `AppLayout`의 세션 변경
   effect가 담당한다 — `accessToken`이 바뀔 때마다(로그인/로그아웃/테넌트 재발급 포함)
   `resetRooms()`를 호출해 `rooms`/`activeRoomId`/`hasLoadedRooms`를 전부 초기화한다.
   `RealtimeBridge`(자식 컴포넌트)의 `loadRooms()`보다 이 리셋이 먼저 반영되어야 하므로
   컴포넌트 트리 순서(자식 effect 우선 실행)에 의존한다 — 그렇지 않으면 세션 전환 직후
   낡은 tenant의 `rooms`로 새 URL의 `roomId`를 오인해 잘못된 요청이 나갈 수 있다

**테넌트 전환은 전용 가드 없이 2단계가 흡수한다.** 다른 tenant로 재발급하면 위 리셋으로 `rooms`가
비고, 새 목록을 받은 뒤 옛 `roomId`가 목록에 없으므로 `/rooms`로 정리된다. 초판의 "URL tenantId ≠
토큰 tenantId → redirect" 분기와 결과가 같고 메커니즘은 하나 줄어든다 (§2.1).

## 6. Selection 변경 지점 전환 매핑

| 동작 | 현재 (store 직접 set) | 전환 후 | 히스토리 |
|---|---|---|---|
| 방 클릭 (RoomSidebar/Contacts) | `selectRoom(id)` | `navigate(/rooms/:roomId)` | push |
| 탭 전환 (rail, me-bar ⚙) | `setActiveTab` | navigate | push |
| 방 생성 3종 성공 | `activeRoomId` set | 새 방으로 navigate | push |
| leave / delete | `activeRoomId: null` | `/rooms`로 navigate | replace |
| 가드 redirect (무세션/목록에 없는 방) | — | replace | replace |

store 함수들은 생성된 roomId를 반환만 하고(이미 반환함) 호출자가 navigate한다.

**현재 UI 호출처 현황**: `createDirectRoom`은 `RoomSidebar`에서 이미 성공 시
`onSelectRoom`(→navigate)으로 연결돼 있다. `createGroupRoom`·`createChannelRoom`·
`leaveActiveRoom`·`deleteActiveRoom`은 아직 호출하는 UI가 없다 — 향후 이 액션들을 호출하는
UI를 추가할 때는 반드시 위 표대로 성공 콜백에서 navigate로 이동해야 하며, store의
`activeRoomId`를 직접 set해서는 안 된다.

## 7. 검증 시나리오 (Playwright 수동 시나리오)

1. 무세션 상태 `/rooms/<roomId>` 딥링크 → `/settings` 착지
2. 토큰 발급 후 `/` 진입 → `/rooms` 착지
3. 방 선택 → 새로고침 → 같은 방 복원 (URL 유지)
4. 방 A → 방 B → 뒤로가기 → 방 A 복원
5. 존재하지 않는 roomId 딥링크 → `/rooms`로 정리 (replace — 뒤로가기가 그 URL로 돌아가지 않음)
6. 다른 tenant로 재발급 → 옛 방 URL이 `/rooms`로 정리 (§5 전용 가드 없이 흡수되는지 확인)
7. 각 탭 화면은 PR1 baseline 스크린샷과 비주얼 동일 (라우팅은 외관 무변경)

`leaveActiveRoom`/`deleteActiveRoom`은 호출 UI가 아직 없어 시나리오에서 제외한다 (§6 참조).

## 8. 브랜치 / 의존

- 브랜치: `feature/frontend-routing` — PR1 브랜치(`refactor/frontend-componentization`,
  `18a663b`) 위에 스택. PR1이 front에 머지된 뒤 rebase 또는 base 변경으로 PR 생성.
- 신규 의존성: `react-router` v7 1개.

## 9. 배포 산출물에 포함할 것 — SPA rewrite

**현재 저장소에는 프론트 배포 경로가 없다** (CI/CD·호스팅 설정 없음. `origin/dev` 기준으로도 동일).
아래는 배포 트랙 착수 시점에 챙길 항목이며 지금 수행할 것이 아니다.

history 라우팅(R5)이라 `/rooms/<id>`로 직접 들어오는 요청은 호스팅이 `index.html`을 돌려줘야 한다.
dev에서는 Vite dev server가 자동 처리하므로 드러나지 않는다.

**SPA rewrite를 배포 산출물에 포함한다.** 오픈소스 self-host 사용자가 별도 설정을 하지 않아도
되도록, 빌드된 앱과 rewrite 규칙을 함께 패키징하는 것을 기본으로 한다(예: nginx를 포함한
Docker 이미지). 호스팅 유형별 조치는 아래와 같다.

| 호스팅 | 조치 |
|---|---|
| 정적 호스팅 (S3/CDN 등) | 404 응답을 `/index.html`(200)로 rewrite |
| Vercel/Netlify류 | SPA rewrite 규칙 1줄 |
| 리버스 프록시 | 미매칭 경로를 `index.html`로 (예: nginx `try_files $uri $uri/ /index.html`) |

`HashRouter`로 교체하면 이 설정 자체가 필요 없어지지만, §2.2의 이유로 채택하지 않는다.

**백엔드 코드·설정은 어느 경우에도 무변경이다.** 프론트는 별도 Vite 앱이고 백엔드는 API만 담당한다
(`origin/dev` 확인: `resources/static`·`WebMvcConfigurer`·`ErrorController` 모두 없고 Gradle에
프론트 빌드 통합도 없다).

> **"백엔드가 프론트를 함께 서빙" 토폴로지는 권장하지 않는다.** forward 설정만으로는 부족하다 —
> `SecurityConfig`가 `anyRequest().authenticated()`(default deny)라 `/`·`/assets/*`·`/rooms/*`가
> 전부 401이 된다. 정적 자산과 SPA 라우트를 `permitAll`에 추가해야 하는데 이는 보안 경계를 뒤집는
> 변경이라 독립 리뷰 대상이다. 프론트 산출물을 jar에 넣는 Gradle 통합도 별도로 필요하다(현재 없음).

## 10. 후속 — 딥링크 공유("링크 복사") 미제공

현재 Pabal에는 "링크 복사" 같은 **공유 전용 진입점이 없다** — 사용자가 대화 링크를 공유하려면
주소창을 직접 복사해야 한다. 주소창 URL이 곧 공유 링크인 셈이라, 경로 구조를 바꾸면 이전에 공유된
링크가 깨진다는 뜻이기도 하다(현재는 공유된 링크가 없어 문제가 되지 않는다).

이 PR 범위 밖이며, 다음 조건이 갖춰질 때 별도로 다룬다:
- 메시지 단위 permalink가 필요해지는 시점 (`/rooms/:roomId/:messageId` 확장)
- Tauri 패키징 시점 — 데스크톱에는 주소창이 없으므로 "링크 복사" 액션이 **필수**가 된다
  (딥링크 수신은 `onOpenUrl` 이벤트로 처리하므로 라우터 모드와 무관, §2.2)
