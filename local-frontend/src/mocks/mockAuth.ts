import { decodeJwtPayload } from '../shared/security/jwt'
import type { UUID } from '../shared/types/api'

export type MockPrincipal = {
  userId: UUID
  tenantId: UUID
  roles: string[]
}

const base64UrlEncode = (value: unknown) =>
  btoa(JSON.stringify(value))
    .replaceAll('+', '-')
    .replaceAll('/', '_')
    .replaceAll('=', '')

export const createMockToken = ({ userId, tenantId, roles }: MockPrincipal) => {
  const nowSeconds = Math.floor(Date.now() / 1000)
  const header = {
    alg: 'none',
    typ: 'JWT',
  }
  const payload = {
    exp: nowSeconds + 60 * 60 * 8,
    iat: nowSeconds,
    roles,
    sub: userId,
    tenant_id: tenantId,
    uid: userId,
  }

  return `${base64UrlEncode(header)}.${base64UrlEncode(payload)}.mock`
}

export const principalFromToken = (accessToken: string): MockPrincipal | null => {
  const payload = decodeJwtPayload(accessToken)

  if (!payload) {
    return null
  }

  const userId = typeof payload.uid === 'string'
    ? payload.uid
    : typeof payload.sub === 'string'
      ? payload.sub
      : null
  const tenantId = typeof payload.tenant_id === 'string' ? payload.tenant_id : null
  const roles = Array.isArray(payload.roles)
    ? payload.roles.filter((role): role is string => typeof role === 'string')
    : []

  return userId && tenantId ? { userId, tenantId, roles } : null
}

export const principalFromAuthorization = (authorization: string | null) => {
  const prefix = 'Bearer '

  if (!authorization?.startsWith(prefix)) {
    return null
  }

  return principalFromToken(authorization.slice(prefix.length))
}
