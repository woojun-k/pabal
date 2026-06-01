import type { Instant } from '../types/api'

export const formatDateTime = (value: Instant | null | undefined) => {
  if (!value) {
    return '-'
  }

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}
