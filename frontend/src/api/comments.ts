import { apiClient } from './client'
import type { ApiResponse, Comment, CommentIdResponse, PageResponse } from './types'

// 오래된 순. 토큰이 있으면 같이 보내져서 내 댓글의 isAuthor가 true로 와요
export async function getComments(postId: number, page: number, size: number): Promise<PageResponse<Comment>> {
  const res = await apiClient.get<ApiResponse<PageResponse<Comment>>>(`/api/study-posts/${postId}/comments`, {
    params: { page, size },
  })
  return res.data.data
}

export async function createComment(postId: number, content: string): Promise<CommentIdResponse> {
  const res = await apiClient.post<ApiResponse<CommentIdResponse>>(`/api/study-posts/${postId}/comments`, {
    content,
  })
  return res.data.data
}

export async function updateComment(commentId: number, content: string): Promise<CommentIdResponse> {
  const res = await apiClient.patch<ApiResponse<CommentIdResponse>>(`/api/comments/${commentId}`, { content })
  return res.data.data
}

// 204 응답이라 본문이 없어요
export async function deleteComment(commentId: number): Promise<void> {
  await apiClient.delete(`/api/comments/${commentId}`)
}
