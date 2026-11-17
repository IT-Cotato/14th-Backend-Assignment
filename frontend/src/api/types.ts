export type Part = 'PM' | 'DE' | 'BE' | 'FE'

export const PARTS: { value: Part; label: string }[] = [
  { value: 'PM', label: '기획 (PM)' },
  { value: 'DE', label: '디자인 (DE)' },
  { value: 'BE', label: '백엔드 (BE)' },
  { value: 'FE', label: '프론트엔드 (FE)' },
]

export function getPartLabel(part: Part): string {
  return PARTS.find((p) => p.value === part)?.label ?? part
}

export type StudyStatus = 'RECRUITING' | 'CLOSED'

export const STUDY_STATUSES: { value: StudyStatus; label: string }[] = [
  { value: 'RECRUITING', label: '모집 중' },
  { value: 'CLOSED', label: '모집 마감' },
]

export function getStudyStatusLabel(status: StudyStatus): string {
  return STUDY_STATUSES.find((s) => s.value === status)?.label ?? status
}

// 인증이 필요한 요청에서 토큰이 없거나 유효하지 않거나 로그아웃된 경우의 응답 code
export const ERROR_CODE_UNAUTHORIZED = 'COMMON-004'

// 성공 응답은 data 안쪽만 필요해서 axios 응답 래퍼 타입으로만 사용한다.
export interface ApiResponse<T> {
  success: boolean
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

export interface UserMe {
  userId: number
  email: string
  name: string
  nickname: string
  generation: number
  part: Part
}

// 모집글·댓글의 작성자 정보
export interface Author {
  nickname: string
  generation: number
  part: Part
}

// 목록 API의 data 형식
export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
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

// 바꿀 필드만 보낸다
export interface StudyPostUpdateRequest {
  title?: string
  content?: string
  studyStatus?: StudyStatus
}

export interface StudyPostIdResponse {
  postId: number
}
