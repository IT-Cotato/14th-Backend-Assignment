export const COMMENT_MAX = 500
export const COMMENT_PAGE_SIZE = 20

// 서버(api-spec 10·12번)와 같은 규칙: 1~500자, 공백만 있으면 안 돼요
export function validateComment(content: string): string | undefined {
  const trimmed = content.trim()
  if (!trimmed) return '댓글 내용을 입력해 주세요.'
  if (trimmed.length > COMMENT_MAX) return `댓글은 ${COMMENT_MAX}자 이하로 입력해 주세요.`
  return undefined
}

// 방금 쓴 댓글을 목록에 바로 붙일 때 서버 형식("2026-11-23T01:19:21")과 맞춰요
export function nowAsServerDateTime(): string {
  const now = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
}
