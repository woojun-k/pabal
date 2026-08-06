# Frontend Architecture Design — 구조·토큰·Tailwind 운영

- 날짜: 2026-08-06
- 상태: Approved (구현 전 설계)
- 범위: 디렉토리 구조, 컴포넌트 분류, 디자인 토큰 체계, Tailwind 도입·운영 규칙, 마이그레이션 로드맵
- 범위 외: 비주얼 리디자인(색/타이포 변경), 다크모드 값 정의, 단위 테스트 인프라(vitest), 라우팅 URL 스킴 상세(별도 스펙)

## 1. 배경

`local-frontend/`는 React 19 + TypeScript + Vite 8 + zustand 기반 채팅 클라이언트 프로토타입이다.
로직 레이어(store/api/realtime)는 feature별 co-location 컨벤션이 잡혀 있으나,
UI 레이어는 `App.tsx`(238줄)에 앱 셸 전체가 인라인이고 스타일은 전역 `App.css`(1,353줄, 204개 클래스) 하나에 몰려 있다.
스타일링 스택은 Tailwind 베이스로 결정되었다(2026-08-06). 디자인 시스템의 비주얼 방향은 미정이므로,
이 문서는 코드 구조와 토큰 체계까지만 고정하고 비주얼은 기존 값을 유지한다.

## 2. 결정 요약

| # | 결정 | 선택지 중 |
|---|---|---|
| D1 | 스펙 범위 = 구조 + 토큰 체계 (비주얼 리디자인 제외) | 구조만 / +토큰 / 풀 디자인 |
| D2 | Primitives = shadcn/ui 방식 copy-in, 기존 warm 토큰으로 재스킨 | copy-in / headless 직접 / 수제 |
| D3 | 분류 체계 = 2-계층 확장 (app/layout + features + shared/ui) | A / FSD-lite / Atomic |
| D4 | 토큰 = 기존 CSS 변수를 Tailwind v4 `@theme`로 1:1 승격, 값 불변 | — |
| D5 | 마이그레이션 = 컴포넌트화 → 라우팅 → Tailwind 기반 → 점진 전환 | — |

## 3. 디렉토리 구조와 분류 규칙

```
src/
├─ app/                    # 앱 셸 조립 + (PR2) 라우터 + 전역 프로바이더
│  ├─ App.tsx
│  └─ layout/              # WorkspaceRail, SidebarFrame, MeBar 등 셸 컴포넌트
├─ features/<domain>/      # 유지 — 도메인 수직 슬라이스 (컴포넌트 + store + api)
│  └─ components/          # feature 내부 컴포넌트가 3개 이상이 되면 하위 폴더로
├─ shared/
│  ├─ ui/                  # 신설 — shadcn copy-in + 수제 primitives
│  └─ api|realtime|security|types|utils|config|constants   # 기존 유지
└─ mocks/                  # 기존 유지
```

**배치 규칙 (3줄)**
1. 도메인 지식(방/메시지/인증 개념)을 아는 컴포넌트 → `features/<domain>/`
2. 도메인을 모르는 범용 UI → `shared/ui/`
3. 앱 셸(레일/사이드바 프레임/전역 오버레이) → `app/layout/`

**import 방향**: `app` → `features` → `shared` 단방향. `shared`는 `features`를 import하지 않는다.
(lint 강제는 도입하지 않고 리뷰로 지킨다 — 규모가 커지면 eslint-plugin-boundaries 재검토)

## 4. 토큰 체계 — Tailwind v4 `@theme` 승격

`index.css`의 `:root` 변수를 v4 네임스페이스로 prefix만 바꿔 승격한다. **값은 전부 그대로.**

| 현재 | 승격 후 | 생성되는 utility |
|---|---|---|
| `--bg` `--bg-sub` `--sidebar` `--rail` `--panel` `--surface` `--surface-muted` | `--color-bg` `--color-bg-sub` `--color-sidebar` `--color-rail` `--color-panel` `--color-surface` `--color-surface-muted` | `bg-sidebar`, `bg-rail` … |
| `--text` `--text-muted` `--text-faint` | `--color-text` `--color-text-muted` `--color-text-faint` | `text-text-muted` … |
| `--border` `--border-strong` | `--color-border` `--color-border-strong` | `border-border` … |
| `--accent` `--accent-strong` `--accent-ink` `--info` `--warning` `--danger` | `--color-accent` 등 | `bg-accent`, `text-danger` … |
| `--font-display` `--font-ui` `--font-mono` | 그대로 (이미 v4 `--font-*` 네임스페이스) | `font-display`, `font-ui`, `font-mono` |
| `--r-sm` `--r-md` `--r-lg` (App.css) | `--radius-sm` `--radius-md` `--radius-lg` | `rounded-sm/md/lg` |
| `--shadow` `--shadow-pop` | `--shadow-card` `--shadow-pop` | `shadow-card`, `shadow-pop` |

파생 변수(`--accent-weak`, `--accent-line`, `--bubble` 등 App.css 내 정의)는 해당 클래스가
Tailwind로 전환될 때 함께 정리한다(@theme 승격 또는 인라인 `color-mix` utility).

**Legacy 호환 alias**: 전환 기간 동안 아래 블록을 유지해 App.css의 기존 `var()` 참조를 보존한다.
App.css가 소멸하는 시점에 함께 삭제한다.

```css
:root {
  --bg: var(--color-bg);
  --text: var(--color-text);
  /* … 승격된 모든 변수의 구이름 alias … */
}
```

**다크모드 준비**: 토큰이 semantic 이름이므로 추후 `[data-theme="dark"]`에서 변수 재정의만 하면 된다.
`App.tsx`에 `data-theme="light"` 어트리뷰트가 이미 존재한다. 이번에는 다크 값을 정의하지 않는다.

## 5. Tailwind 운영 규칙

**셋업**: `tailwindcss` v4 + `@tailwindcss/vite` (PostCSS 설정 불필요) + `clsx` + `tailwind-merge`(`cn` 헬퍼) + `class-variance-authority`(cva) + `prettier` + `prettier-plugin-tailwindcss`.

**경계 규칙 (3줄)**
1. `App.css`는 freeze — 신규 클래스 추가 금지. 새 UI는 Tailwind로만 작성한다.
2. 컴포넌트를 Tailwind로 전환하면 대응 클래스를 App.css에서 즉시 삭제한다 (점진 소멸).
3. `shared/ui` primitives는 cva variant 패턴으로 작성하고, 색·radius·shadow는 토큰 utility만 사용한다
   (arbitrary value `bg-[#...]` 금지).

**Primitives 조달 (D2)**: shadcn/ui 컴포넌트를 copy-in 후 기존 warm 토큰으로 재스킨한다.
채팅 앱에 필요한 접근성 난이도 높은 부품(Dialog, DropdownMenu, Popover, Tooltip, ContextMenu, Toast)을
우선 대상으로 하고, 필요해지는 시점에 하나씩 들여온다 (선제 일괄 도입 금지).

## 6. 마이그레이션 로드맵

| PR | 브랜치 | 내용 | 성격 |
|---|---|---|---|
| PR1 | `refactor/frontend-componentization` | App.tsx 분해(`app/` + `app/layout/` 신설), dead code `features/chat/ChatWorkspace.tsx` 삭제, tsconfig strict 활성화. CSS 무변경 | 구조만, 동작·외관 불변 |
| PR2 | `feature/frontend-routing` | react-router 도입, URL을 tab/activeRoom의 source of truth로 승격. 라우트 정의는 `app/` | 동작 변경 |
| PR3 | `chore/tailwind-foundation` | Tailwind v4 셋업, 토큰 `@theme` 승격 + legacy alias, `cn`/`cva`, prettier, `shared/ui` 첫 primitives(Button, Input) | 기반 공사 |
| PR4+ | `refactor/tw-<영역>` | 영역 단위(rail → sidebar → messages …) Tailwind 전환 + App.css 클래스 삭제 | 점진 전환 |

각 PR은 저장소 PR 가이드의 "하나의 명확한 목적" 원칙을 따른다.
구조 변경(PR1)과 동작 변경(PR2)을 섞지 않는다.

## 7. 검증 전략

- **PR1 (불변 refactor)**: `tsc --noEmit` + `npm run build` + mock 모드 스크린샷 before/after 비교
  (Playwright — 토큰 발급 후 메시지/연락처/설정 3탭). 시각적으로 동일해야 통과.
- **PR2 이후 (동작 변경)**: 스크린샷 + 수동 시나리오(방 선택 → 메시지 전송 → 새로고침 후 URL·상태 유지).
- **단위 테스트**: 이번 범위 밖. vitest + RTL 도입은 별도 `chore/` PR로 로드맵에 기록만 한다.
  (`shared/utils/messageReconciliation.ts` 등이 우선 대상 후보)

## 8. 관련 트랙 (이 스펙과 독립)

- real 모드 백엔드 온보딩(B1~B3: dev tenant/user bootstrap, RBAC invite 권한, workspace 생성)은
  별도 feature 트랙이며 이 구조 작업과 순서 제약이 없다.
- front 브랜치는 dev와 히스토리가 발산되어 있다. 이 스펙의 작업은 전부 `local-frontend/` 내부로
  한정하여 향후 dev 포팅 시 충돌 면적을 만들지 않는다.
