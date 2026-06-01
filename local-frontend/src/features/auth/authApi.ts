import { httpClient } from '../../shared/api/httpClient'
import { apiPaths } from '../../shared/api/apiPaths'
import type { LocalTokenResponse, UUID } from '../../shared/types/api'

export type IssueLocalTokenParams = {
  userId: UUID
  tenantId: UUID
  scopes?: string[]
  roles?: string[]
}

export const issueLocalToken = async (params: IssueLocalTokenParams) => {
  const searchParams = new URLSearchParams()
  searchParams.set('userId', params.userId)
  searchParams.set('tenantId', params.tenantId)
  params.scopes?.forEach((scope) => searchParams.append('scope', scope))
  params.roles?.forEach((role) => searchParams.append('role', role))

  const response = await httpClient.get<LocalTokenResponse>(
    `${apiPaths.devToken()}?${searchParams.toString()}`,
  )

  return response.data
}
