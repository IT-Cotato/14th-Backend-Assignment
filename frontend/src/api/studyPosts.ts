import api from './client'
import type { ApiResponse, PageResponse } from '../types/api'
import type {
    StudyPostCreateRequest,
    StudyPostDetail,
    StudyPostIdResponse,
    StudyPostSearchParams,
    StudyPostSummary,
    StudyPostUpdateRequest,
} from '../types/studyPost'

export async function getStudyPosts(
    params: StudyPostSearchParams,
): Promise<PageResponse<StudyPostSummary>> {
    // 값이 없는 필터는 쿼리에서 빼요.
    const query = Object.fromEntries(
        Object.entries(params).filter(([, value]) => value !== undefined && value !== ''),
    )
    const res = await api.get<ApiResponse<PageResponse<StudyPostSummary>>>('/study-posts', {
        params: query,
    })
    return res.data.data
}

export async function getStudyPost(postId: number): Promise<StudyPostDetail> {
    const res = await api.get<ApiResponse<StudyPostDetail>>(`/study-posts/${postId}`)
    return res.data.data
}

export async function createStudyPost(
    request: StudyPostCreateRequest,
): Promise<StudyPostIdResponse> {
    const res = await api.post<ApiResponse<StudyPostIdResponse>>('/study-posts', request)
    return res.data.data
}

export async function updateStudyPost(
    postId: number,
    request: StudyPostUpdateRequest,
): Promise<StudyPostIdResponse> {
    const res = await api.patch<ApiResponse<StudyPostIdResponse>>(
        `/study-posts/${postId}`,
        request,
    )
    return res.data.data
}

// 204 응답이라 본문이 없어요.
export async function deleteStudyPost(postId: number): Promise<void> {
    await api.delete(`/study-posts/${postId}`)
}
