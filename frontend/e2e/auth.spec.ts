import { test, expect, header, main, loginViaUI, getStoredToken } from './fixtures'

test.describe('A. 회원가입 및 로그인', () => {
    test('신규 회원가입 후 로그인 화면으로 이동해요', async ({ page, data }) => {
        const info = data.newUserInfo('BE')
        await page.goto('/signup')

        const form = main(page)
        await form.getByLabel('이메일').fill(info.email)
        await form.getByLabel('비밀번호', { exact: true }).fill(info.password)
        await form.getByLabel('비밀번호 확인').fill(info.password)
        await form.getByLabel('이름').fill(info.name)
        await form.getByLabel('닉네임').fill(info.nickname)
        await form.getByLabel('기수').fill(String(info.generation))
        await form.getByLabel('파트').selectOption('BE')
        await form.getByRole('button', { name: '회원가입' }).click()

        await expect(page).toHaveURL(/\/login$/)
        await expect(main(page).getByText('회원가입이 완료됐어요. 로그인해 주세요.')).toBeVisible()

        // 가입한 계정으로 실제 로그인까지 돼요.
        await loginViaUI(page, info, '')
        await expect(page).toHaveURL(/\/studies$/)
    })

    test('회원가입 입력값 오류와 중복 이메일을 안내해요', async ({ page, data }) => {
        const existing = await data.createUser()
        await page.goto('/signup')
        const form = main(page)

        await form.getByRole('button', { name: '회원가입' }).click()
        await expect(form.getByText('올바른 이메일 형식으로 입력해 주세요.')).toBeVisible()
        await expect(form.getByText('파트를 선택해 주세요.')).toBeVisible()

        await form.getByLabel('이메일').fill(existing.email)
        await form.getByLabel('비밀번호', { exact: true }).fill('password1234')
        await form.getByLabel('비밀번호 확인').fill('password1234')
        await form.getByLabel('이름').fill('중복테스터')
        await form.getByLabel('닉네임').fill('중복닉네임')
        await form.getByLabel('파트').selectOption('FE')
        await form.getByRole('button', { name: '회원가입' }).click()

        // 서버의 409 EMAIL_ALREADY_EXISTS 메시지를 보여줘요.
        await expect(form.getByRole('alert')).toBeVisible()
        await expect(page).toHaveURL(/\/signup$/)
    })

    test('정상 로그인하면 목록으로 이동하고 헤더에 닉네임이 보여요', async ({ page, data }) => {
        const user = await data.createUser()
        await loginViaUI(page, user)

        await expect(page).toHaveURL(/\/studies$/)
        await expect(header(page).getByRole('link', { name: '마이페이지' })).toBeVisible()
        await expect(header(page).getByRole('button', { name: '로그아웃' })).toBeVisible()
        await expect(header(page).getByRole('link', { name: '로그인' })).toHaveCount(0)
        expect(await getStoredToken(page)).not.toBeNull()
    })

    test('잘못된 비밀번호로는 로그인할 수 없어요', async ({ page, data }) => {
        const user = await data.createUser()
        await page.goto('/login')

        await main(page).getByLabel('이메일').fill(user.email)
        await main(page).getByLabel('비밀번호', { exact: true }).fill('wrong-password-123')
        await main(page).getByRole('button', { name: '로그인', exact: true }).click()

        await expect(main(page).getByRole('alert')).toHaveText('이메일 또는 비밀번호가 올바르지 않습니다.')
        await expect(page).toHaveURL(/\/login$/)
        await expect(header(page).getByRole('link', { name: '로그인' })).toBeVisible()
        expect(await getStoredToken(page)).toBeNull()
    })

    test('새로고침해도 로그인 상태가 유지돼요', async ({ page, data }) => {
        const user = await data.createUser()
        await loginViaUI(page, user)

        await page.reload()
        await expect(header(page).getByText(user.nickname)).toBeVisible()

        // 새 탭(같은 브라우저 컨텍스트)에서도 유지돼요.
        const second = await page.context().newPage()
        await second.goto('/mypage')
        await expect(second.getByTestId('my-info')).toContainText(user.email)
        await second.close()
    })

    test('비로그인 사용자는 보호 페이지에 들어갈 수 없고, 로그인 후 원래 페이지로 돌아가요', async ({ page, data }) => {
        for (const path of ['/studies/new', '/mypage', '/studies/1/edit']) {
            await page.goto(path)
            await expect(page).toHaveURL(/\/login$/)
            await expect(main(page).getByText('로그인이 필요한 페이지예요.')).toBeVisible()
        }

        const user = await data.createUser()
        await page.goto('/studies/new')
        await loginViaUI(page, user, '')
        await expect(page).toHaveURL(/\/studies\/new$/)
        await expect(main(page).getByRole('heading', { name: '스터디 모집글 작성' })).toBeVisible()
    })
})
