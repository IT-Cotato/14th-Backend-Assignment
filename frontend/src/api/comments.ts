import { client } from './client.ts'
import type { ApiResponse, Comment, PageResponse } from './types.ts'

export async function getComments(postId: number, page = 0, size = 20) {
  const res = await client.get<ApiResponse<PageResponse<Comment>>>(`/study-posts/${postId}/comments`, {
    params: { page, size },
  })
  return res.data.data
}

export async function createComment(postId: number, content: string) {
  const res = await client.post<ApiResponse<{ commentId: number }>>(`/study-posts/${postId}/comments`, { content })
  return res.data.data
}

export async function updateComment(commentId: number, content: string) {
  const res = await client.patch<ApiResponse<{ commentId: number }>>(`/comments/${commentId}`, { content })
  return res.data.data
}

export async function deleteComment(commentId: number) {
  await client.delete(`/comments/${commentId}`)
}
