import type { Author, Part } from './api'

export const STUDY_STATUSES = ['RECRUITING', 'CLOSED'] as const
export type StudyStatus = (typeof STUDY_STATUSES)[number]

export const STUDY_STATUS_LABELS: Record<StudyStatus, string> = {
    RECRUITING: '모집 중',
    CLOSED: '모집 마감',
}

export const TITLE_MAX_LENGTH = 50
export const CONTENT_MAX_LENGTH = 2000

// GET /api/study-posts 목록의 한 항목이에요.
export interface StudyPostSummary {
    postId: number
    title: string
    studyStatus: StudyStatus
    author: Author
    commentCount: number
    createdAt: string
}

// GET /api/study-posts/{postId} 응답이에요.
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

// PATCH 요청이에요. 바꿀 필드만 보내요.
export interface StudyPostUpdateRequest {
    title?: string
    content?: string
    studyStatus?: StudyStatus
}

export interface StudyPostIdResponse {
    postId: number
}
