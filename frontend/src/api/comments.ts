import { api } from './client'
import type { ApiResponse, PageResponse, StudyPostComment } from './types'

export const commentKeys = {
  all: ['comments'] as const,
  list: (postId: number, page: number) => ['comments', postId, page] as const,
  byPost: (postId: number) => ['comments', postId] as const,
}

export async function getComments(postId: number, page: number, size = 20) {
  const { data } = await api.get<ApiResponse<PageResponse<StudyPostComment>>>(
    `/api/study-posts/${postId}/comments`,
    { params: { page, size } },
  )
  return data.data
}

export async function createComment(postId: number, content: string) {
  const { data } = await api.post<ApiResponse<{ commentId: number }>>(`/api/study-posts/${postId}/comments`, {
    content,
  })
  return data.data
}

export async function updateComment(commentId: number, content: string) {
  const { data } = await api.patch<ApiResponse<{ commentId: number }>>(`/api/comments/${commentId}`, { content })
  return data.data
}

export async function deleteComment(commentId: number) {
  await api.delete(`/api/comments/${commentId}`)
}
