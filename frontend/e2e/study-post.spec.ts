import { expect, test, type Page } from '@playwright/test'
import {
  acceptNextDialog,
  API_URL,
  authHeader,
  createPostViaApi,
  createUserViaApi,
  loginViaUi,
  uniqueTitle,
} from './helpers.ts'

interface SearchOptions {
  keyword: string
  status?: '전체' | '모집 중' | '모집 마감'
  part?: '' | 'PM' | 'DE' | 'BE' | 'FE'
}

const STATUS_PARAMS = { 전체: null, '모집 중': 'RECRUITING', '모집 마감': 'CLOSED' } as const

async function search(page: Page, { keyword, status = '전체', part = '' }: SearchOptions) {
  await page.getByLabel('검색어').fill(keyword)
  await page.getByLabel('모집 상태').selectOption({ label: status })
  await page.getByLabel('파트').selectOption(part)
  await page.getByRole('button', { name: '검색', exact: true }).click()
  // 검색 조건 전체가 주소에 반영될 때까지 기다린다.
  // keyword 만 보면 연속 검색에서 이전 검색의 주소로도 통과해, 폼이 다시 그려지기 전에 다음 입력을 하게 된다
  await expect(page).toHaveURL(
    (url) =>
      url.searchParams.get('keyword') === keyword &&
      url.searchParams.get('status') === STATUS_PARAMS[status] &&
      url.searchParams.get('part') === (part || null),
  )
}

const EMPTY_MESSAGE = '조건에 맞는 모집글이 없습니다.'

test.describe('스터디 모집글', () => {
  test('모집글을 작성하고 목록에서 검색, 필터로 찾은 뒤 수정, 모집 마감, 삭제까지 할 수 있다', async ({
    page,
    request,
  }) => {
    const { user } = await createUserViaApi(request, { generation: 14, part: 'BE' })
    const title = uniqueTitle('E2E 스터디')
    const editedTitle = uniqueTitle('E2E 수정됨')
    const content = '주 2회 모여서 문제를 풉니다.'
    const listItem = (text: string) => page.getByRole('listitem').filter({ hasText: text })

    await loginViaUi(page, user)

    // 작성
    await page.getByRole('link', { name: '모집글 작성' }).click()
    await expect(page).toHaveURL(/\/studies\/new$/)
    await page.getByLabel('제목').fill(title)
    await page.getByLabel('내용').fill(content)
    await page.getByRole('button', { name: '작성하기' }).click()
    await expect(page).toHaveURL(/\/studies\/\d+$/)
    await expect(page.getByRole('heading', { name: title })).toBeVisible()
    const detailUrl = page.url()
    const postId = Number(detailUrl.split('/').pop())

    // 목록: 작성자의 닉네임, 기수, 파트와 모집 상태가 보인다
    await page.goto('/studies')
    await expect(listItem(title)).toContainText(`${user.nickname} (14기 BE)`)
    await expect(listItem(title).getByText('모집 중')).toBeVisible()

    // 검색, 모집 상태와 파트 필터
    await search(page, { keyword: title })
    await expect(listItem(title)).toBeVisible()
    await search(page, { keyword: title, status: '모집 중', part: 'BE' })
    await expect(listItem(title)).toBeVisible()
    await search(page, { keyword: title, part: 'FE' })
    await expect(page.getByText(EMPTY_MESSAGE)).toBeVisible()
    await search(page, { keyword: title, status: '모집 마감' })
    await expect(page.getByText(EMPTY_MESSAGE)).toBeVisible()

    // 상세
    await search(page, { keyword: title })
    await page.getByRole('link', { name: title }).click()
    await expect(page).toHaveURL(detailUrl)
    await expect(page.getByText(content)).toBeVisible()

    // 수정
    await page.getByRole('link', { name: '모집글 수정' }).click()
    await expect(page).toHaveURL(/\/studies\/\d+\/edit$/)
    await expect(page.getByLabel('제목')).toHaveValue(title)
    await page.getByLabel('제목').fill(editedTitle)
    await page.getByRole('button', { name: '수정하기' }).click()
    await expect(page).toHaveURL(detailUrl)
    await expect(page.getByRole('heading', { name: editedTitle })).toBeVisible()

    // 모집 마감
    await page.getByRole('button', { name: '모집 마감하기' }).click()
    await expect(page.getByText('모집 마감', { exact: true })).toBeVisible()
    await expect(page.getByRole('button', { name: '모집 마감하기' })).toHaveCount(0)

    await page.goto('/studies')
    await search(page, { keyword: editedTitle, status: '모집 마감' })
    await expect(listItem(editedTitle)).toBeVisible()
    await search(page, { keyword: editedTitle, status: '모집 중' })
    await expect(page.getByText(EMPTY_MESSAGE)).toBeVisible()

    // 삭제
    await page.goto(detailUrl)
    acceptNextDialog(page)
    await page.getByRole('button', { name: '모집글 삭제' }).click()
    await expect(page).toHaveURL(/\/studies$/)
    await search(page, { keyword: editedTitle })
    await expect(page.getByText(EMPTY_MESSAGE)).toBeVisible()

    const response = await request.get(`${API_URL}/api/study-posts/${postId}`)
    expect(response.status()).toBe(404)
    expect((await response.json()).code).toBe('STUDY-001')
  })

  test('로그인 없이 모집글을 작성하려 하면 로그인 화면으로 보내고 서버도 401로 막는다', async ({
    page,
    request,
  }) => {
    await page.goto('/studies/new')
    await expect(page).toHaveURL(/\/login$/)

    const response = await request.post(`${API_URL}/api/study-posts`, {
      data: { title: uniqueTitle('E2E 비로그인'), content: '작성되면 안 되는 글' },
    })
    expect(response.status()).toBe(401)
    expect((await response.json()).code).toBe('COMMON-004')
  })

  test('남의 글에는 수정, 마감, 삭제 버튼이 보이지 않고 서버도 403으로 막는다', async ({
    page,
    request,
  }) => {
    const author = await createUserViaApi(request)
    const other = await createUserViaApi(request)
    const title = uniqueTitle('E2E 남의 글')
    const postId = await createPostViaApi(request, author.token, {
      title,
      content: '작성자만 고칠 수 있는 글',
    })

    await loginViaUi(page, other.user)
    await page.goto(`/studies/${postId}`)
    await expect(page.getByRole('heading', { name: title })).toBeVisible()
    await expect(page.getByRole('link', { name: '모집글 수정' })).toHaveCount(0)
    await expect(page.getByRole('button', { name: '모집 마감하기' })).toHaveCount(0)
    await expect(page.getByRole('button', { name: '모집글 삭제' })).toHaveCount(0)

    // 주소를 직접 입력해 수정 화면에 들어가도 수정할 수 없다
    await page.goto(`/studies/${postId}/edit`)
    await expect(page.getByRole('alert')).toHaveText('모집글 작성자만 수정할 수 있습니다.')
    await expect(page.getByRole('button', { name: '수정하기' })).toHaveCount(0)

    const patchResponse = await request.patch(`${API_URL}/api/study-posts/${postId}`, {
      headers: authHeader(other.token),
      data: { title: '남이 바꾼 제목' },
    })
    expect(patchResponse.status()).toBe(403)
    expect((await patchResponse.json()).code).toBe('STUDY-002')

    const deleteResponse = await request.delete(`${API_URL}/api/study-posts/${postId}`, {
      headers: authHeader(other.token),
    })
    expect(deleteResponse.status()).toBe(403)
    expect((await deleteResponse.json()).code).toBe('STUDY-002')

    const detailResponse = await request.get(`${API_URL}/api/study-posts/${postId}`)
    expect((await detailResponse.json()).data.title).toBe(title)
  })

  test('제목 없이 작성하면 서버의 검증 메시지를 보여준다', async ({ page, request }) => {
    const { user } = await createUserViaApi(request)

    await loginViaUi(page, user)
    await page.goto('/studies/new')
    await page.getByLabel('내용').fill('제목이 없는 글')
    await page.getByRole('button', { name: '작성하기' }).click()

    await expect(page.getByRole('alert')).toBeVisible()
    await expect(page).toHaveURL(/\/studies\/new$/)
  })

  test('목록 조회에서 모집 상태나 파트 값이 올바르지 않으면 서버가 400을 내려준다', async ({
    request,
  }) => {
    const statusResponse = await request.get(`${API_URL}/api/study-posts?status=WRONG`)
    expect(statusResponse.status()).toBe(400)
    expect((await statusResponse.json()).code).toBe('COMMON-003')

    const partResponse = await request.get(`${API_URL}/api/study-posts?part=WRONG`)
    expect(partResponse.status()).toBe(400)
    expect((await partResponse.json()).code).toBe('COMMON-003')
  })
})
