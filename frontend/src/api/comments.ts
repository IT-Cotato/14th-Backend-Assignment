import { apiClient } from './client'
import type { ApiResponse, Comment, CommentRequest, PageResponse } from './types'

// 오래된 순으로 내려와요
export async function getComments(
  postId: number,
  page: number,
  size: number,
): Promise<PageResponse<Comment>> {
  const response = await apiClient.get<ApiResponse<PageResponse<Comment>>>(
    `/api/study-posts/${postId}/comments`,
    { params: { page, size } },
  )
  return response.data.data
}

export async function createComment(postId: number, request: CommentRequest): Promise<{ commentId: number }> {
  const response = await apiClient.post<ApiResponse<{ commentId: number }>>(
    `/api/study-posts/${postId}/comments`,
    request,
  )
  return response.data.data
}

export async function updateComment(commentId: number, request: CommentRequest): Promise<{ commentId: number }> {
  const response = await apiClient.patch<ApiResponse<{ commentId: number }>>(
    `/api/comments/${commentId}`,
    request,
  )
  return response.data.data
}

// 204 No Content라서 돌려줄 값이 없어요
export async function deleteComment(commentId: number): Promise<void> {
  await apiClient.delete(`/api/comments/${commentId}`)
}
