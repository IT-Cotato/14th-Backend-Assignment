// 서버 날짜 "2026-11-08T12:00:00" → 화면 표시 "2026-11-08 12:00"
export function formatDateTime(value: string): string {
  return value.replace('T', ' ').slice(0, 16)
}
