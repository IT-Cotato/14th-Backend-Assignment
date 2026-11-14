// 검증 규칙은 docs/api-spec.md 10번(content 1~500자)과 같습니다.
export const COMMENT_MAX = 500

export function validateComment(content: string): string | null {
  if (!content.trim()) {
    return '댓글 내용을 입력해 주세요.'
  }
  if (content.length > COMMENT_MAX) {
    return `댓글은 ${COMMENT_MAX}자 이하여야 해요.`
  }
  return null
}
