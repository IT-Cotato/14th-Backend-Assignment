import { test as base, expect, type Page } from '@playwright/test'
import { ApiClient } from './api.ts'
import { newUser, type TestUser } from './data.ts'
import { API_URL } from './env.ts'

export interface LoggedInUser extends TestUser {
  token: string
}

interface E2eFixtures {
  api: ApiClient
  // API로 새 계정을 만들고 로그인까지 해서 토큰을 돌려줘요
  createUser: (overrides?: Partial<TestUser>) => Promise<LoggedInUser>
  // 테스트가 만든 모집글을 등록하면 테스트가 끝날 때 지워요 (댓글도 함께 지워져요)
  trackPost: (postId: number, token: string) => void
}

// 픽스처의 두 번째 인자를 use로 부르면 react-hooks 린트가 훅으로 오인해서 provide로 이름 지었어요
export const test = base.extend<E2eFixtures>({
  api: async ({ playwright }, provide) => {
    const context = await playwright.request.newContext({ baseURL: API_URL })
    await provide(new ApiClient(context))
    await context.dispose()
  },

  createUser: async ({ api }, provide) => {
    await provide(async (overrides) => {
      const user = newUser(overrides)
      const res = await api.signup(user)
      expect(res.status(), '테스트 준비: 회원가입').toBe(201)
      const token = await api.login(user.email, user.password)
      return { ...user, token }
    })
  },

  trackPost: async ({ api }, provide) => {
    const tracked: { postId: number; token: string }[] = []
    await provide((postId, token) => {
      tracked.push({ postId, token })
    })
    for (const { postId, token } of tracked) {
      const res = await api.deletePost(postId, token)
      // 테스트 안에서 이미 지운 글이면 404라 괜찮아요
      if (res.status() !== 204 && res.status() !== 404) {
        throw new Error(`테스트 정리: 모집글 ${postId} 삭제 실패 (${res.status()})`)
      }
    }
  },
})

export { expect }

// 로그인 화면을 거치지 않고 로그인한 상태로 시작해요. (로그인 자체가 시나리오가 아닌 테스트용)
// 앱이 localStorage의 accessToken을 읽어 로그인 상태를 복원하는 걸 이용해요. 페이지를 열 때마다 다시 넣어져요
export async function loginAs(page: Page, token: string): Promise<void> {
  await page.addInitScript({ content: `window.localStorage.setItem('accessToken', ${JSON.stringify(token)});` })
}

// 주소(/studies/12)에서 모집글 id를 꺼내요
export function postIdFromUrl(page: Page): number {
  const match = new URL(page.url()).pathname.match(/^\/studies\/(\d+)$/)
  if (!match) throw new Error(`모집글 상세 주소가 아니에요: ${page.url()}`)
  return Number(match[1])
}
