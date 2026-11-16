import { randomUUID } from 'node:crypto'
import type { Part } from '../../src/api/auth'

export interface TestUser {
  email: string
  password: string
  name: string
  nickname: string
  generation: number
  part: Part
}

// 테스트 데이터(닉네임, 글 제목, 댓글 등)를 매번 다르게 만들 때 쓰는 랜덤 문자열
export function uniqueSuffix(length = 6): string {
  return randomUUID().replace(/-/g, '').slice(0, length)
}

// 테스트마다 새 사용자 정보를 만든다. 이메일은 e2e-로 시작하고, 비밀번호는 실행할 때 만든다.
export function createTestUser(overrides: Partial<TestUser> = {}): TestUser {
  const suffix = uniqueSuffix()
  return {
    email: `e2e-${Date.now()}-${suffix}@test.com`,
    password: randomUUID(),
    name: 'E2E 사용자',
    nickname: `e2e${suffix}`, // 닉네임은 2~10자
    generation: 14,
    part: 'BE',
    ...overrides,
  }
}
