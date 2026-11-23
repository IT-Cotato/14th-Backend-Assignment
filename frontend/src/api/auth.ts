import { apiClient } from "./client";
import type { ApiResponse, Part } from "./types";

export type { ApiResponse, Part } from "./types";

export interface SignupRequest {
  email: string;
  password: string;
  name: string;
  nickname: string;
  generation: number;
  part: Part;
}

export interface SignupResponse {
  userId: number;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
}

export interface MyInfoResponse {
  userId: number;
  email: string;
  name: string;
  nickname: string;
  generation: number;
  part: Part;
}

export async function signup(request: SignupRequest): Promise<SignupResponse> {
  const response = await apiClient.post<ApiResponse<SignupResponse>>("/api/auth/signup", request);
  return response.data.data;
}

export async function login(request: LoginRequest): Promise<LoginResponse> {
  const response = await apiClient.post<ApiResponse<LoginResponse>>("/api/auth/login", request);
  return response.data.data;
}

export async function logout(): Promise<void> {
  await apiClient.post("/api/auth/logout");
}

export async function getMyInfo(): Promise<MyInfoResponse> {
  const response = await apiClient.get<ApiResponse<MyInfoResponse>>("/api/users/me");
  return response.data.data;
}
