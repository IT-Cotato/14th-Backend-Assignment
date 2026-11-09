import {
    test,
    expect,
    main,
    loginViaUI,
    uniqueId,
    postIdFromUrl,
    authHeader,
    API_URL,
} from './fixtures'

test.describe('B. 스터디 모집글', () => {
    test('모집글 목록을 조회하면 최신 글이 맨 위에 보여요', async ({ page, data }) => {
        const author = await data.createUser('PM')
        const title = `E2E 목록 ${uniqueId()}`
        await data.createPost(author, { title })

        await page.goto('/studies')
        await expect(main(page).getByRole('heading', { name: '스터디 모집글' })).toBeVisible()
        await expect(main(page).getByText(/총 \d+개/)).toBeVisible()

        const first = main(page).getByRole('listitem').first()
        await expect(first).toContainText(title)
        await expect(first).toContainText('모집 중')
        await expect(first).toContainText(author.nickname)
        await expect(first).toContainText('14기')
        await expect(first).toContainText('PM')
        await expect(first).toContainText('댓글 0')

        await first.getByRole('link').click()
        await expect(page).toHaveURL(/\/studies\/\d+$/)
        await expect(main(page).getByRole('heading', { name: title })).toBeVisible()
    })

    test('키워드로 제목과 내용을 검색해요', async ({ page, data }) => {
        const author = await data.createUser()
        const keyword = `kw${uniqueId()}`
        await data.createPost(author, { title: `E2E 제목 검색 ${keyword}` })
        await data.createPost(author, { title: 'E2E 내용 검색', content: `본문에 ${keyword} 포함` })
        await data.createPost(author, { title: `E2E 검색 제외 ${uniqueId()}` })

        await page.goto('/studies')
        await main(page).getByLabel('검색어').fill(keyword)
        await main(page).getByRole('button', { name: '검색' }).click()

        await expect(page).toHaveURL(new RegExp(`keyword=${keyword}`))
        await expect(main(page).getByRole('listitem')).toHaveCount(2)
        await expect(main(page).getByText('총 2개')).toBeVisible()
        await expect(main(page).getByText('E2E 검색 제외')).toHaveCount(0)

        // 결과가 없는 검색어는 빈 상태를 보여줘요.
        await main(page).getByLabel('검색어').fill(`${keyword}none`)
        await main(page).getByRole('button', { name: '검색' }).click()
        await expect(main(page).getByText('조건에 맞는 모집글이 없어요.')).toBeVisible()
    })

    test('모집 상태와 작성자 파트로 필터링해요', async ({ page, data }) => {
        const fe = await data.createUser('FE')
        const be = await data.createUser('BE')
        const keyword = `kw${uniqueId()}`
        await data.createPost(fe, { title: `E2E FE 모집중 ${keyword}` })
        await data.createPost(fe, { title: `E2E FE 마감 ${keyword}`, closed: true })
        await data.createPost(be, { title: `E2E BE 모집중 ${keyword}` })

        await page.goto(`/studies?keyword=${keyword}`)
        await expect(main(page).getByRole('listitem')).toHaveCount(3)

        await main(page).getByLabel('모집 상태').selectOption('CLOSED')
        await expect(page).toHaveURL(/status=CLOSED/)
        await expect(main(page).getByRole('listitem')).toHaveCount(1)
        await expect(main(page).getByRole('listitem')).toContainText('E2E FE 마감')
        await expect(main(page).getByRole('listitem')).toContainText('모집 마감')

        await main(page).getByLabel('작성자 파트').selectOption('BE')
        await expect(main(page).getByText('조건에 맞는 모집글이 없어요.')).toBeVisible()

        await main(page).getByLabel('모집 상태').selectOption('')
        await expect(main(page).getByRole('listitem')).toHaveCount(1)
        await expect(main(page).getByRole('listitem')).toContainText('E2E BE 모집중')

        await main(page).getByLabel('모집 상태').selectOption('RECRUITING')
        await main(page).getByLabel('작성자 파트').selectOption('FE')
        await expect(main(page).getByRole('listitem')).toHaveCount(1)
        await expect(main(page).getByRole('listitem')).toContainText('E2E FE 모집중')

        await main(page).getByRole('button', { name: '초기화' }).click()
        await expect(page).toHaveURL(/\/studies$/)
    })

    test('목록을 10개씩 페이지로 나눠서 보여줘요', async ({ page, data }) => {
        const author = await data.createUser()
        const keyword = `kw${uniqueId()}`
        for (let i = 1; i <= 11; i += 1) {
            await data.createPost(author, { title: `E2E 페이지 ${String(i).padStart(2, '0')} ${keyword}` })
        }

        await page.goto(`/studies?keyword=${keyword}`)
        await expect(main(page).getByText('총 11개')).toBeVisible()
        await expect(main(page).getByRole('listitem')).toHaveCount(10)
        await expect(main(page).getByRole('listitem').first()).toContainText('E2E 페이지 11')

        const pagination = main(page).getByRole('navigation', { name: '페이지 이동' })
        await expect(pagination.getByRole('button', { name: '이전' })).toBeDisabled()
        await pagination.getByRole('button', { name: '2', exact: true }).click()

        await expect(page).toHaveURL(/page=2/)
        await expect(main(page).getByRole('listitem')).toHaveCount(1)
        await expect(main(page).getByRole('listitem')).toContainText('E2E 페이지 01')
        await expect(pagination.getByRole('button', { name: '다음' })).toBeDisabled()
        await expect(pagination.getByRole('button', { name: '2', exact: true })).toHaveAttribute('aria-current', 'page')
    })

    test('로그인 사용자가 모집글을 작성하면 상세 페이지로 이동해요', async ({ page, data }) => {
        const author = await data.createUser('DE')
        await loginViaUI(page, author)

        await main(page).getByRole('link', { name: '+ 글 작성' }).click()
        await expect(page).toHaveURL(/\/studies\/new$/)

        // 빈 값은 화면에서 먼저 막아요.
        await main(page).getByRole('button', { name: '등록' }).click()
        await expect(main(page).getByText('제목을 입력해 주세요.')).toBeVisible()
        await expect(main(page).getByText('내용을 입력해 주세요.')).toBeVisible()

        const title = `E2E 작성 ${uniqueId()}`
        await main(page).getByLabel('제목').fill(title)
        await main(page).getByLabel('내용').fill('매주 토요일 알고리즘 스터디를 진행해요.\n두 번째 줄이에요.')
        await main(page).getByRole('button', { name: '등록' }).click()

        await expect(page).toHaveURL(/\/studies\/\d+$/)
        data.trackPost(postIdFromUrl(page.url()), author)

        await expect(main(page).getByRole('heading', { name: title })).toBeVisible()
        await expect(main(page).getByText('두 번째 줄이에요.')).toBeVisible()
        await expect(main(page).locator('article')).toContainText('모집 중')
        await expect(main(page).locator('article')).toContainText(author.nickname)
    })

    test('비로그인 사용자도 모집글 상세를 볼 수 있고 수정·삭제 버튼은 보이지 않아요', async ({ page, data }) => {
        const author = await data.createUser('BE')
        const title = `E2E 상세 ${uniqueId()}`
        const postId = await data.createPost(author, { title, content: '상세 조회 본문이에요.' })

        await page.goto(`/studies/${postId}`)
        const article = main(page).locator('article')
        await expect(article.getByRole('heading', { name: title })).toBeVisible()
        await expect(article).toContainText('상세 조회 본문이에요.')
        await expect(article).toContainText(author.nickname)
        await expect(article).toContainText('14기')
        await expect(article).toContainText('BE')
        await expect(article).toContainText(/작성 \d{4}\.\d{2}\.\d{2} \d{2}:\d{2}/)
        await expect(article.getByRole('link', { name: '수정' })).toHaveCount(0)
        await expect(article.getByRole('button', { name: '삭제' })).toHaveCount(0)

        // 없는 글은 404 안내를 보여줘요.
        await page.goto('/studies/999999999')
        await expect(main(page).getByText('존재하지 않는 모집글이에요.')).toBeVisible()
    })

    test('작성자는 모집글을 수정하고 모집 상태를 CLOSED로 바꿀 수 있어요', async ({ page, data }) => {
        const author = await data.createUser()
        const postId = await data.createPost(author, { title: `E2E 수정 전 ${uniqueId()}` })
        await loginViaUI(page, author)

        await page.goto(`/studies/${postId}`)
        await main(page).locator('article').getByRole('link', { name: '수정' }).click()
        await expect(page).toHaveURL(new RegExp(`/studies/${postId}/edit$`))

        const newTitle = `E2E 수정 후 ${uniqueId()}`
        await main(page).getByLabel('제목').fill(newTitle)
        await main(page).getByLabel('내용').fill('수정된 본문이에요.')
        await main(page).getByLabel('모집 상태').selectOption('CLOSED')
        await main(page).getByRole('button', { name: '수정 완료' }).click()

        await expect(page).toHaveURL(new RegExp(`/studies/${postId}$`))
        const article = main(page).locator('article')
        await expect(article.getByRole('heading', { name: newTitle })).toBeVisible()
        await expect(article).toContainText('수정된 본문이에요.')
        await expect(article).toContainText('모집 마감')
        // 마감된 글에는 댓글 작성 폼이 없어요.
        await expect(main(page).getByText('모집이 마감된 스터디라 댓글을 작성할 수 없어요.')).toBeVisible()
        await expect(main(page).getByLabel('신청 댓글 작성')).toHaveCount(0)
    })

    test('작성자는 확인 후 모집글을 삭제할 수 있어요', async ({ page, data }) => {
        const author = await data.createUser()
        const postId = await data.createPost(author, { title: `E2E 삭제 ${uniqueId()}` })
        await loginViaUI(page, author)
        await page.goto(`/studies/${postId}`)
        const deleteButton = main(page).locator('article').getByRole('button', { name: '삭제' })

        // 확인 창에서 취소하면 그대로 남아요.
        page.once('dialog', (dialog) => dialog.dismiss())
        await deleteButton.click()
        await expect(page).toHaveURL(new RegExp(`/studies/${postId}$`))
        await expect(deleteButton).toBeEnabled()

        page.once('dialog', (dialog) => {
            expect(dialog.message()).toContain('삭제할까요')
            void dialog.accept()
        })
        await deleteButton.click()
        await expect(page).toHaveURL(/\/studies$/)

        await page.goto(`/studies/${postId}`)
        await expect(main(page).getByText('존재하지 않는 모집글이에요.')).toBeVisible()
    })

    test('다른 사용자는 모집글을 수정·삭제할 수 없어요 (화면과 서버 모두)', async ({ page, data, request }) => {
        const author = await data.createUser()
        const other = await data.createUser()
        const title = `E2E 권한 ${uniqueId()}`
        const postId = await data.createPost(author, { title })
        await loginViaUI(page, other)

        await page.goto(`/studies/${postId}`)
        const article = main(page).locator('article')
        await expect(article.getByRole('heading', { name: title })).toBeVisible()
        await expect(article.getByRole('link', { name: '수정' })).toHaveCount(0)
        await expect(article.getByRole('button', { name: '삭제' })).toHaveCount(0)

        // 주소로 직접 들어가도 수정 화면을 열 수 없어요.
        await page.goto(`/studies/${postId}/edit`)
        await expect(main(page).getByText('수정 권한이 없어요.')).toBeVisible()
        await expect(main(page).getByLabel('제목')).toHaveCount(0)

        // 서버도 작성자를 검증해요. (403 NOT_POST_AUTHOR)
        const patch = await request.patch(`${API_URL}/study-posts/${postId}`, {
            headers: authHeader(other.token),
            data: { title: '탈취 시도' },
        })
        expect(patch.status()).toBe(403)
        const del = await request.delete(`${API_URL}/study-posts/${postId}`, { headers: authHeader(other.token) })
        expect(del.status()).toBe(403)

        await page.goto(`/studies/${postId}`)
        await expect(main(page).getByRole('heading', { name: title })).toBeVisible()
    })
})
