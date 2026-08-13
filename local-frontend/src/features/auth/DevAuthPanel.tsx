import { useState } from 'react'
import type { FormEvent } from 'react'
import { BackendModeBadge } from '../../shared/config/BackendModeBadge'
import { env } from '../../shared/config/env'
import { displayRole, roleOptions } from '../../shared/security/roles'
import { getJwtExpiration, isJwtExpired } from '../../shared/security/jwt'
import { Button } from '../../shared/ui/Button'
import { createUuid, isUuid } from '../../shared/utils/uuid'
import { formatDateTime } from '../../shared/utils/dateTime'
import { useAuthStore } from './authStore'

export function DevAuthPanel() {
  const {
    accessToken,
    userId: activeUserId,
    tenantId: activeTenantId,
    roles,
    isIssuingToken,
    error,
    issueDevToken,
    clearSession,
  } = useAuthStore()
  const [userId, setUserId] = useState(activeUserId ?? createUuid())
  const [tenantId, setTenantId] = useState(activeTenantId ?? createUuid())
  const [role, setRole] = useState(roles[0] ?? '')
  const tokenExpiration = accessToken ? getJwtExpiration(accessToken) : null

  const canSubmit = isUuid(userId) && isUuid(tenantId) && !isIssuingToken

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    if (!canSubmit) {
      return
    }

    await issueDevToken(userId, tenantId, role ? [role] : [])
  }

  return (
    <section className="panel auth-panel" aria-labelledby="auth-title">
      <div className="panel-header">
        <div>
          <p className="eyebrow">
            Local auth
            <BackendModeBadge />
          </p>
          <h1 id="auth-title">Pabal Chat Frontend</h1>
        </div>
        <span className={accessToken ? 'status-pill is-ready' : 'status-pill'}>
          {accessToken ? 'Token ready' : 'No token'}
        </span>
      </div>

      <form className="auth-form" onSubmit={handleSubmit}>
        <label>
          <span>User ID</span>
          <input
            value={userId}
            onChange={(event) => setUserId(event.target.value)}
            spellCheck={false}
          />
        </label>

        <label>
          <span>Tenant ID</span>
          <input
            value={tenantId}
            onChange={(event) => setTenantId(event.target.value)}
            spellCheck={false}
          />
        </label>

        <label>
          <span>Local test role</span>
          <select value={role} onChange={(event) => setRole(event.target.value)}>
            {roleOptions.map((option) => (
              <option key={option.value || 'member'} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>

        <div className="button-row">
          <Button type="submit" disabled={!canSubmit}>
            {isIssuingToken ? 'Issuing...' : 'Issue local token'}
          </Button>
          <Button type="button" variant="ghost" onClick={clearSession}>
            Clear
          </Button>
        </div>
      </form>

      {error && <p className="error-text">{error.message}</p>}

      <dl className="runtime-grid">
        <div>
          <dt>Backend</dt>
          <dd>{env.backendMode}</dd>
        </div>
        <div>
          <dt>API</dt>
          <dd>{env.apiBaseUrl || 'same-origin'}</dd>
        </div>
        <div>
          <dt>WS</dt>
          <dd>{env.wsUrl}</dd>
        </div>
        <div>
          <dt>Auth header</dt>
          <dd>{accessToken ? 'Authorization: Bearer <token>' : '-'}</dd>
        </div>
        <div>
          <dt>Local role</dt>
          <dd>{displayRole(roles)}</dd>
        </div>
        <div>
          <dt>Token expires</dt>
          <dd>
            {tokenExpiration ? formatDateTime(tokenExpiration.toISOString()) : '-'}
            {accessToken && isJwtExpired(accessToken) ? ' (expired)' : ''}
          </dd>
        </div>
      </dl>
    </section>
  )
}
