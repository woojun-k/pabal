import { apiContract, type LocalRole } from '../constants/apiContract'

export type RoleOption = {
  label: string
  value: LocalRole | ''
}

export const roleOptions: RoleOption[] = [
  { label: 'Regular member', value: '' },
  { label: 'Tenant admin', value: 'tenant-admin' },
  { label: 'Workspace admin', value: 'workspace-admin' },
  { label: 'Channel owner', value: 'channel-owner' },
  { label: 'Pabal admin', value: 'pabal-admin' },
]

export const isLocalRole = (value: string): value is LocalRole =>
  (apiContract.localRoles as readonly string[]).includes(value)

export const normalizeRoleAuthority = (role: string) => {
  const value = role.trim().toUpperCase().replaceAll('-', '_')
  return value.startsWith('ROLE_') ? value : `ROLE_${value}`
}

export const hasRole = (roles: string[], role: string) => {
  const expected = normalizeRoleAuthority(role)
  return roles.map(normalizeRoleAuthority).includes(expected)
}

export const hasAnyRole = (roles: string[], candidates: string[]) =>
  candidates.some((candidate) => hasRole(roles, candidate))

export const displayRole = (roles: string[]) => {
  if (roles.length === 0) {
    return 'regular member'
  }
  return roles.join(', ')
}
