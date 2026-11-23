import type { Author } from './api'

// docs/api-spec.md 6장의 댓글 검증 규칙(1~500자)과 같아요.
export const COMMENT_MAX_LENGTH = 500

// 댓글 목록은 page=0, size=20으로 불러와요.
export const COMMENT_PAGE_SIZE = 20

// GET /api/study-posts/{postId}/comments 목록의 한 항목이에요.
export interface Comment {
    commentId: number
    content: string
    author: Author
    isAuthor: boolean
    createdAt: string
}

// POST, PATCH 요청 본문이에요.
export interface CommentRequest {
    content: string
}

export interface CommentIdResponse {
    commentId: number
}
