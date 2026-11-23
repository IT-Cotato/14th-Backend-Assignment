/** 2026-11-08T12:00:00 -> 2026-11-08 12:00 */
export function formatDateTime(value: string) {
  return value.replace('T', ' ').slice(0, 16)
}
