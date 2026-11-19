import { client } from './client'
import type { ApiResponse, Comment, PageResponse } from './types'

export async function getComments(postId: number, page = 0, size = 20) {
  const res = await client.get<ApiResponse<PageResponse<Comment>>>(`/api/study-posts/${postId}/comments`, {
    params: { page, size },
  })
  return res.data.data
}

export async function createComment(postId: number, content: string) {
  const res = await client.post<ApiResponse<{ commentId: number }>>(`/api/study-posts/${postId}/comments`, {
    content,
  })
  return res.data.data
}

export async function updateComment(commentId: number, content: string) {
  const res = await client.patch<ApiResponse<{ commentId: number }>>(`/api/comments/${commentId}`, { content })
  return res.data.data
}

export async function deleteComment(commentId: number) {
  await client.delete(`/api/comments/${commentId}`)
}
