import { apiClient } from './client'
import type { ApiResponse, CommentIdResponse, CommentItem, CommentRequest, PageResponse } from './types'

export async function fetchComments(postId: number, page: number, size: number): Promise<PageResponse<CommentItem>> {
  const { data } = await apiClient.get<ApiResponse<PageResponse<CommentItem>>>(
    `/api/study-posts/${postId}/comments`,
    { params: { page, size } },
  )
  return data.data
}

export async function createComment(postId: number, body: CommentRequest): Promise<CommentIdResponse> {
  const { data } = await apiClient.post<ApiResponse<CommentIdResponse>>(`/api/study-posts/${postId}/comments`, body)
  return data.data
}

export async function updateComment(commentId: number, body: CommentRequest): Promise<CommentIdResponse> {
  const { data } = await apiClient.patch<ApiResponse<CommentIdResponse>>(`/api/comments/${commentId}`, body)
  return data.data
}

// 204 응답이라 본문이 없다.
export async function deleteComment(commentId: number): Promise<void> {
  await apiClient.delete(`/api/comments/${commentId}`)
}
