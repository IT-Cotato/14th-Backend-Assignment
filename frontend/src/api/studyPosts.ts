import { apiClient } from "./client";
import type { ApiResponse, Author, PageResponse, Part } from "./types";

export type { Author };

export type StudyStatus = "RECRUITING" | "CLOSED";

export interface StudyPostListItem {
  postId: number;
  title: string;
  studyStatus: StudyStatus;
  author: Author;
  commentCount: number;
  createdAt: string;
}

export interface StudyPostDetail {
  postId: number;
  title: string;
  content: string;
  studyStatus: StudyStatus;
  author: Author;
  isAuthor: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface GetStudyPostsParams {
  keyword?: string;
  status?: StudyStatus;
  part?: Part;
  page?: number;
  size?: number;
}

export interface CreateStudyPostRequest {
  title: string;
  content: string;
}

export interface CreateStudyPostResponse {
  postId: number;
}

export interface UpdateStudyPostRequest {
  title?: string;
  content?: string;
  studyStatus?: StudyStatus;
}

export interface UpdateStudyPostResponse {
  postId: number;
}

export async function getStudyPosts(
  params: GetStudyPostsParams,
): Promise<PageResponse<StudyPostListItem>> {
  const response = await apiClient.get<ApiResponse<PageResponse<StudyPostListItem>>>(
    "/api/study-posts",
    { params },
  );
  return response.data.data;
}

export async function getStudyPost(postId: number): Promise<StudyPostDetail> {
  const response = await apiClient.get<ApiResponse<StudyPostDetail>>(`/api/study-posts/${postId}`);
  return response.data.data;
}

export async function createStudyPost(
  request: CreateStudyPostRequest,
): Promise<CreateStudyPostResponse> {
  const response = await apiClient.post<ApiResponse<CreateStudyPostResponse>>(
    "/api/study-posts",
    request,
  );
  return response.data.data;
}

export async function updateStudyPost(
  postId: number,
  request: UpdateStudyPostRequest,
): Promise<UpdateStudyPostResponse> {
  const response = await apiClient.patch<ApiResponse<UpdateStudyPostResponse>>(
    `/api/study-posts/${postId}`,
    request,
  );
  return response.data.data;
}

export async function deleteStudyPost(postId: number): Promise<void> {
  await apiClient.delete(`/api/study-posts/${postId}`);
}
