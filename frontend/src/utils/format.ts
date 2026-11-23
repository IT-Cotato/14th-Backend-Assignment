// 백엔드의 "2026-11-08T12:00:00" 형식을 "2026.11.08 12:00"으로 바꿔요.
export function formatDateTime(value: string): string {
    const match = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})/.exec(value)
    if (!match) return value
    const [, y, m, d, hh, mm] = match
    return `${y}.${m}.${d} ${hh}:${mm}`
}

export function formatDate(value: string): string {
    return formatDateTime(value).split(' ')[0]
}
