import { expectError } from './support/api.ts'
import { newPostTitle, uniqueTag } from './support/data.ts'
import { expect, loginAs, postIdFromUrl, test } from './support/fixtures.ts'

test.describe('스터디 모집글', () => {
  test('작성 → 목록 검색·필터 → 상세 → 수정 → 모집 마감 → 삭제', async ({ page, api, createUser, trackPost }) => {
    const author = await createUser({ part: 'BE' })
    await loginAs(page, author.token)

    const tag = uniqueTag()
    const title = newPostTitle(tag)
    const editedTitle = `${title} (수정)`
    let postId = 0

    await test.step('모집글을 작성하면 상세 화면으로 가요', async () => {
      await page.goto('/studies')
      await page.getByRole('link', { name: '모집글 작성' }).click()
      await expect(page.getByRole('heading', { name: '모집글 작성' })).toBeVisible()

      await page.getByLabel('제목').fill(title)
      await page.getByLabel('내용').fill(`주 2회 백준 풀이해요.\n태그: ${tag}`)
      await page.getByRole('button', { name: '등록', exact: true }).click()

      await expect(page).toHaveURL(/\/studies\/\d+$/)
      postId = postIdFromUrl(page)
      trackPost(postId, author.token)
      await expect(page.getByRole('heading', { level: 1, name: title, exact: true })).toBeVisible()
    })

    const postLink = (name: string) => page.getByRole('link', { name, exact: true })

    await test.step('목록에서 검색어로 찾아요', async () => {
      await page.goto('/studies')
      await page.getByLabel('검색어').fill(tag)
      await page.getByRole('button', { name: '검색', exact: true }).click()

      await expect(page).toHaveURL(new RegExp(`keyword=${tag}`))
      await expect(postLink(title)).toBeVisible()
    })

    await test.step('모집 상태·파트 필터를 함께 쓰고, 새로고침해도 유지돼요', async () => {
      await page.getByLabel('모집 상태').selectOption('RECRUITING')
      await page.getByLabel('파트').selectOption('BE')

      await expect(page).toHaveURL(/status=RECRUITING/)
      await expect(page).toHaveURL(/part=BE/)
      await expect(postLink(title)).toBeVisible()

      await page.reload()
      await expect(page.getByLabel('검색어')).toHaveValue(tag)
      await expect(page.getByLabel('모집 상태')).toHaveValue('RECRUITING')
      await expect(page.getByLabel('파트')).toHaveValue('BE')
      await expect(postLink(title)).toBeVisible()
    })

    await test.step('작성자 파트와 다른 파트로 거르면 나오지 않아요', async () => {
      await page.getByLabel('파트').selectOption('FE')

      await expect(page.getByText('조건에 맞는 모집글이 없어요.')).toBeVisible()
      await expect(postLink(title)).toHaveCount(0)
    })

    await test.step('목록에서 제목을 눌러 상세로 가요', async () => {
      await page.getByLabel('파트').selectOption('BE')
      await postLink(title).click()

      await expect(page).toHaveURL(new RegExp(`/studies/${postId}$`))
      await expect(page.getByRole('heading', { level: 1, name: title, exact: true })).toBeVisible()
    })

    await test.step('수정하면 상세에 새 제목이 보여요', async () => {
      await page.getByRole('link', { name: '수정', exact: true }).click()
      await expect(page.getByRole('heading', { name: '모집글 수정' })).toBeVisible()
      await expect(page.getByLabel('제목')).toHaveValue(title)

      await page.getByLabel('제목').fill(editedTitle)
      await page.getByLabel('내용').fill(`수정한 내용이에요. 태그: ${tag}`)
      await page.getByRole('button', { name: '저장', exact: true }).click()

      await expect(page).toHaveURL(new RegExp(`/studies/${postId}$`))
      await expect(page.getByRole('heading', { level: 1, name: editedTitle, exact: true })).toBeVisible()
      await expect(page.getByText(`수정한 내용이에요. 태그: ${tag}`)).toBeVisible()
    })

    await test.step('모집 마감하면 다시 모집 버튼으로 바뀌고, 마감 필터에서 보여요', async () => {
      await page.getByRole('button', { name: '모집 마감', exact: true }).click()
      await expect(page.getByRole('button', { name: '다시 모집', exact: true })).toBeVisible()

      await page.goto(`/studies?keyword=${tag}&status=CLOSED`)
      await expect(page.getByLabel('모집 상태')).toHaveValue('CLOSED')
      await expect(postLink(editedTitle)).toBeVisible()
    })

    await test.step('삭제를 확인하면 목록으로 가고 글이 사라져요', async () => {
      await page.goto(`/studies/${postId}`)
      await expect(page.getByRole('heading', { level: 1, name: editedTitle, exact: true })).toBeVisible()

      page.once('dialog', async (dialog) => {
        expect(dialog.type()).toBe('confirm')
        expect(dialog.message()).toContain('삭제할까요')
        await dialog.accept()
      })
      await page.getByRole('button', { name: '삭제', exact: true }).click()

      await expect(page).toHaveURL(/\/studies$/)
      await expectError(await api.getPost(postId), 404, 'STUDY-001')
    })
  })

  test('로그인하지 않으면 모집글을 작성할 수 없어요', async ({ page, api }) => {
    await page.goto('/studies')
    await expect(page.getByRole('heading', { name: '스터디 목록' })).toBeVisible()
    await expect(page.getByRole('link', { name: '모집글 작성' })).toHaveCount(0)

    await page.goto('/studies/new')
    await expect(page).toHaveURL(/\/login$/)

    // 토큰 없이 API로 작성해도 서버가 막아요
    await expectError(
      await api.createPostRaw(undefined, { title: newPostTitle(uniqueTag()), content: '내용' }),
      401,
      'COMMON-004',
    )
  })

  test('남의 글은 수정·삭제·마감할 수 없어요', async ({ page, api, createUser, trackPost }) => {
    const author = await createUser()
    const other = await createUser()
    const title = newPostTitle(uniqueTag())
    const postId = await api.createPost(author.token, { title, content: '작성자만 고칠 수 있어요' })
    trackPost(postId, author.token)

    await loginAs(page, other.token)

    await test.step('상세 화면에 작성자 버튼이 보이지 않아요', async () => {
      await page.goto(`/studies/${postId}`)
      await expect(page.getByRole('heading', { level: 1, name: title, exact: true })).toBeVisible()

      await expect(page.getByRole('link', { name: '수정', exact: true })).toHaveCount(0)
      await expect(page.getByRole('button', { name: '삭제', exact: true })).toHaveCount(0)
      await expect(page.getByRole('button', { name: '모집 마감', exact: true })).toHaveCount(0)
    })

    await test.step('수정 화면 주소로 직접 들어가면 상세로 돌려보내요', async () => {
      await page.goto(`/studies/${postId}/edit`)

      await expect(page).toHaveURL(new RegExp(`/studies/${postId}$`))
      await expect(page.getByText('작성자만 수정할 수 있어요.')).toBeVisible()
    })

    await test.step('API로 직접 수정·삭제해도 서버가 403으로 막아요', async () => {
      await expectError(
        await api.updatePost(postId, other.token, { title: '남이 바꾼 제목', studyStatus: 'CLOSED' }),
        403,
        'STUDY-002',
      )
      await expectError(await api.deletePost(postId, other.token), 403, 'STUDY-002')

      const res = await api.getPost(postId)
      expect(res.status()).toBe(200)
      const post = (await res.json()).data
      expect(post.title).toBe(title)
      expect(post.studyStatus).toBe('RECRUITING')
    })
  })
})
