export const PARTS = ['PM', 'DE', 'BE', 'FE'] as const
export type Part = (typeof PARTS)[number]

export const PART_LABELS: Record<Part, string> = {
  PM: '기획',
  DE: '디자인',
  BE: '백엔드',
  FE: '프론트엔드',
}

export const STUDY_STATUSES = ['RECRUITING', 'CLOSED'] as const
export type StudyStatus = (typeof STUDY_STATUSES)[number]

export const STATUS_LABELS: Record<StudyStatus, string> = {
  RECRUITING: '모집 중',
  CLOSED: '모집 마감',
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

export interface MyInfo {
  userId: number
  email: string
  name: string
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

export interface Comment {
  commentId: number
  content: string
  author: Author
  isAuthor: boolean
  createdAt: string
}
