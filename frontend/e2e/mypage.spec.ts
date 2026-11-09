import { test, expect, header, main, loginViaUI, getStoredToken, authHeader, API_URL, PART_LABELS } from './fixtures'

test.describe('D. 마이페이지 및 로그아웃', () => {
    test('로그인 후 헤더에서 마이페이지로 이동해 내 정보를 볼 수 있어요', async ({ page, data }) => {
        const user = await data.createUser('DE')
        await loginViaUI(page, user)

        const meResponse = page.waitForResponse((res) => res.url().endsWith('/api/users/me'))
        await header(page).getByRole('link', { name: '마이페이지' }).click()
        await expect(page).toHaveURL(/\/mypage$/)
        // 화면에 들어올 때 실제 API를 다시 호출해요.
        expect((await meResponse).status()).toBe(200)

        const info = main(page).getByTestId('my-info')
        await expect(info.getByText('이름', { exact: true }).locator('..')).toContainText(user.name)
        await expect(info.getByText('이메일', { exact: true }).locator('..')).toContainText(user.email)
        await expect(info.getByText('닉네임', { exact: true }).locator('..')).toContainText(user.nickname)
        await expect(info.getByText('기수', { exact: true }).locator('..')).toContainText('14기')
        await expect(info.getByText('파트', { exact: true }).locator('..')).toContainText(PART_LABELS[user.part])
        await expect(main(page).getByRole('heading', { name: user.nickname })).toBeVisible()
    })

    test('로그아웃하면 로그인 화면으로 이동하고 토큰이 지워져요', async ({ page, data }) => {
        const user = await data.createUser()
        await loginViaUI(page, user, '/login')
        await page.goto('/mypage')

        const logoutResponse = page.waitForResponse((res) => res.url().endsWith('/api/auth/logout'))
        await main(page).getByRole('button', { name: '로그아웃' }).click()
        expect((await logoutResponse).status()).toBe(204)

        await expect(page).toHaveURL(/\/login$/)
        await expect(main(page).getByText('로그아웃했어요.')).toBeVisible()
        await expect(header(page).getByRole('link', { name: '로그인' })).toBeVisible()
        expect(await getStoredToken(page)).toBeNull()
    })

    test('헤더의 로그아웃 버튼도 서버 로그아웃 후 로그인 화면으로 이동해요', async ({ page, data }) => {
        const user = await data.createUser()
        await loginViaUI(page, user)

        const logoutResponse = page.waitForResponse((res) => res.url().endsWith('/api/auth/logout'))
        await header(page).getByRole('button', { name: '로그아웃' }).click()
        expect((await logoutResponse).status()).toBe(204)
        await expect(page).toHaveURL(/\/login$/)
        await expect(main(page).getByText('로그아웃했어요.')).toBeVisible()
    })

    test('로그아웃 후에는 보호 페이지에 들어갈 수 없어요', async ({ page, data }) => {
        const user = await data.createUser()
        await loginViaUI(page, user)
        await header(page).getByRole('button', { name: '로그아웃' }).click()
        await expect(page).toHaveURL(/\/login$/)

        for (const path of ['/mypage', '/studies/new']) {
            await page.goto(path)
            await expect(page).toHaveURL(/\/login$/)
            await expect(main(page).getByText('로그인이 필요한 페이지예요.')).toBeVisible()
        }
    })

    test('로그아웃한 JWT는 다시 쓸 수 없어요', async ({ page, data, request }) => {
        const user = await data.createUser()
        await loginViaUI(page, user)
        // 값은 메모리에서만 비교하고 출력하지 않아요.
        const token = await getStoredToken(page)
        expect(token).not.toBeNull()

        await header(page).getByRole('button', { name: '로그아웃' }).click()
        await expect(page).toHaveURL(/\/login$/)

        // 서버가 무효화한 토큰이라 직접 요청해도 401이에요.
        const me = await request.get(`${API_URL}/users/me`, { headers: authHeader(token!) })
        expect(me.status()).toBe(401)
        expect((await me.json()).code).toBe('COMMON-004')

        // 브라우저에 같은 토큰을 다시 넣어도 로그인 상태가 되지 않아요.
        await page.evaluate((value) => localStorage.setItem('accessToken', value), token!)
        await page.goto('/mypage')
        await expect(page).toHaveURL(/\/login$/)
        await expect(header(page).getByRole('link', { name: '로그인' })).toBeVisible()
        expect(await getStoredToken(page)).toBeNull()
    })
})
