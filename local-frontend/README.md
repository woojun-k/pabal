# Pabal Local Frontend

React + TypeScript + Vite chat frontend for local development.

## Scripts

```bash
npm run dev
npm run lint
npm run build
```

## Backend Mode

The app supports two backend modes.

- `mock`: default in Vite dev mode. Uses MSW for REST and an in-memory realtime client.
- `real`: uses the same-origin Vite proxy for REST and SockJS/STOMP for realtime.

Set the mode with:

```bash
VITE_BACKEND_MODE=mock npm run dev
VITE_BACKEND_MODE=real npm run dev
```

The committed default keeps request paths same-origin. Vite proxies `/api`, `/dev`,
`/actuator`, and `/websocket` to the local backend when real mode is used.

## Mock Coverage

Mock mode currently covers:

- `/dev/token`
- room list and direct/group/channel creation
- room join/leave/deletion schedule
- message list, send, reply, edit, delete, read-state, unread-count
- room events, typing events, and `/user/queue/chat.control`

Mock data is stored in memory and is reset when the browser session reloads.
