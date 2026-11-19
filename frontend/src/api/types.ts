// docs/api-spec.md 의 필드 이름과 맞춘다

export type Part = 'PM' | 'DE' | 'BE' | 'FE'

export const PARTS: Part[] = ['PM', 'DE', 'BE', 'FE']

export const PART_LABEL: Record<Part, string> = {
  PM: '기획',
  DE: '디자인',
  BE: '백엔드',
  FE: '프론트엔드',
}

export interface ApiResponse<T> {
  success: true
  status: number
  data: T
  timestamp: string
}

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
  tokenType: 'Bearer'
}

export interface MyInfo {
  userId: number
  email: string
  name: string
  nickname: string
  generation: number
  part: Part
}

export type StudyStatus = 'RECRUITING' | 'CLOSED'

export const STUDY_STATUS_LABEL: Record<StudyStatus, string> = {
  RECRUITING: '모집 중',
  CLOSED: '모집 마감',
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

export interface StudyPostSearchParams {
  keyword?: string
  status?: StudyStatus
  part?: Part
  page?: number
  size?: number
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
