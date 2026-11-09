import type { Page } from '@playwright/test'
import { test, expect, header, main, loginViaUI, getStoredToken, authHeader, API_URL } from './fixtures'

// 의도적인 장애 상황만 page.route로 만들어요. 나머지 요청은 모두 실제 서버로 가요.
const LOGOUT_URL = '**/api/auth/logout'

const UNCONFIRMED = '서버에서 로그아웃(토큰 무효화)을 확인하지 못했어요.'

async function failLogoutWith500(page: Page) {
    await page.route(LOGOUT_URL, (route) =>
        route.fulfill({
            status: 500,
            contentType: 'application/json',
            body: JSON.stringify({
                success: false,
                status: 500,
                code: 'COMMON-001',
                message: '서버 내부 오류가 발생했습니다.',
                method: 'POST',
                path: '/api/auth/logout',
                timestamp: '2026-11-08 12:00:00',
            }),
        }),
    )
}

async function failLogoutWithNetworkError(page: Page) {
    await page.route(LOGOUT_URL, (route) => route.abort('internetdisconnected'))
}

test.describe('E. 로그아웃 실패 처리', () => {
    test('서버가 500을 돌려주면 성공으로 표시하지 않고 로그인 상태와 토큰을 유지해요', async ({
        page,
        data,
        request,
    }) => {
        const user = await data.createUser()
        await loginViaUI(page, user)
        await page.goto('/mypage')
        const token = await getStoredToken(page)
        await failLogoutWith500(page)

        await main(page).getByRole('button', { name: '로그아웃' }).click()

        const alert = main(page).getByRole('alert')
        await expect(alert).toContainText(UNCONFIRMED)
        await expect(page).toHaveURL(/\/mypage$/)
        await expect(header(page).getByText(user.nickname)).toBeVisible()
        await expect(main(page).getByText('로그아웃했어요.')).toHaveCount(0)
        expect(await getStoredToken(page)).toBe(token)

        // 실제 서버에서도 토큰이 아직 유효해요. (무효화되지 않았는데 성공이라고 하지 않았어요)
        const me = await request.get(`${API_URL}/users/me`, { headers: authHeader(token!) })
        expect(me.status()).toBe(200)

        // 장애가 풀린 뒤 다시 시도하면 실제 서버 로그아웃이 성공해요.
        await page.unroute(LOGOUT_URL)
        const retry = page.waitForResponse((res) => res.url().endsWith('/api/auth/logout'))
        await alert.getByRole('button', { name: '로그아웃 다시 시도' }).click()
        expect((await retry).status()).toBe(204)

        await expect(page).toHaveURL(/\/login$/)
        await expect(main(page).getByText('로그아웃했어요.')).toBeVisible()
        expect(await getStoredToken(page)).toBeNull()
        const after = await request.get(`${API_URL}/users/me`, { headers: authHeader(token!) })
        expect(after.status()).toBe(401)
    })

    test('재시도도 실패하면 안내를 유지해요', async ({ page, data }) => {
        const user = await data.createUser()
        await loginViaUI(page, user)
        await page.goto('/mypage')
        await failLogoutWith500(page)

        await main(page).getByRole('button', { name: '로그아웃' }).click()
        const alert = main(page).getByRole('alert')
        await expect(alert).toContainText(UNCONFIRMED)

        const retry = page.waitForResponse((res) => res.url().endsWith('/api/auth/logout'))
        await alert.getByRole('button', { name: '로그아웃 다시 시도' }).click()
        expect((await retry).status()).toBe(500)
        await expect(main(page).getByRole('alert')).toContainText(UNCONFIRMED)
        await expect(page).toHaveURL(/\/mypage$/)
        expect(await getStoredToken(page)).not.toBeNull()

        // 정리: 실제 서버 로그아웃으로 토큰을 무효화해요.
        await page.unroute(LOGOUT_URL)
        await main(page).getByRole('alert').getByRole('button', { name: '로그아웃 다시 시도' }).click()
        await expect(page).toHaveURL(/\/login$/)
    })

    test('네트워크 오류일 때 "이 기기에서만 로그아웃"을 고르면 서버 무효화가 안 됐다고 알려줘요', async ({
        page,
        data,
        request,
    }) => {
        const user = await data.createUser()
        await loginViaUI(page, user)
        await page.goto('/mypage')
        const token = await getStoredToken(page)
        await failLogoutWithNetworkError(page)

        await main(page).getByRole('button', { name: '로그아웃' }).click()
        const alert = main(page).getByRole('alert')
        await expect(alert).toContainText(UNCONFIRMED)
        expect(await getStoredToken(page)).toBe(token)

        await alert.getByRole('button', { name: '이 기기에서만 로그아웃' }).click()
        await expect(page).toHaveURL(/\/login$/)
        await expect(main(page).getByText('이 기기에서 로그아웃했어요.', { exact: false })).toContainText(
            '서버에서 토큰 무효화는 확인하지 못해서',
        )
        await expect(main(page).getByText('로그아웃했어요.', { exact: true })).toHaveCount(0)
        await expect(header(page).getByRole('link', { name: '로그인' })).toBeVisible()
        expect(await getStoredToken(page)).toBeNull()

        // 서버 로그아웃 성공과 달리, 서버에서는 토큰이 아직 유효해요.
        const me = await request.get(`${API_URL}/users/me`, { headers: authHeader(token!) })
        expect(me.status()).toBe(200)

        // 정리: 남은 테스트 토큰을 실제 서버에서 무효화해요.
        const cleanup = await request.post(`${API_URL}/auth/logout`, { headers: authHeader(token!) })
        expect(cleanup.status()).toBe(204)
    })

    test('헤더에서 로그아웃이 실패하면 마이페이지로 이동해 안내해요', async ({ page, data }) => {
        const user = await data.createUser()
        await loginViaUI(page, user)
        await failLogoutWithNetworkError(page)

        await header(page).getByRole('button', { name: '로그아웃' }).click()
        await expect(page).toHaveURL(/\/mypage$/)
        await expect(main(page).getByRole('alert')).toContainText(UNCONFIRMED)
        await expect(header(page).getByText(user.nickname)).toBeVisible()

        // 마이페이지에 있는 상태에서 헤더 로그아웃이 다시 실패해도 안내가 다시 보여요.
        await main(page).getByRole('alert').getByRole('button', { name: '로그아웃 다시 시도' }).click()
        await expect(main(page).getByRole('alert')).toContainText(UNCONFIRMED)

        await page.unroute(LOGOUT_URL)
        await header(page).getByRole('button', { name: '로그아웃' }).click()
        await expect(page).toHaveURL(/\/login$/)
        await expect(main(page).getByText('로그아웃했어요.')).toBeVisible()
    })

    test('토큰이 이미 서버에서 무효화된 경우(401)는 만료 안내와 함께 로그아웃 상태가 돼요', async ({
        page,
        data,
        request,
    }) => {
        const user = await data.createUser()
        await loginViaUI(page, user)
        const token = await getStoredToken(page)

        // 다른 곳에서 같은 토큰으로 이미 로그아웃했어요. (실제 서버)
        const revoke = await request.post(`${API_URL}/auth/logout`, { headers: authHeader(token!) })
        expect(revoke.status()).toBe(204)

        const logoutResponse = page.waitForResponse((res) => res.url().endsWith('/api/auth/logout'))
        await header(page).getByRole('button', { name: '로그아웃' }).click()
        expect((await logoutResponse).status()).toBe(401)

        await expect(page).toHaveURL(/\/login$/)
        await expect(main(page).getByText('로그인 정보가 이미 만료되어 로그아웃 상태로 전환했어요.')).toBeVisible()
        expect(await getStoredToken(page)).toBeNull()
    })
})
