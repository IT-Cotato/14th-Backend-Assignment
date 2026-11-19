import { apiClient } from './client'
import type {
  ApiResponse,
  PageResponse,
  StudyPostCreateRequest,
  StudyPostDetail,
  StudyPostListParams,
  StudyPostSummary,
  StudyPostUpdateRequest,
} from './types'

// 값이 없는 조건은 쿼리에서 빼요 (서버는 없는 조건을 "전체"로 봐요)
export async function getStudyPosts(params: StudyPostListParams): Promise<PageResponse<StudyPostSummary>> {
  const query = Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== undefined && value !== ''),
  )
  const response = await apiClient.get<ApiResponse<PageResponse<StudyPostSummary>>>('/api/study-posts', {
    params: query,
  })
  return response.data.data
}

export async function getStudyPost(postId: number): Promise<StudyPostDetail> {
  const response = await apiClient.get<ApiResponse<StudyPostDetail>>(`/api/study-posts/${postId}`)
  return response.data.data
}

export async function createStudyPost(request: StudyPostCreateRequest): Promise<{ postId: number }> {
  const response = await apiClient.post<ApiResponse<{ postId: number }>>('/api/study-posts', request)
  return response.data.data
}

export async function updateStudyPost(
  postId: number,
  request: StudyPostUpdateRequest,
): Promise<{ postId: number }> {
  const response = await apiClient.patch<ApiResponse<{ postId: number }>>(
    `/api/study-posts/${postId}`,
    request,
  )
  return response.data.data
}

// 204 No Content라서 돌려줄 값이 없어요
export async function deleteStudyPost(postId: number): Promise<void> {
  await apiClient.delete(`/api/study-posts/${postId}`)
}
