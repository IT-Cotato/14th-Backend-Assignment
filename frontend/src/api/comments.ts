import { apiClient } from "./client";
import type { ApiResponse, Author, PageResponse } from "./types";

export interface CommentListItem {
  commentId: number;
  content: string;
  author: Author;
  isAuthor: boolean;
  createdAt: string;
}

export interface CreateCommentRequest {
  content: string;
}

export interface CreateCommentResponse {
  commentId: number;
}

export interface UpdateCommentRequest {
  content: string;
}

export interface UpdateCommentResponse {
  commentId: number;
}

export async function getComments(
  postId: number,
  page: number,
  size: number,
): Promise<PageResponse<CommentListItem>> {
  const response = await apiClient.get<ApiResponse<PageResponse<CommentListItem>>>(
    `/api/study-posts/${postId}/comments`,
    { params: { page, size } },
  );
  return response.data.data;
}

export async function createComment(
  postId: number,
  request: CreateCommentRequest,
): Promise<CreateCommentResponse> {
  const response = await apiClient.post<ApiResponse<CreateCommentResponse>>(
    `/api/study-posts/${postId}/comments`,
    request,
  );
  return response.data.data;
}

export async function updateComment(
  commentId: number,
  request: UpdateCommentRequest,
): Promise<UpdateCommentResponse> {
  const response = await apiClient.patch<ApiResponse<UpdateCommentResponse>>(
    `/api/comments/${commentId}`,
    request,
  );
  return response.data.data;
}

export async function deleteComment(commentId: number): Promise<void> {
  await apiClient.delete(`/api/comments/${commentId}`);
}
