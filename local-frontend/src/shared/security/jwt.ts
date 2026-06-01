type JwtPayload = {
  exp?: number
  iat?: number
  sub?: string
  uid?: string
  tenant_id?: string
  roles?: string[]
  scope?: string
  scp?: string[] | string
  permissions?: string[]
  [claim: string]: unknown
}

const decodeBase64Url = (value: string) => {
  const base64 = value.replaceAll('-', '+').replaceAll('_', '/')
  const padded = base64.padEnd(Math.ceil(base64.length / 4) * 4, '=')
  return atob(padded)
}

export const decodeJwtPayload = (token: string): JwtPayload | null => {
  const [, payload] = token.split('.')

  if (!payload) {
    return null
  }

  try {
    return JSON.parse(decodeBase64Url(payload)) as JwtPayload
  } catch {
    return null
  }
}

export const getJwtExpiration = (token: string) => {
  const exp = decodeJwtPayload(token)?.exp
  return typeof exp === 'number' ? new Date(exp * 1000) : null
}

export const isJwtExpired = (token: string, clockSkewSeconds = 0) => {
  const exp = decodeJwtPayload(token)?.exp

  if (typeof exp !== 'number') {
    return false
  }

  return Date.now() >= (exp - clockSkewSeconds) * 1000
}

export const getJwtRoles = (token: string) => {
  const roles = decodeJwtPayload(token)?.roles
  return Array.isArray(roles) ? roles.filter((role): role is string => typeof role === 'string') : []
}

export const getJwtScopes = (token: string) => {
  const payload = decodeJwtPayload(token)

  if (!payload) {
    return []
  }

  if (Array.isArray(payload.scp)) {
    return payload.scp.filter((scope): scope is string => typeof scope === 'string')
  }

  if (typeof payload.scp === 'string') {
    return payload.scp.split(/\s+/).filter(Boolean)
  }

  if (typeof payload.scope === 'string') {
    return payload.scope.split(/\s+/).filter(Boolean)
  }

  return []
}
