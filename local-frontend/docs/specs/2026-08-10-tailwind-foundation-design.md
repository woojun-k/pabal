# Tailwind Foundation Design — 셋업·토큰 승격·첫 primitive (PR3)

- 날짜: 2026-08-10
- 상태: Approved (구현 전 설계)
- 선행: [2026-08-06-frontend-architecture-design.md](2026-08-06-frontend-architecture-design.md) §4·§5·§6 로드맵의 PR3
- 범위: Tailwind v4 셋업, 디자인 토큰 `@theme` 승격, `cn`/`cva` 헬퍼, `shared/ui` 첫 primitive(Button)와 그 첫 소비처 전환
- 범위 외: 나머지 영역의 Tailwind 전환(PR4+), 비주얼 리디자인, 다크모드 값, preflight 도입, prettier 도입

## 1. 배경

선행 스펙이 스타일링 스택을 Tailwind v4로 정했지만 아직 코드에는 없다. 스타일은 전역
`App.css`(1,353줄, 204개 클래스)와 `index.css`의 CSS 변수 23개에 있다. 이 PR은 Tailwind를
동작 가능한 상태로 들여오고, 토큰을 `@theme`로 승격하며, 패턴을 증명하는 primitive 하나를
실제 소비처와 함께 넣는다. **외관은 변하지 않는다.**

## 2. 결정 요약

| # | 결정 | 근거 |
|---|---|---|
| T1 | **preflight 제외** — `theme.css`/`utilities.css` 레이어만 import | preflight는 `button` 배경·테두리, heading, list 기본값을 전역으로 리셋해 App.css가 의존하는 브라우저 기본값을 흔든다. 아무것도 전환하지 않아도 외관이 바뀐다. `index.css`에 이미 자체 reset(box-sizing, margin, `font: inherit`)이 있어 공백이 없다 |
| T2 | 토큰 = `index.css`의 23개 전부 `@theme` 승격 (미사용 3개 포함), 값 불변 | 값 불변 원칙을 기계적으로 지켜 diff를 단순하게. `--panel`·`--info`·`--accent`는 semantic 슬롯이라 PR4+ 전환 중 쓰일 여지가 있다 |
| T3 | `.app-shell` 스코프 파생 변수 중 **radius 3개만** 승격 | `--accent-weak`/`--accent-line`/`--bubble`은 `color-mix` 기반이라 해당 클래스를 전환할 때 함께 정리하는 편이 맞다 |
| T4 | Button 도입 시 **`.button-row` 안의 버튼 4개를 한 번에** 전환 | `.button-row button`은 자손 선택자(특이성 0,1,1)라 Tailwind utility(0,1,0)를 이긴다. 하나만 바꾸면 Tailwind가 적용된 것처럼 보여도 실제로는 App.css가 이겨 검증이 무의미해진다 |
| T5 | 색·radius·shadow는 토큰 utility만, **spacing은 arbitrary 허용** | 기존 padding(9px·10px·14px)이 Tailwind 4px 스케일에 안 맞는다. 픽셀 동일이 우선이며 스케일 정규화는 PR4+ 판단 |

## 3. Tailwind 셋업

- 의존성: `tailwindcss` v4, `@tailwindcss/vite`, `clsx`, `tailwind-merge`, `class-variance-authority`
  (prettier·prettier-plugin-tailwindcss는 이번 범위 밖 — 선행 스펙 §5의 셋업 목록에서 보류)
- `vite.config.ts`: `plugins` 배열에 `tailwindcss()` 추가. 기존 `define`·`server.proxy` 설정은 손대지 않는다
- `index.css` 최상단 (T1). `@theme` 블록은 이 import들 **뒤**, 기존 `:root` 블록 **앞**에 온다:
  ```css
  @import 'tailwindcss/theme.css' layer(theme);
  @import 'tailwindcss/utilities.css' layer(utilities);
  ```
  **선행 확인**: 이 선택적 레이어 import는 전체 `@import 'tailwindcss'`보다 덜 다니는 길이다.
  구현 첫 단계에서 (a) preflight가 실제로 빠졌는지(예: `button` 배경이 리셋되지 않았는지),
  (b) `@theme` 블록이 실제로 utility를 만들어내는지를 먼저 확인하고 나머지를 쌓는다.
  둘 중 하나라도 실패하면 멈추고 보고한다 — 대안(전체 import 후 preflight 레이어만 배제)은
  외관 리스크가 달라지므로 사람이 판단할 사안이다
- `src/shared/utils/cn.ts` 신설 — `clsx` + `tailwind-merge` 결합 헬퍼. `shared/utils/`의 기존
  파일들과 같은 위치(도메인을 모르는 순수 유틸)

## 4. 토큰 승격

`index.css`의 `:root` 변수를 `@theme` 블록으로 옮기되 **값은 한 글자도 바꾸지 않는다.**

| 원본 | 승격 후 | 생성 utility |
|---|---|---|
| `--bg` `--bg-sub` `--sidebar` `--rail` `--panel` `--surface` `--surface-muted` | `--color-*` 동명 | `bg-sidebar`, `bg-rail` … |
| `--text` `--text-muted` `--text-faint` | `--color-text*` | `text-text-muted` … |
| `--border` `--border-strong` | `--color-border*` | `border-border` … |
| `--accent` `--accent-strong` `--accent-ink` `--info` `--warning` `--danger` | `--color-*` 동명 | `bg-accent-strong`, `text-danger` … |
| `--font-display` `--font-ui` `--font-mono` | 그대로 (이미 v4 네임스페이스) | `font-display` … |
| `--shadow` `--shadow-pop` | `--shadow-card` `--shadow-pop` | `shadow-card`, `shadow-pop` |
| `--r-sm` `--r-md` `--r-lg` (App.css `.app-shell`) | `--radius-sm` `--radius-md` `--radius-lg` | `rounded-sm/md/lg` |

radius 승격은 Tailwind의 **기본 radius 스케일을 덮어쓴다**(v4 기본 `--radius-sm`은 다른 값이다).
아직 Tailwind를 쓰는 코드가 없으므로 영향은 없고, 이 프로젝트의 `rounded-sm`이 곧 7px을
의미하게 되는 것이 의도다.

**Legacy alias**: 승격된 모든 토큰의 구이름을 `:root`에서 `var(--color-*)`로 alias 해 App.css의
기존 `var()` 참조를 전부 보존한다. radius 3개는 `.app-shell` 블록에 원래 위치대로 alias를 남긴다
(`--r-sm: var(--radius-sm)` 등) — App.css가 `.app-shell` 스코프에서 참조하기 때문이다.
alias 블록은 App.css가 소멸할 때 함께 삭제한다.

**남기는 것**: `--accent-weak`, `--accent-line`, `--bubble`(App.css `.app-shell` 내 `color-mix`)과
`--av`(`var(--av, var(--accent-strong))` 형태의 per-instance override 슬롯)는 이번에 건드리지 않는다.

## 5. Button primitive와 첫 전환

**`src/shared/ui/Button.tsx`** — cva 기반. 기존 CSS 조합을 그대로 반영한다:

| variant / size | 대응 CSS | 클래스 |
|---|---|---|
| `primary` (기본) | `.button-row button` | `border-0 rounded-sm px-3.5 py-2.5 font-bold cursor-pointer text-accent-ink bg-accent-strong` |
| `ghost` | `.button-row button.secondary` | `border-0 rounded-sm px-3 py-[9px] font-bold cursor-pointer text-text-muted bg-transparent hover:text-text hover:bg-surface` |
| `size: compact` | `.button-row button.compact` | `px-2.5 py-2 text-[13px]` (padding override) |

`disabled` 상태는 기존에 별도 CSS가 없으므로 스타일을 추가하지 않는다 (브라우저 기본값 유지).

**전환 대상 4개** (모두 `/settings` 한 화면에 있다):
- `features/auth/DevAuthPanel.tsx` — submit(`primary`), Clear(`ghost`)
- `features/realtime/RealtimeStatusPanel.tsx` — connect·disconnect(`ghost` + `compact`) 2개

**App.css 정리**: 콤마 선택자 목록에서 `.button-row button`, `.button-row button.secondary`,
`.button-row button.compact`, `.button-row button.secondary:hover` 만 제거하고
`.btn-primary`, `.btn-ghost`, `.ghost-button`, `button.compact`, `.btn-ghost.compact`는 남긴다
(다른 컴포넌트가 사용 중). `.button-row`/`.compact-row` 컨테이너 규칙도 그대로 둔다.

`shared/ui`는 도메인을 모른다 — Button은 `features/`를 import하지 않는다 (선행 스펙 §3 배치 규칙).

## 6. 검증

- **`/settings` before/after 스크린샷** — 전환된 버튼 4개가 전부 이 화면에 있다. 픽셀 동일해야 통과
  (허용 차이: 토큰 만료 시각 텍스트)
- **나머지 3개 화면**(메시지 방 미선택·방 선택·연락처) before/after — preflight 미도입으로 기존
  스타일이 흔들리지 않았음을 증명
- **빌드 산출 CSS 확인** — Tailwind가 실제로 동작했는지: 산출 CSS에 `bg-accent-strong` 등
  전환된 클래스의 정의가 존재하고, 그 값이 토큰을 참조하는지
- `npm run build`(tsc -b, strict) + `npm run lint`
- baseline은 PR2 검증 세션이 캡처해 둔 `.claude/artifacts/pr2b-after/` 3장(1440×900, mock 모드)을
  재사용한다 (PR1 baseline은 라우팅 이전 URL 구조라 재사용 불가). 방 선택 화면은 pr2b-after에
  없으므로 before/after를 같은 세션에서 신규 캡처한다

## 7. 브랜치 / 의존

- 브랜치: `chore/tailwind-foundation` — PR2 브랜치(`feature/frontend-routing`) 위에 스택.
  PR #5 → #6 → 이 PR 순으로 머지된다. 파일 충돌 면적은 `package.json`/`package-lock.json` 뿐이다
  (PR1·PR2가 만진 `app/`·`features/` 파일 중 이번에 겹치는 것은 버튼 2개를 쓰는
  `DevAuthPanel.tsx`·`RealtimeStatusPanel.tsx`이며, PR1·PR2는 두 파일의 버튼 부분을 건드리지 않았다)
- 신규 의존성 5개: `tailwindcss`, `@tailwindcss/vite`, `clsx`, `tailwind-merge`,
  `class-variance-authority`

## 8. 후속 (이 스펙 범위 밖)

- PR4+: 영역 단위 Tailwind 전환(rail → sidebar → messages …)과 App.css 클래스 점진 삭제
- App.css 소멸 시점: preflight 도입, legacy alias 블록 삭제, `--accent-weak`/`--accent-line`/`--bubble` 정리
- prettier + prettier-plugin-tailwindcss 도입은 별도 chore
