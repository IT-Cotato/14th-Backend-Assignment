// 백엔드 공통 성공 응답. 실제 값은 data 안에 있어요.
export interface ApiResponse<T> {
  success: true
  status: number
  data: T
  timestamp: string
}

// 백엔드 공통 실패 응답 (ErrorResponse)
export interface ErrorResponse {
  success: false
  status: number
  code: string
  message: string
  method: string
  path: string
  timestamp: string
}

export const PARTS = ['PM', 'DE', 'BE', 'FE'] as const
export type Part = (typeof PARTS)[number]

export const PART_LABELS: Record<Part, string> = {
  PM: '기획',
  DE: '디자인',
  BE: '백엔드',
  FE: '프론트엔드',
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
  generation: number
  part: Part
}

export interface SignupResponse {
  userId: number
}

export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  tokenType: string
}

// 목록 API 공통 페이지 응답 (api-spec "페이지 응답")
export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export const STUDY_STATUSES = ['RECRUITING', 'CLOSED'] as const
export type StudyStatus = (typeof STUDY_STATUSES)[number]

export const STUDY_STATUS_LABELS: Record<StudyStatus, string> = {
  RECRUITING: '모집 중',
  CLOSED: '마감',
}

// 모집글·댓글의 작성자 정보 (api-spec "작성자 정보")
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

export interface StudyPostCreateRequest {
  title: string
  content: string
}

// 바꿀 필드만 보내요
export interface StudyPostUpdateRequest {
  title?: string
  content?: string
  studyStatus?: StudyStatus
}

export interface StudyPostIdResponse {
  postId: number
}

// 모집글 댓글 = 스터디 신청
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
