// "2026-11-23T00:56:56" → "2026.11.23"
// 서버가 시간대 없이 보내서 문자열을 그대로 잘라 써요. new Date()로 바꾸면 브라우저 시간대에 따라 날짜가 밀릴 수 있어요.
export function formatDate(dateTime: string): string {
  const [date] = dateTime.split('T')
  return date.replaceAll('-', '.')
}
