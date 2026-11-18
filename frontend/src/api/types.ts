// 백엔드 공통 응답 (docs/api-spec.md)
export interface ApiResponse<T> {
  success: boolean
  status: number
  data: T
}

export interface ErrorResponse {
  success: false
  status: number
  code: string
  message: string
}

export type Part = 'PM' | 'DE' | 'BE' | 'FE'

export const PART_LABELS: Record<Part, string> = {
  PM: '기획',
  DE: '디자인',
  BE: '백엔드',
  FE: '프론트엔드',
}

export interface SignupRequest {
  email: string
  password: string
  name: string
  nickname: string
  generation: number
  part: Part
}

export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  tokenType: string
}

export interface User {
  userId: number
  email: string
  name: string
  nickname: string
  generation: number
  part: Part
}

// ---- 스터디 모집글 ----
export type StudyStatus = 'RECRUITING' | 'CLOSED'

export const STATUS_LABELS: Record<StudyStatus, string> = {
  RECRUITING: '모집 중',
  CLOSED: '모집 마감',
}

export interface Author {
  nickname: string
  generation: number
  part: Part
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface StudyPostListItem {
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

export interface StudyPostCreateRequest {
  title: string
  content: string
}

// 바꿀 필드만 보낸다
export interface StudyPostUpdateRequest {
  title?: string
  content?: string
  studyStatus?: StudyStatus
}

export interface StudyPostSearchParams {
  keyword?: string
  status?: StudyStatus
  part?: Part
  page?: number
  size?: number
}

// ---- 댓글 (스터디 신청) ----
export interface Comment {
  commentId: number
  content: string
  author: Author
  isAuthor: boolean
  createdAt: string
}
