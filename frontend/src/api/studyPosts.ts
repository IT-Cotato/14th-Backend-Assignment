import { api } from './client'
import type { ApiResponse } from './client'
import type { Part } from './auth'

export const STUDY_STATUSES = ['RECRUITING', 'CLOSED'] as const
export type StudyStatus = (typeof STUDY_STATUSES)[number]

export const STUDY_STATUS_LABEL: Record<StudyStatus, string> = {
  RECRUITING: '모집 중',
  CLOSED: '모집 마감',
}

export interface Author {
  nickname: string
  generation: number
  part: Part
}

export interface StudyPostSummary {
  postId: number
  title: string
  studyStatus: StudyStatus
  author: Author
  commentCount: number
  createdAt: string
}

export interface StudyPostDetail {
  postId: number
  title: string
  content: string
  studyStatus: StudyStatus
  author: Author
  isAuthor: boolean
  createdAt: string
  updatedAt: string
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface StudyPostListQuery {
  keyword?: string
  status?: StudyStatus | ''
  part?: Part | ''
  page?: number
  size?: number
}

export interface CreateStudyPostRequest {
  title: string
  content: string
}

export interface UpdateStudyPostRequest {
  title?: string
  content?: string
  studyStatus?: StudyStatus
}

export interface StudyPostIdResponse {
  postId: number
}

export async function getStudyPosts(
  query: StudyPostListQuery,
): Promise<PageResponse<StudyPostSummary>> {
  // 빈 값은 보내지 않는다 (서버 기본값 사용)
  const params: Record<string, string | number> = {}
  const keyword = query.keyword?.trim()
  if (keyword) params.keyword = keyword
  if (query.status) params.status = query.status
  if (query.part) params.part = query.part
  if (query.page !== undefined) params.page = query.page
  if (query.size !== undefined) params.size = query.size

  const res = await api.get<ApiResponse<PageResponse<StudyPostSummary>>>('/api/study-posts', {
    params,
  })
  return res.data.data
}

export async function getStudyPost(postId: number | string): Promise<StudyPostDetail> {
  const res = await api.get<ApiResponse<StudyPostDetail>>(`/api/study-posts/${postId}`)
  return res.data.data
}

export async function createStudyPost(
  request: CreateStudyPostRequest,
): Promise<StudyPostIdResponse> {
  const res = await api.post<ApiResponse<StudyPostIdResponse>>('/api/study-posts', request)
  return res.data.data
}

export async function updateStudyPost(
  postId: number | string,
  request: UpdateStudyPostRequest,
): Promise<StudyPostIdResponse> {
  const res = await api.patch<ApiResponse<StudyPostIdResponse>>(
    `/api/study-posts/${postId}`,
    request,
  )
  return res.data.data
}

export async function deleteStudyPost(postId: number | string): Promise<void> {
  await api.delete(`/api/study-posts/${postId}`)
}

// "2026-11-08T12:00:00" → "2026-11-08 12:00"
export function formatDateTime(value: string): string {
  return value.replace('T', ' ').slice(0, 16)
}
