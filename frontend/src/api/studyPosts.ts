import { apiClient } from './client'
import type {
  ApiResponse,
  PageResponse,
  StudyPostCreateRequest,
  StudyPostDetail,
  StudyPostIdResponse,
  StudyPostSearchParams,
  StudyPostSummary,
  StudyPostUpdateRequest,
} from './types'

// 각 함수는 공통 응답 래퍼를 벗기고 data만 돌려줍니다.

export async function createStudyPost(request: StudyPostCreateRequest): Promise<StudyPostIdResponse> {
  const { data } = await apiClient.post<ApiResponse<StudyPostIdResponse>>('/api/study-posts', request)
  return data.data
}

// 빈 값은 쿼리에서 뺍니다.
export async function getStudyPosts(params: StudyPostSearchParams): Promise<PageResponse<StudyPostSummary>> {
  const query = Object.fromEntries(Object.entries(params).filter(([, value]) => value !== undefined && value !== ''))
  const { data } = await apiClient.get<ApiResponse<PageResponse<StudyPostSummary>>>('/api/study-posts', {
    params: query,
  })
  return data.data
}

export async function getStudyPost(postId: number): Promise<StudyPostDetail> {
  const { data } = await apiClient.get<ApiResponse<StudyPostDetail>>(`/api/study-posts/${postId}`)
  return data.data
}

export async function updateStudyPost(postId: number, request: StudyPostUpdateRequest): Promise<StudyPostIdResponse> {
  const { data } = await apiClient.patch<ApiResponse<StudyPostIdResponse>>(`/api/study-posts/${postId}`, request)
  return data.data
}

// 204 본문 없음
export async function deleteStudyPost(postId: number): Promise<void> {
  await apiClient.delete(`/api/study-posts/${postId}`)
}
