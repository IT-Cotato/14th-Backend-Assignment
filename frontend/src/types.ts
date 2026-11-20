export type Part = 'PM' | 'DE' | 'BE' | 'FE'
export type StudyStatus = 'RECRUITING' | 'CLOSED'

export const PARTS: Part[] = ['PM', 'DE', 'BE', 'FE']

export const PART_LABELS: Record<Part, string> = {
  PM: '기획',
  DE: '디자인',
  BE: '백엔드',
  FE: '프론트엔드',
}

export const STUDY_STATUSES: StudyStatus[] = ['RECRUITING', 'CLOSED']

export const STUDY_STATUS_LABELS: Record<StudyStatus, string> = {
  RECRUITING: '모집 중',
  CLOSED: '모집 마감',
}

/** 서버의 공통 성공 응답 */
export interface ApiResponse<T> {
  success: boolean
  status: number
  data: T
  timestamp: string
}

/** 서버의 공통 실패 응답 */
export interface ErrorResponse {
  success: false
  status: number
  code: string
  message: string
  method: string
  path: string
  timestamp: string
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface Author {
  nickname: string
  generation: number
  part: Part
}

export interface User {
  userId: number
  email: string
  name: string
  nickname: string
  generation: number
  part: Part
}

export interface SignupRequest {
  email: string
  password: string
  name: string
  nickname: string
  generation: number | null
  part: Part
}

export interface LoginRequest {
  email: string
  password: string
}

export interface TokenResponse {
  accessToken: string
  tokenType: string
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

export interface StudyPostListParams {
  keyword?: string
  status?: StudyStatus
  part?: Part
  page: number
  size: number
}

export interface StudyPostCreateRequest {
  title: string
  content: string
}

export interface StudyPostUpdateRequest {
  title?: string
  content?: string
  studyStatus?: StudyStatus
}

export interface Comment {
  commentId: number
  content: string
  author: Author
  isAuthor: boolean
  createdAt: string
}
