import { expect, test, type Page } from '@playwright/test'
import {
  API_URL,
  authHeader,
  closePostViaApi,
  createLoggedInUser,
  createPostViaApi,
  loginViaUi,
  signupViaApi,
  uniqueId,
} from './helpers'

async function search(page: Page, filters: { keyword?: string; status?: string; part?: string }) {
  const form = page.getByRole('search')
  await form.getByLabel('검색어').fill(filters.keyword ?? '')
  await form.getByLabel('모집 상태').selectOption(filters.status ?? '')
  await form.getByLabel('작성자 파트').selectOption(filters.part ?? '')
  await form.getByRole('button', { name: '검색' }).click()
}

const postList = (page: Page) => page.getByRole('list', { name: '모집글 목록' })

test.describe('스터디 모집글', () => {
  test('작성 → 목록 → 검색 → 상세 → 수정 → 모집 마감 → 삭제', async ({ page, request }) => {
    const user = await signupViaApi(request, { generation: 12, part: 'DE' })
    const tag = uniqueId()
    const title = `알고리즘 스터디 ${tag}`
    await loginViaUi(page, user)

    // 작성
    await page.getByRole('link', { name: '모집글 작성' }).click()
    await expect(page).toHaveURL(/\/studies\/new$/)
    await page.getByLabel('제목').fill(title)
    await page.getByLabel('내용').fill('주 2회 백준 풀이\n온라인으로 진행해요.')
    await page.getByRole('button', { name: '등록하기' }).click()

    await expect(page).toHaveURL(/\/studies\/\d+$/)
    await expect(page.getByRole('heading', { name: title })).toBeVisible()
    await expect(page.getByText('모집 중', { exact: true })).toBeVisible()
    const postUrl = page.url()

    // 목록에서 작성자 정보와 함께 보이고 검색된다
    await page.getByRole('link', { name: '← 목록으로' }).click()
    await search(page, { keyword: tag })
    await expect(postList(page).getByRole('listitem')).toHaveCount(1)
    const item = postList(page).getByRole('listitem').first()
    await expect(item.getByRole('link', { name: title })).toBeVisible()
    await expect(item.getByText(user.nickname)).toBeVisible()
    await expect(item.getByText('디자인(DE)')).toBeVisible()
    await expect(item.getByText('12기')).toBeVisible()

    // 상세
    await item.getByRole('link', { name: title }).click()
    await expect(page).toHaveURL(postUrl)

    // 수정
    await page.getByRole('link', { name: '수정', exact: true }).click()
    await expect(page).toHaveURL(/\/edit$/)
    await expect(page.getByLabel('제목')).toHaveValue(title)
    await page.getByLabel('제목').fill(`${title} (수정)`)
    await page.getByLabel('내용').fill('주 3회로 바꿨어요.')
    await page.getByRole('button', { name: '수정 완료' }).click()
    await expect(page).toHaveURL(postUrl)
    await expect(page.getByRole('heading', { name: `${title} (수정)` })).toBeVisible()
    await expect(page.getByText('주 3회로 바꿨어요.')).toBeVisible()

    // 모집 마감
    await page.getByRole('button', { name: '모집 마감' }).click()
    await expect(page.getByText('모집 마감', { exact: true }).first()).toBeVisible()
    await expect(page.getByRole('button', { name: '모집 다시 열기' })).toBeVisible()

    // 삭제
    page.once('dialog', (dialog) => dialog.accept())
    await page.getByRole('button', { name: '삭제', exact: true }).click()
    await expect(page).toHaveURL(/\/studies$/)
    await expect(page.getByText('모집글을 삭제했어요.')).toBeVisible()

    await search(page, { keyword: tag })
    await expect(page.getByText('조건에 맞는 모집글이 없어요.')).toBeVisible()
    await page.goto(postUrl)
    await expect(page.getByRole('alert')).toHaveText('존재하지 않는 스터디 모집글이에요.')
  })

  test('모집 상태와 작성자 파트로 거르고, 조건을 함께 쓰면 모두 만족하는 글만 보인다', async ({ page, request }) => {
    const tag = uniqueId()
    const be = await createLoggedInUser(request, { part: 'BE' })
    const fe = await createLoggedInUser(request, { part: 'FE' })
    const beRecruiting = `BE 모집중 ${tag}`
    const feRecruiting = `FE 모집중 ${tag}`
    const feClosed = `FE 마감 ${tag}`
    await createPostViaApi(request, be.token, { title: beRecruiting, content: '스프링' })
    await createPostViaApi(request, fe.token, { title: feRecruiting, content: '리액트' })
    const closedId = await createPostViaApi(request, fe.token, { title: feClosed, content: '타입스크립트' })
    await closePostViaApi(request, fe.token, closedId)

    await page.goto('/studies')
    await search(page, { keyword: tag })
    await expect(postList(page).getByRole('listitem')).toHaveCount(3)

    await search(page, { keyword: tag, part: 'FE' })
    await expect(postList(page).getByRole('listitem')).toHaveCount(2)
    await expect(postList(page).getByRole('link', { name: beRecruiting })).toHaveCount(0)

    await search(page, { keyword: tag, status: 'CLOSED' })
    await expect(postList(page).getByRole('listitem')).toHaveCount(1)
    await expect(postList(page).getByRole('link', { name: feClosed })).toBeVisible()

    await search(page, { keyword: tag, status: 'RECRUITING', part: 'FE' })
    await expect(postList(page).getByRole('listitem')).toHaveCount(1)
    await expect(postList(page).getByRole('link', { name: feRecruiting })).toBeVisible()
    await expect(page).toHaveURL(new RegExp(`keyword=${tag}.*status=RECRUITING.*part=FE`))

    await search(page, { keyword: tag, status: 'CLOSED', part: 'BE' })
    await expect(page.getByText('조건에 맞는 모집글이 없어요.')).toBeVisible()
  })

  test('글이 많으면 페이지를 넘겨 볼 수 있다', async ({ page, request }) => {
    const tag = uniqueId()
    const { token } = await createLoggedInUser(request)
    for (let i = 1; i <= 11; i += 1) {
      await createPostViaApi(request, token, { title: `페이지 ${tag} ${String(i).padStart(2, '0')}`, content: '내용' })
    }

    await page.goto('/studies')
    await search(page, { keyword: tag })
    await expect(page.getByText('총 11개')).toBeVisible()
    await expect(postList(page).getByRole('listitem')).toHaveCount(10)
    await expect(postList(page).getByRole('link').first()).toHaveText(`페이지 ${tag} 11`)

    await page.getByRole('navigation', { name: '모집글 페이지' }).getByRole('button', { name: '다음' }).click()
    await expect(postList(page).getByRole('listitem')).toHaveCount(1)
    await expect(postList(page).getByRole('link', { name: `페이지 ${tag} 01` })).toBeVisible()
  })

  test('로그인하지 않으면 모집글을 작성할 수 없다', async ({ page, request }) => {
    await page.goto('/studies')
    await expect(page.getByRole('link', { name: '모집글 작성' })).toHaveCount(0)

    await page.goto('/studies/new')
    await expect(page).toHaveURL(/\/login$/)

    const response = await request.post(`${API_URL}/api/study-posts`, { data: { title: '몰래 작성', content: '내용' } })
    expect(response.status()).toBe(401)
    expect(await response.json()).toMatchObject({ code: 'COMMON-004' })
  })

  test('남의 글은 수정, 마감, 삭제 버튼이 없고 서버도 403으로 막는다', async ({ page, request }) => {
    const owner = await createLoggedInUser(request)
    const other = await createLoggedInUser(request)
    const title = `남의 글 ${uniqueId()}`
    const postId = await createPostViaApi(request, owner.token, { title, content: '작성자만 바꿀 수 있어요' })

    await loginViaUi(page, other.user)
    await page.goto(`/studies/${postId}`)
    await expect(page.getByRole('heading', { name: title })).toBeVisible()
    await expect(page.getByRole('link', { name: '수정', exact: true })).toHaveCount(0)
    await expect(page.getByRole('button', { name: '모집 마감' })).toHaveCount(0)
    await expect(page.getByRole('button', { name: '삭제', exact: true })).toHaveCount(0)

    await page.goto(`/studies/${postId}/edit`)
    await expect(page.getByRole('alert')).toHaveText('모집글 작성자만 수정할 수 있어요.')

    const patch = await request.patch(`${API_URL}/api/study-posts/${postId}`, {
      data: { title: '바꿔치기', studyStatus: 'CLOSED' },
      headers: authHeader(other.token),
    })
    expect(patch.status()).toBe(403)
    expect(await patch.json()).toMatchObject({ code: 'STUDY-002' })

    const remove = await request.delete(`${API_URL}/api/study-posts/${postId}`, { headers: authHeader(other.token) })
    expect(remove.status()).toBe(403)

    const detail = await request.get(`${API_URL}/api/study-posts/${postId}`)
    expect((await detail.json()).data).toMatchObject({ title, studyStatus: 'RECRUITING', isAuthor: false })
  })

  test('제목 없이 등록하면 검증 메시지를 보여준다', async ({ page, request }) => {
    const user = await signupViaApi(request)
    await loginViaUi(page, user)

    await page.goto('/studies/new')
    await page.getByLabel('내용').fill('제목을 깜빡했어요')
    await page.getByRole('button', { name: '등록하기' }).click()

    await expect(page.getByText('제목을 입력해 주세요.')).toBeVisible()
    await expect(page).toHaveURL(/\/studies\/new$/)
  })
})
