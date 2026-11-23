import type { Part } from './auth'
import { request } from './client'

export type StudyComment = {
  commentId: number
  content: string
  author: { nickname: string; generation: number; part: Part }
  isAuthor: boolean
  createdAt: string
}

type CommentPage = {
  content: StudyComment[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export const commentPageSize = 20

function postCommentsPath(postId: string) {
  return `/api/study-posts/${encodeURIComponent(postId)}/comments`
}

export function listComments(postId: string, page: number, token: string | null, signal: AbortSignal) {
  return request<CommentPage>(`${postCommentsPath(postId)}?page=${page}&size=${commentPageSize}`, {
    token: token ?? undefined, signal,
  })
}

export function createComment(postId: string, content: string, token: string, signal: AbortSignal) {
  return request<{ commentId: number }>(postCommentsPath(postId), {
    method: 'POST', body: { content }, token, signal,
  })
}

export function updateComment(commentId: number, content: string, token: string, signal: AbortSignal) {
  return request<{ commentId: number }>(`/api/comments/${commentId}`, {
    method: 'PATCH', body: { content }, token, signal,
  })
}

export function deleteComment(commentId: number, token: string, signal: AbortSignal) {
  return request<void>(`/api/comments/${commentId}`, { method: 'DELETE', token, signal })
}
