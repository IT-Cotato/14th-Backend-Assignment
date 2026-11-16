import { api } from './client'
import type { ApiResponse } from './client'
import type { Author, PageResponse } from './studyPosts'

export const COMMENT_MAX = 500

export interface Comment {
  commentId: number
  content: string
  author: Author
  isAuthor: boolean
  createdAt: string
}

export interface CommentIdResponse {
  commentId: number
}

export async function getComments(
  postId: number | string,
  page: number,
  size: number,
): Promise<PageResponse<Comment>> {
  const res = await api.get<ApiResponse<PageResponse<Comment>>>(
    `/api/study-posts/${postId}/comments`,
    { params: { page, size } },
  )
  return res.data.data
}

export async function createComment(
  postId: number | string,
  content: string,
): Promise<CommentIdResponse> {
  const res = await api.post<ApiResponse<CommentIdResponse>>(
    `/api/study-posts/${postId}/comments`,
    { content },
  )
  return res.data.data
}

export async function updateComment(
  commentId: number,
  content: string,
): Promise<CommentIdResponse> {
  const res = await api.patch<ApiResponse<CommentIdResponse>>(`/api/comments/${commentId}`, {
    content,
  })
  return res.data.data
}

export async function deleteComment(commentId: number): Promise<void> {
  await api.delete(`/api/comments/${commentId}`)
}
