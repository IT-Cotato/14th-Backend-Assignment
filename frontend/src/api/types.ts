// 백엔드 응답 타입입니다. 필드명은 docs/api-spec.md와 백엔드 공통 응답을 그대로 따릅니다.

// 성공 응답 래퍼 (백엔드 ApiResponse<T>)
export interface ApiResponse<T> {
  success: true
  status: number
  data: T
  timestamp: string
}

// 실패 응답 (백엔드 ErrorResponse)
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

// 1. 회원가입
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

// 2. 로그인
export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  tokenType: string
}

// 4. 내 정보 조회
export interface MyInfoResponse {
  userId: number
  email: string
  name: string
  nickname: string
  generation: number
  part: Part
}

// 목록 API의 페이지 응답
export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

// 작성자 정보 (모집글, 댓글 공통)
export interface Author {
  nickname: string
  generation: number
  part: Part
}

export const STUDY_STATUSES = ['RECRUITING', 'CLOSED'] as const
export type StudyStatus = (typeof STUDY_STATUSES)[number]

export const STUDY_STATUS_LABELS: Record<StudyStatus, string> = {
  RECRUITING: '모집 중',
  CLOSED: '모집 마감',
}

// 5. 모집글 작성
export interface StudyPostCreateRequest {
  title: string
  content: string
}

// 5, 8. 모집글 작성/수정 응답
export interface StudyPostIdResponse {
  postId: number
}

// 6. 모집글 목록
export interface StudyPostSearchParams {
  keyword?: string
  status?: string
  part?: string
  page?: number
  size?: number
}

export interface StudyPostSummary {
  postId: number
  title: string
  studyStatus: StudyStatus
  author: Author
  commentCount: number
  createdAt: string
}

// 7. 모집글 상세
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

// 8. 모집글 수정 (바꿀 필드만 보내요)
export interface StudyPostUpdateRequest {
  title?: string
  content?: string
  studyStatus?: StudyStatus
}

// 10, 12. 댓글 작성/수정
export interface CommentRequest {
  content: string
}

export interface CommentIdResponse {
  commentId: number
}

// 11. 댓글 목록
export interface Comment {
  commentId: number
  content: string
  author: Author
  isAuthor: boolean
  createdAt: string
}
