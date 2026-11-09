import { test as base, expect, type APIRequestContext, type Page } from '@playwright/test'

// 테스트 준비·정리용으로 실제 백엔드를 직접 호출해요. (Mock 아님)
export const API_URL = 'http://localhost:8080/api'

export type Part = 'PM' | 'DE' | 'BE' | 'FE'

export const PART_LABELS: Record<Part, string> = {
    PM: '기획(PM)',
    DE: '디자인(DE)',
    BE: '백엔드(BE)',
    FE: '프론트엔드(FE)',
}

export interface TestUser {
    email: string
    password: string
    name: string
    nickname: string
    generation: number
    part: Part
    // 준비 단계에서 받은 토큰이에요. 화면이나 로그에 출력하지 않아요.
    token: string
}

// 로컬 개발 DB 전용 테스트 계정 비밀번호예요. 실제 계정에는 쓰지 않아요.
// 필요하면 E2E_POOL_PASSWORD 환경 변수로 바꿀 수 있어요.
const POOL_PASSWORD = process.env.E2E_POOL_PASSWORD ?? 'e2e-pool-password'

let counter = 0

// 실행마다 겹치지 않는 짧은 식별자예요.
export function uniqueId(): string {
    counter += 1
    return (Date.now().toString(36).slice(-5) + counter.toString(36) + Math.random().toString(36).slice(2, 4))
        .toLowerCase()
}

export function authHeader(token: string) {
    return { Authorization: `Bearer ${token}` }
}

export class TestData {
    private readonly posts: { postId: number; owner: TestUser }[] = []

    constructor(private readonly request: APIRequestContext) {}

    // 회원가입 정보만 만들고 가입은 하지 않아요. (UI 회원가입 테스트용)
    newUserInfo(part: Part = 'FE'): Omit<TestUser, 'token'> {
        const id = uniqueId()
        return {
            email: `e2e.${id}@example.com`,
            password: `e2ePass-${id}`,
            name: 'E2E테스터',
            nickname: `e2e${id}`.slice(0, 10),
            generation: 14,
            part,
        }
    }

    async login(email: string, password: string): Promise<string> {
        const res = await this.request.post(`${API_URL}/auth/login`, { data: { email, password } })
        expect(res.status(), '테스트 계정 로그인').toBe(200)
        return (await res.json()).data.accessToken as string
    }

    // 테스트 안에서 파트별로 몇 번째 계정을 썼는지 세요. (같은 테스트 안에서는 서로 다른 계정이 필요해요)
    private readonly poolUsage = new Map<Part, number>()

    // 고정된 테스트 계정 풀에서 계정을 꺼내요. 실행할 때마다 새 계정을 만들지 않아서 DB에 계정이 쌓이지 않아요.
    // 처음 실행할 때만 가입하고, 이후에는 로그인만 해요.
    // 계정 정보를 바꾸는 API가 없고 테스트가 만든 모집글·댓글은 정리되니, 테스트끼리 계정을 같이 써도 안전해요.
    async createUser(part: Part = 'FE'): Promise<TestUser> {
        const index = (this.poolUsage.get(part) ?? 0) + 1
        this.poolUsage.set(part, index)
        const info: Omit<TestUser, 'token'> = {
            email: `e2e.pool.${part.toLowerCase()}${index}@example.com`,
            password: POOL_PASSWORD,
            name: 'E2E테스터',
            nickname: `e2e${part}${index}`,
            generation: 14,
            part,
        }

        const login = await this.request.post(`${API_URL}/auth/login`, {
            data: { email: info.email, password: info.password },
        })
        if (login.status() === 200) {
            return { ...info, token: (await login.json()).data.accessToken as string }
        }
        expect(login.status(), '테스트 계정 로그인').toBe(401)

        const signup = await this.request.post(`${API_URL}/auth/signup`, { data: info })
        expect(signup.status(), '테스트 계정 최초 가입').toBe(201)
        return { ...info, token: await this.login(info.email, info.password) }
    }

    // 직접 만든 모집글만 정리 대상으로 기록해요.
    trackPost(postId: number, owner: TestUser): void {
        this.posts.push({ postId, owner })
    }

    async createPost(
        owner: TestUser,
        { title, content = 'E2E 테스트용 모집글이에요.', closed = false }: { title: string; content?: string; closed?: boolean },
    ): Promise<number> {
        const res = await this.request.post(`${API_URL}/study-posts`, {
            headers: authHeader(owner.token),
            data: { title, content },
        })
        expect(res.status(), '테스트 모집글 작성').toBe(201)
        const postId = (await res.json()).data.postId as number
        this.trackPost(postId, owner)
        if (closed) await this.closePost(owner, postId)
        return postId
    }

    async closePost(owner: TestUser, postId: number): Promise<void> {
        const res = await this.request.patch(`${API_URL}/study-posts/${postId}`, {
            headers: authHeader(owner.token),
            data: { studyStatus: 'CLOSED' },
        })
        expect(res.status(), '테스트 모집글 마감').toBe(200)
    }

    async createComment(user: TestUser, postId: number, content: string): Promise<number> {
        const res = await this.request.post(`${API_URL}/study-posts/${postId}/comments`, {
            headers: authHeader(user.token),
            data: { content },
        })
        expect(res.status(), '테스트 댓글 작성').toBe(201)
        return (await res.json()).data.commentId as number
    }

    // 테스트가 만든 모집글만 지워요. 댓글은 모집글과 함께 삭제돼요.
    // 테스트 중 로그아웃으로 토큰이 무효화됐을 수 있어서 새로 로그인해서 지워요.
    async cleanup(): Promise<void> {
        const failures: string[] = []
        for (const { postId, owner } of this.posts.reverse()) {
            const token = await this.login(owner.email, owner.password)
            const res = await this.request.delete(`${API_URL}/study-posts/${postId}`, {
                headers: authHeader(token),
            })
            // 테스트에서 이미 삭제했으면 404예요.
            if (res.status() !== 204 && res.status() !== 404) failures.push(`post ${postId}: ${res.status()}`)
        }
        expect(failures, '테스트 데이터 정리').toEqual([])
    }
}

export const test = base.extend<{ data: TestData }>({
    // Playwright의 fixture 콜백이에요. 이름이 use면 React Hook 규칙으로 오인돼서 provide로 받아요.
    data: async ({ request }, provide) => {
        const data = new TestData(request)
        await provide(data)
        await data.cleanup()
    },
})

export { expect }

// 헤더와 본문에 같은 이름의 버튼이 있어서 영역을 나눠서 찾아요.
export const header = (page: Page) => page.getByRole('banner')
export const main = (page: Page) => page.getByRole('main')

export async function loginViaUI(page: Page, user: Pick<TestUser, 'email' | 'password' | 'nickname'>, path = '/login') {
    if (path) await page.goto(path)
    await main(page).getByLabel('이메일').fill(user.email)
    await main(page).getByLabel('비밀번호', { exact: true }).fill(user.password)
    await main(page).getByRole('button', { name: '로그인', exact: true }).click()
    await expect(header(page).getByText(user.nickname)).toBeVisible()
}

export async function getStoredToken(page: Page): Promise<string | null> {
    return page.evaluate(() => localStorage.getItem('accessToken'))
}

export function postIdFromUrl(url: string): number {
    const match = /\/studies\/(\d+)/.exec(url)
    if (!match) throw new Error(`모집글 주소가 아니에요: ${url}`)
    return Number(match[1])
}
