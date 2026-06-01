# Frontend TODO

Working plan for the Pabal chat frontend on the `front` branch.

## Phase 1: Auth, Security, Common

- [x] Create React + TypeScript + Vite frontend workspace.
- [x] Keep frontend work isolated on the `front` branch until push/PR is explicitly approved.
- [x] Add local env config for REST and WebSocket base URLs.
- [x] Add shared API contract constants.
- [x] Add shared API path builders.
- [x] Add shared API client with bearer token injection.
- [x] Add normalized API error handling.
- [x] Add API error classification helpers.
- [x] Add auth store for local dev token flow.
- [x] Add JWT decode/expiration helpers.
- [x] Add role normalization and role option helpers.
- [x] Add local token storage wrapper.
- [x] Add local session/header helpers.
- [x] Add common API and realtime DTO types from `local-docs/api-chat.md`.
- [x] Add UUID and date-time utilities.
- [x] Add realtime event guards and parsers.
- [x] Add message reconciliation helpers.
- [x] Add cursor pagination helpers.
- [x] Add minimal dev login/status UI.
- [x] Add mock backend mode with MSW and in-memory fixtures.

## Phase 2: Rooms

- [x] Add room API module.
- [x] Add room store.
- [x] Render room list.
- [x] Add direct room creation with known participant UUID.
- [x] Add active room selection.
- [ ] Add group/channel creation UI.

## Phase 3: Messages

- [x] Add message API module.
- [x] Add message store keyed by room ID.
- [x] Render paged message history.
- [x] Send message with generated `clientMessageId`.
- [x] Add optimistic message state.
- [x] Support edit, delete, and mark-read.
- [ ] Add reply UI.
- [ ] Add paged "load older" UI.

## Phase 4: Realtime

- [x] Add STOMP client wrapper.
- [x] Connect with bearer token headers.
- [x] Add subscribe/unsubscribe management.
- [x] Add reconnect policy.
- [x] Subscribe to room events.
- [x] Subscribe to typing events.
- [x] Add `/user/queue/chat.control` subscription helper.
- [x] Add realtime connection state store.
- [x] Merge realtime events into room/message stores.
- [x] Handle `/user/queue/chat.control` revocation events in app state.
- [x] Add mock realtime client for backend-independent UI development.

## Phase 5: App Shell

- [ ] Build responsive two-pane chat layout.
- [ ] Add Android-width layout behavior.
- [ ] Add loading, empty, and error states.
- [ ] Add Tauri packaging only after web MVP is usable.

## Known Backend Gaps

- User directory/search is not available yet.
- Profile/avatar API is not available yet.
- Workspace/channel discovery is not available yet.
- Production login flow is not part of the current local API.

## Branch / Commit Policy

- Current frontend work lives on the local `front` branch.
- `local-frontend/` is tracked normally so frontend changes can be committed.
- `.env.local`, `dist/`, and `node_modules/` remain ignored by `local-frontend/.gitignore`.
- Push and PR creation require explicit approval.

## Backend Contract TODO

### Auth / Current User

- [ ] Add a production login/session contract.
- [ ] Add `GET /api/v1/me` or equivalent current-user endpoint.
- [ ] Response should include `userId`, `tenantId`, display name, avatar URL, roles, and effective permissions.
- [ ] Define token refresh behavior for web and future Tauri clients.
- [ ] Define 401/403 error response shape for expired token vs insufficient permission.

### Users / Profiles

- [ ] Add user directory/search API for starting direct rooms.
- [ ] Add profile summary response for message sender display.
- [ ] Add batch user lookup API by IDs so message lists can render names/avatars efficiently.
- [ ] Define avatar fallback behavior if no image exists.
- [ ] Define whether cross-tenant user lookup is forbidden or impossible by query scope.

### Workspace / Channel Discovery

- [ ] Add workspace list API.
- [ ] Add workspace detail API.
- [ ] Add channel list/search API by workspace.
- [ ] Add channel joinability metadata, such as public/private, alreadyJoined, memberCount.
- [ ] Add channel creation response fields enough for UI insertion without full refetch.

### Room Admin / Member Role

- [ ] Decide whether room authority should be modeled as room member roles or computed permission flags.
- [ ] If using room member roles, add backend role values such as `OWNER`, `ADMIN`, and `MEMBER`.
- [ ] If using computed flags, add fields to room/member responses such as `canInvite`, `canManageMembers`, `canDelete`, `canPost`, `canEditOwnMessage`, and `canDeleteAnyMessage`.
- [ ] Add an API for room member listing with role/permission information.
- [ ] Add APIs for inviting/removing members.
- [ ] Add APIs for promoting/demoting room members if room-level roles are chosen.
- [ ] Define owner transfer behavior before owner leaves or deletes a room.
- [ ] Define whether direct rooms can have admins or only participants.
- [ ] Add frontend admin/member badges only after backend response contracts expose role or permission flags.
- [ ] Hide or disable privileged frontend actions from backend-provided permission flags, not from client-only assumptions.

### Room Responses

- [ ] Add `createdBy` to room responses if creator-specific permissions remain relevant.
- [ ] Add `memberCount` to room list responses.
- [ ] Add `myMemberState` or equivalent, including `joinedAt`, `lastReadMessageId`, and role/permission flags.
- [ ] Add `lastMessagePreview` if room list should avoid fetching messages for preview.
- [ ] Define stable enum values for room `type` and `status`.
- [ ] Define room name rules for direct/group/channel rooms.

### Messages

- [ ] Add sender profile summary or sender display-name lookup strategy.
- [ ] Add message type contract beyond plain text if files/images/system messages are planned.
- [ ] Add attachment upload/download contract.
- [ ] Add reply UI support in API response by including enough parent message preview data.
- [ ] Define edit/delete permission response behavior.
- [ ] Define whether deleted messages return tombstone content or no content.
- [ ] Add reaction API if emoji reactions are planned.
- [ ] Add pinned message API if needed.

### Realtime / Notifications

- [ ] Add user-level notification stream if all-room notifications should work without subscribing to every room.
- [ ] Define whether clients should subscribe to all rooms or only active rooms.
- [ ] Add realtime event for room list changes, such as room created, invited, removed, deleted.
- [ ] Add realtime event for member role changes.
- [ ] Add event payload discriminators that allow TypeScript narrowing without client-side casts.
- [ ] Define missed-event recovery strategy after reconnect, such as cursor-based sync.
- [ ] Define heartbeat/reconnect expectations for web and Tauri clients.

### Pagination / Sync

- [ ] Define message pagination direction and cursor semantics precisely.
- [ ] Add "load newer since sequence" or sync endpoint for reconnect recovery.
- [ ] Define consistency rules between REST message send response and STOMP room event.
- [ ] Define idempotency behavior for `clientMessageId` across retries and app restarts.

### Errors

- [ ] Document common error envelope shape.
- [ ] Document validation error shape per field.
- [ ] Document domain error codes that frontend should branch on.
- [ ] Document retryable vs non-retryable error categories.

### Local Dev Support

- [ ] Add stable seed data or a dev endpoint for creating test users/workspaces.
- [ ] Add local dev endpoint to generate a matched set of tenant/user/workspace IDs.
- [ ] Add example scenarios for member, workspace admin, tenant admin, and channel owner.

Current local-only workaround:

- [x] Allow local dev JWT issuance with test roles.
- [x] Use backend-supported JWT roles for local permission scenario testing.
