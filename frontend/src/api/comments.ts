import { apiClient } from './client'
import type { ApiResponse, Comment, CommentIdResponse, CommentRequest, PageResponse } from './types'

// 각 함수는 공통 응답 래퍼를 벗기고 data만 돌려줍니다.

export async function createComment(postId: number, request: CommentRequest): Promise<CommentIdResponse> {
  const { data } = await apiClient.post<ApiResponse<CommentIdResponse>>(`/api/study-posts/${postId}/comments`, request)
  return data.data
}

export async function getComments(postId: number, page: number, size: number): Promise<PageResponse<Comment>> {
  const { data } = await apiClient.get<ApiResponse<PageResponse<Comment>>>(`/api/study-posts/${postId}/comments`, {
    params: { page, size },
  })
  return data.data
}

export async function updateComment(commentId: number, request: CommentRequest): Promise<CommentIdResponse> {
  const { data } = await apiClient.patch<ApiResponse<CommentIdResponse>>(`/api/comments/${commentId}`, request)
  return data.data
}

// 204 본문 없음
export async function deleteComment(commentId: number): Promise<void> {
  await apiClient.delete(`/api/comments/${commentId}`)
}
