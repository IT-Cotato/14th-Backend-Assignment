import type { Comment, PageResponse } from '../types'
import { client } from './client'

export async function fetchComments(
  postId: number,
  page: number,
  size: number,
): Promise<PageResponse<Comment>> {
  const response = await client.get<PageResponse<Comment>>(
    `/api/study-posts/${postId}/comments`,
    { params: { page, size } },
  )
  return response.data
}

export async function createComment(
  postId: number,
  content: string,
): Promise<{ commentId: number }> {
  const response = await client.post<{ commentId: number }>(
    `/api/study-posts/${postId}/comments`,
    { content },
  )
  return response.data
}

export async function updateComment(
  commentId: number,
  content: string,
): Promise<{ commentId: number }> {
  const response = await client.patch<{ commentId: number }>(`/api/comments/${commentId}`, {
    content,
  })
  return response.data
}

export async function deleteComment(commentId: number): Promise<void> {
  await client.delete(`/api/comments/${commentId}`)
}
