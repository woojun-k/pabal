import { create } from 'zustand'
import { toApiError, type ApiError } from '../../shared/api/apiError'
import { tokenStorage } from '../../shared/security/tokenStorage'
import type { UUID } from '../../shared/types/api'
import { issueLocalToken } from './authApi'

type AuthState = {
  accessToken: string | null
  userId: UUID | null
  tenantId: UUID | null
  roles: string[]
  isIssuingToken: boolean
  error: ApiError | null
  hydrate: () => void
  issueDevToken: (userId: UUID, tenantId: UUID, roles?: string[]) => Promise<void>
  clearSession: () => void
}

const storedSession = tokenStorage.load()

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: storedSession?.accessToken ?? null,
  userId: storedSession?.userId ?? null,
  tenantId: storedSession?.tenantId ?? null,
  roles: storedSession?.roles ?? [],
  isIssuingToken: false,
  error: null,

  hydrate: () => {
    const session = tokenStorage.load()
    set({
      accessToken: session?.accessToken ?? null,
      userId: session?.userId ?? null,
      tenantId: session?.tenantId ?? null,
      roles: session?.roles ?? [],
    })
  },

  issueDevToken: async (userId, tenantId, roles = []) => {
    set({ isIssuingToken: true, error: null })

    try {
      const response = await issueLocalToken({ userId, tenantId, roles })
      const session = {
        accessToken: response.accessToken,
        userId,
        tenantId,
        roles,
      }

      tokenStorage.save(session)
      set({
        ...session,
        isIssuingToken: false,
        error: null,
      })
    } catch (error) {
      set({
        isIssuingToken: false,
        error: toApiError(error),
      })
    }
  },

  clearSession: () => {
    tokenStorage.clear()
    set({
      accessToken: null,
      userId: null,
      tenantId: null,
      roles: [],
      error: null,
    })
  },
}))
