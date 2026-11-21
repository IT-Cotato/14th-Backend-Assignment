import type {
  PageResponse,
  StudyPostCreateRequest,
  StudyPostDetail,
  StudyPostListParams,
  StudyPostSummary,
  StudyPostUpdateRequest,
} from '../types'
import { client } from './client'

export async function fetchStudyPosts(
  params: StudyPostListParams,
): Promise<PageResponse<StudyPostSummary>> {
  const response = await client.get<PageResponse<StudyPostSummary>>('/api/study-posts', {
    params,
  })
  return response.data
}

export async function fetchStudyPost(postId: number): Promise<StudyPostDetail> {
  const response = await client.get<StudyPostDetail>(`/api/study-posts/${postId}`)
  return response.data
}

export async function createStudyPost(
  request: StudyPostCreateRequest,
): Promise<{ postId: number }> {
  const response = await client.post<{ postId: number }>('/api/study-posts', request)
  return response.data
}

export async function updateStudyPost(
  postId: number,
  request: StudyPostUpdateRequest,
): Promise<{ postId: number }> {
  const response = await client.patch<{ postId: number }>(`/api/study-posts/${postId}`, request)
  return response.data
}

export async function deleteStudyPost(postId: number): Promise<void> {
  await client.delete(`/api/study-posts/${postId}`)
}
