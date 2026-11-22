import { apiClient } from './client'
import type {
  ApiResponse,
  PageResponse,
  Part,
  StudyPostCreateRequest,
  StudyPostDetail,
  StudyPostIdResponse,
  StudyPostSummary,
  StudyPostUpdateRequest,
  StudyStatus,
} from './types'

export interface StudyPostListParams {
  keyword?: string
  status?: StudyStatus
  part?: Part
  page: number // API 기준(0부터)
  size: number
}

export async function getStudyPosts(params: StudyPostListParams): Promise<PageResponse<StudyPostSummary>> {
  // 값이 없는 필터는 보내지 않아요 (빈 문자열을 보내면 서버가 enum 변환에 실패해요)
  const query: Record<string, string | number> = { page: params.page, size: params.size }
  if (params.keyword) query.keyword = params.keyword
  if (params.status) query.status = params.status
  if (params.part) query.part = params.part

  const res = await apiClient.get<ApiResponse<PageResponse<StudyPostSummary>>>('/api/study-posts', {
    params: query,
  })
  return res.data.data
}

// 토큰이 있으면 같이 보내져서 isAuthor가 맞게 나와요
export async function getStudyPost(postId: number): Promise<StudyPostDetail> {
  const res = await apiClient.get<ApiResponse<StudyPostDetail>>(`/api/study-posts/${postId}`)
  return res.data.data
}

export async function createStudyPost(request: StudyPostCreateRequest): Promise<StudyPostIdResponse> {
  const res = await apiClient.post<ApiResponse<StudyPostIdResponse>>('/api/study-posts', request)
  return res.data.data
}

export async function updateStudyPost(
  postId: number,
  request: StudyPostUpdateRequest,
): Promise<StudyPostIdResponse> {
  const res = await apiClient.patch<ApiResponse<StudyPostIdResponse>>(`/api/study-posts/${postId}`, request)
  return res.data.data
}

// 204 응답이라 본문이 없어요
export async function deleteStudyPost(postId: number): Promise<void> {
  await apiClient.delete(`/api/study-posts/${postId}`)
}
