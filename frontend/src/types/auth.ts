import type { Part } from './api'

export interface SignupRequest {
    email: string
    password: string
    name: string
    nickname: string
    generation: number
    part: Part
}

export interface SignupResponse {
    userId: number
}

export interface LoginRequest {
    email: string
    password: string
}

export interface LoginResponse {
    accessToken: string
    tokenType: string
}

// GET /api/users/me 응답이에요.
export interface User {
    userId: number
    email: string
    name: string
    nickname: string
    generation: number
    part: Part
}
