const ACCESS_TOKEN_KEY = 'pabal.local.accessToken'
const USER_ID_KEY = 'pabal.local.userId'
const TENANT_ID_KEY = 'pabal.local.tenantId'
const ROLES_KEY = 'pabal.local.roles'

export type StoredAuthSession = {
  accessToken: string
  userId: string
  tenantId: string
  roles: string[]
}

const canUseStorage = () => typeof window !== 'undefined' && !!window.localStorage

const parseStoredRoles = (roles: string | null) => {
  if (!roles) {
    return []
  }

  try {
    const parsed = JSON.parse(roles) as unknown
    return Array.isArray(parsed)
      ? parsed.filter((role): role is string => typeof role === 'string')
      : []
  } catch {
    return []
  }
}

export const tokenStorage = {
  load(): StoredAuthSession | null {
    if (!canUseStorage()) {
      return null
    }

    const accessToken = window.localStorage.getItem(ACCESS_TOKEN_KEY)
    const userId = window.localStorage.getItem(USER_ID_KEY)
    const tenantId = window.localStorage.getItem(TENANT_ID_KEY)
    const roles = window.localStorage.getItem(ROLES_KEY)

    if (!accessToken || !userId || !tenantId) {
      return null
    }

    return {
      accessToken,
      userId,
      tenantId,
      roles: parseStoredRoles(roles),
    }
  },

  save(session: StoredAuthSession) {
    if (!canUseStorage()) {
      return
    }

    window.localStorage.setItem(ACCESS_TOKEN_KEY, session.accessToken)
    window.localStorage.setItem(USER_ID_KEY, session.userId)
    window.localStorage.setItem(TENANT_ID_KEY, session.tenantId)
    window.localStorage.setItem(ROLES_KEY, JSON.stringify(session.roles))
  },

  clear() {
    if (!canUseStorage()) {
      return
    }

    window.localStorage.removeItem(ACCESS_TOKEN_KEY)
    window.localStorage.removeItem(USER_ID_KEY)
    window.localStorage.removeItem(TENANT_ID_KEY)
    window.localStorage.removeItem(ROLES_KEY)
  },
}
