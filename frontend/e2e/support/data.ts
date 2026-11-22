import { randomUUID } from 'node:crypto'

// 비밀번호를 코드에 고정하지 않고 실행할 때마다 새로 만들어요 (8자 이상 규칙 충족)
export const TEST_PASSWORD = `E2e-${randomUUID()}`

export type TestPart = 'PM' | 'DE' | 'BE' | 'FE'

export interface TestUser {
  email: string
  password: string
  name: string
  nickname: string
  generation: number
  part: TestPart
}

function randomId(length: number): string {
  return randomUUID().replaceAll('-', '').slice(0, length)
}

// 검색어·제목에 넣을 이번 테스트만의 고유 문자열
export function uniqueTag(): string {
  return `${Date.now().toString(36)}${randomId(4)}`
}

// 테스트마다 새 계정. 이메일은 e2e-로 시작하고, 닉네임은 10자 제한 안(e2e + 6자)이에요
export function newUser(overrides: Partial<TestUser> = {}): TestUser {
  const id = randomId(6)
  return {
    email: `e2e-${Date.now()}-${id}@example.com`,
    password: TEST_PASSWORD,
    name: '테스트',
    nickname: `e2e${id}`,
    generation: 14,
    part: 'BE',
    ...overrides,
  }
}

// 제목은 50자 이하
export function newPostTitle(tag: string): string {
  return `E2E ${tag} 스터디 모집`
}
