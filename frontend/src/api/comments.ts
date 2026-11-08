import api from './client'
import type { ApiResponse, PageResponse } from '../types/api'
import type { Comment, CommentIdResponse, CommentRequest } from '../types/comment'

// 비로그인 사용자도 조회할 수 있어요. 로그인하면 내 댓글은 isAuthor가 true예요.
export async function getComments(
    postId: number,
    page: number,
    size: number,
): Promise<PageResponse<Comment>> {
    const res = await api.get<ApiResponse<PageResponse<Comment>>>(
        `/study-posts/${postId}/comments`,
        { params: { page, size } },
    )
    return res.data.data
}

export async function createComment(
    postId: number,
    request: CommentRequest,
): Promise<CommentIdResponse> {
    const res = await api.post<ApiResponse<CommentIdResponse>>(
        `/study-posts/${postId}/comments`,
        request,
    )
    return res.data.data
}

export async function updateComment(
    commentId: number,
    request: CommentRequest,
): Promise<CommentIdResponse> {
    const res = await api.patch<ApiResponse<CommentIdResponse>>(`/comments/${commentId}`, request)
    return res.data.data
}

// 204 응답이라 본문이 없어요.
export async function deleteComment(commentId: number): Promise<void> {
    await api.delete(`/comments/${commentId}`)
}
