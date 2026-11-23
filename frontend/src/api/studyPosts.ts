import type { Part } from './auth'
import { request } from './client'

export type StudyStatus = 'RECRUITING' | 'CLOSED'

type Author = { nickname: string; generation: number; part: Part }
type StudyPostSummary = {
  postId: number
  title: string
  studyStatus: StudyStatus
  author: Author
  commentCount: number
  createdAt: string
}

type StudyPostPage = {
  content: StudyPostSummary[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type StudyPostDetail = Omit<StudyPostSummary, 'commentCount'> & {
  content: string
  isAuthor: boolean
  updatedAt: string
}

export const studyStatusLabels: Record<StudyStatus, string> = {
  RECRUITING: '모집 중',
  CLOSED: '모집 마감',
}

export function listStudyPosts(query: string, signal: AbortSignal) {
  return request<StudyPostPage>(`/api/study-posts?${query}`, { signal })
}

export function getStudyPost(postId: string, token: string | null, signal: AbortSignal) {
  return request<StudyPostDetail>(`/api/study-posts/${encodeURIComponent(postId)}`, {
    token: token ?? undefined,
    signal,
  })
}

export type StudyPostFields = Pick<StudyPostDetail, 'title' | 'content'>
export type StudyPostChanges = Partial<StudyPostFields & { studyStatus: StudyStatus }>

export function createStudyPost(body: StudyPostFields, token: string, signal: AbortSignal) {
  return request<{ postId: number }>('/api/study-posts', { method: 'POST', body, token, signal })
}

export function updateStudyPost(postId: string, body: StudyPostChanges, token: string, signal: AbortSignal) {
  return request<{ postId: number }>(`/api/study-posts/${encodeURIComponent(postId)}`, {
    method: 'PATCH', body, token, signal,
  })
}

export function deleteStudyPost(postId: string, token: string, signal: AbortSignal) {
  return request<void>(`/api/study-posts/${encodeURIComponent(postId)}`, { method: 'DELETE', token, signal })
}
