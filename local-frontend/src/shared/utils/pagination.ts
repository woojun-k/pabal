import { apiContract } from '../constants/apiContract'

export type CursorPage<TItem> = {
  messages: TItem[]
  nextCursor: number | null
  hasNext: boolean
}

export const normalizePageSize = (size?: number | null) => {
  if (!Number.isFinite(size ?? Number.NaN)) {
    return apiContract.message.pageSizeDefault
  }

  const value = Math.trunc(size as number)
  return Math.min(
    apiContract.message.pageSizeMax,
    Math.max(apiContract.message.pageSizeMin, value),
  )
}

export const cursorParam = (cursor?: number | null) => cursor ?? undefined

export const appendCursorPage = <TItem>(
  currentItems: TItem[],
  page: CursorPage<TItem>,
  merge: (current: TItem[], incoming: TItem[]) => TItem[],
) => ({
  items: merge(currentItems, page.messages),
  nextCursor: page.nextCursor,
  hasNext: page.hasNext,
})

export const canLoadNextPage = (page?: Pick<CursorPage<unknown>, 'hasNext' | 'nextCursor'>) =>
  Boolean(page?.hasNext && page.nextCursor !== null)
