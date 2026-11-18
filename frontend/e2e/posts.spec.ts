import { expect, test } from '@playwright/test'
import {
  closePostViaApi,
  createPostViaApi,
  createUserAndLogin,
  loginViaUi,
  newUser,
  signupViaApi,
  uniqueTitle,
} from './helpers.ts'

test.describe('스터디 모집글', () => {
  test('로그인한 사용자가 글을 쓰면 상세 화면에서 내용을 볼 수 있다', async ({ page, request }) => {
    await createUserAndLogin(page, request)
    const title = uniqueTitle('알고리즘 스터디')

    await page.getByRole('link', { name: '글쓰기' }).click()
    await page.getByLabel('제목').fill(title)
    await page.getByLabel('내용').fill('주 2회 백준 문제를 풀어요.')
    await page.getByRole('button', { name: '등록하기' }).click()

    await expect(page.getByRole('heading', { name: title })).toBeVisible()
    await expect(page.getByText('주 2회 백준 문제를 풀어요.')).toBeVisible()
    await expect(page.getByText('모집 중')).toBeVisible()
  })

  test('제목을 비우고 등록하면 안내가 보이고 글이 만들어지지 않는다', async ({ page, request }) => {
    await createUserAndLogin(page, request)

    await page.goto('/studies/new')
    await page.getByLabel('내용').fill('내용만 있어요.')
    await page.getByRole('button', { name: '등록하기' }).click()

    await expect(page.getByRole('alert')).toHaveText('제목과 내용을 입력해 주세요.')
    await expect(page).toHaveURL('/studies/new')
  })

  test('로그인하지 않으면 글쓰기 버튼이 없고, 글쓰기 주소로 가면 로그인 화면이 나온다', async ({ page }) => {
    await page.goto('/studies')
    await expect(page.getByRole('heading', { name: '스터디 모집글' })).toBeVisible()
    await expect(page.getByRole('link', { name: '글쓰기' })).toHaveCount(0)

    await page.goto('/studies/new')
    await expect(page).toHaveURL('/login')
  })

  test('검색어로 찾으면 해당 글만 목록에 나온다', async ({ page, request }) => {
    const user = newUser()
    await signupViaApi(request, user)
    const target = uniqueTitle('검색대상')
    const other = uniqueTitle('다른글')
    await createPostViaApi(request, user, target)
    await createPostViaApi(request, user, other)

    await page.goto('/studies')
    await page.getByLabel('검색어').fill(target)
    await page.getByRole('button', { name: '검색' }).click()

    await expect(page.getByRole('link', { name: new RegExp(target) })).toBeVisible()
    await expect(page.getByText(other)).toHaveCount(0)
  })

  test('파트 필터를 고르면 그 파트 작성자의 글만 목록에 나온다', async ({ page, request }) => {
    const backend = newUser('BE')
    const frontend = newUser('FE')
    await signupViaApi(request, backend)
    await signupViaApi(request, frontend)
    const keyword = uniqueTitle('파트필터')
    await createPostViaApi(request, backend, `${keyword} 백엔드글`)
    await createPostViaApi(request, frontend, `${keyword} 프론트글`)

    await page.goto('/studies')
    await page.getByLabel('검색어').fill(keyword)
    await page.getByLabel('작성자 파트').selectOption('FE')

    await expect(page.getByRole('link', { name: /프론트글/ })).toBeVisible()
    await expect(page.getByText('백엔드글')).toHaveCount(0)
  })

  test('조건에 맞는 글이 없으면 안내 문구가 보인다', async ({ page }) => {
    await page.goto('/studies')
    await page.getByLabel('검색어').fill(uniqueTitle('절대없는검색어'))
    await page.getByRole('button', { name: '검색' }).click()

    await expect(page.getByText('조건에 맞는 모집글이 없어요.')).toBeVisible()
  })

  test('작성자가 모집을 마감하면 마감 상태로 보이고 상태 필터로 찾을 수 있다', async ({ page, request }) => {
    const user = await createUserAndLogin(page, request)
    const title = uniqueTitle('마감할글')
    const postId = await createPostViaApi(request, user, title)

    await page.goto(`/studies/${postId}`)
    await page.getByRole('button', { name: '모집 마감하기' }).click()
    await expect(page.getByRole('button', { name: '모집 다시 열기' })).toBeVisible()
    await expect(page.getByText('모집 마감', { exact: true })).toBeVisible()

    await page.goto('/studies')
    await page.getByLabel('검색어').fill(title)
    await page.getByLabel('모집 상태').selectOption('CLOSED')
    await expect(page.getByRole('link', { name: new RegExp(title) })).toBeVisible()

    await page.getByLabel('모집 상태').selectOption('RECRUITING')
    await expect(page.getByText('조건에 맞는 모집글이 없어요.')).toBeVisible()
  })

  test('작성자가 글을 수정하면 상세 화면에 바뀐 제목이 보인다', async ({ page, request }) => {
    const user = await createUserAndLogin(page, request)
    const postId = await createPostViaApi(request, user, uniqueTitle('수정전'))
    const newTitle = uniqueTitle('수정후')

    await page.goto(`/studies/${postId}`)
    await page.getByRole('link', { name: '수정' }).click()
    await page.getByLabel('제목').fill(newTitle)
    await page.getByRole('button', { name: '수정하기' }).click()

    await expect(page.getByRole('heading', { name: newTitle })).toBeVisible()
    await expect(page.getByText('(수정됨)')).toBeVisible()
  })

  test('수정 화면에서 모집 상태를 마감으로 바꾸면 상세 화면에 마감으로 보인다', async ({ page, request }) => {
    const user = await createUserAndLogin(page, request)
    const postId = await createPostViaApi(request, user, uniqueTitle('상태변경'))

    await page.goto(`/studies/${postId}/edit`)
    await page.getByLabel('모집 상태').selectOption('CLOSED')
    await page.getByRole('button', { name: '수정하기' }).click()

    await expect(page).toHaveURL(`/studies/${postId}`)
    await expect(page.getByText('모집 마감', { exact: true })).toBeVisible()
    await expect(page.getByRole('button', { name: '모집 다시 열기' })).toBeVisible()
  })

  test('작성자가 글을 삭제하면 목록으로 돌아가고 그 글은 더 이상 열리지 않는다', async ({ page, request }) => {
    const user = await createUserAndLogin(page, request)
    const postId = await createPostViaApi(request, user, uniqueTitle('삭제할글'))

    await page.goto(`/studies/${postId}`)
    page.once('dialog', (dialog) => dialog.accept())
    await page.locator('article').getByRole('button', { name: '삭제' }).click()
    await expect(page).toHaveURL('/studies')

    await page.goto(`/studies/${postId}`)
    await expect(page.getByRole('alert')).toHaveText('존재하지 않는 스터디 모집글이에요.')
  })

  test('남의 글에는 수정·삭제 버튼이 없고, 수정 주소로 가도 상세 화면으로 돌아온다', async ({ page, request }) => {
    const author = newUser()
    await signupViaApi(request, author)
    const postId = await createPostViaApi(request, author, uniqueTitle('남의글'))
    const visitor = newUser()
    await signupViaApi(request, visitor)
    await loginViaUi(page, visitor)

    await page.goto(`/studies/${postId}`)
    await expect(page.locator('article')).toBeVisible()
    await expect(page.getByRole('link', { name: '수정' })).toHaveCount(0)
    await expect(page.locator('article').getByRole('button', { name: '삭제' })).toHaveCount(0)

    await page.goto(`/studies/${postId}/edit`)
    await expect(page).toHaveURL(`/studies/${postId}`)
  })

  test('없는 글 주소로 들어가면 오류 메시지가 보인다', async ({ page }) => {
    await page.goto('/studies/99999999')

    await expect(page.getByRole('alert')).toHaveText('존재하지 않는 스터디 모집글이에요.')
  })

  test('마감된 글을 목록에서 열어도 마감 상태로 보인다', async ({ page, request }) => {
    const user = newUser()
    await signupViaApi(request, user)
    const title = uniqueTitle('이미마감')
    const postId = await createPostViaApi(request, user, title)
    await closePostViaApi(request, user, postId)

    await page.goto('/studies')
    await page.getByLabel('검색어').fill(title)
    await page.getByRole('button', { name: '검색' }).click()
    await page.getByRole('link', { name: new RegExp(title) }).click()

    await expect(page.getByRole('heading', { name: title })).toBeVisible()
    await expect(page.getByText('모집 마감', { exact: true })).toBeVisible()
  })
})
