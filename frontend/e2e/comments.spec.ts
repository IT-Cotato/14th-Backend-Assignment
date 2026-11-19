import { expect, test } from '@playwright/test'
import {
  API_URL,
  authHeader,
  closePostViaApi,
  createCommentViaApi,
  createLoggedInUser,
  createPostViaApi,
  loginViaUi,
  uniqueId,
} from './helpers'

const commentList = (page: import('@playwright/test').Page) => page.getByRole('list', { name: '신청 댓글 목록' })

test.describe('댓글(스터디 신청)', () => {
  test('댓글로 신청하고, 수정하고, 삭제한다', async ({ page, request }) => {
    const owner = await createLoggedInUser(request)
    const applicant = await createLoggedInUser(request, { generation: 13, part: 'FE' })
    const title = `댓글 흐름 ${uniqueId()}`
    const postId = await createPostViaApi(request, owner.token, { title, content: '같이 공부해요' })

    await loginViaUi(page, applicant.user)
    await page.goto(`/studies/${postId}`)
    await expect(page.getByText('아직 신청 댓글이 없어요.')).toBeVisible()

    // 작성
    await page.getByLabel('신청 내용').fill('안녕하세요! 알고리즘 입문 중인데 참여하고 싶어요.')
    await page.getByRole('button', { name: '신청하기' }).click()
    const item = commentList(page).getByRole('listitem')
    await expect(item).toHaveCount(1)
    await expect(item.getByText('안녕하세요! 알고리즘 입문 중인데 참여하고 싶어요.')).toBeVisible()
    await expect(item.getByText(applicant.user.nickname)).toBeVisible()
    await expect(item.getByText('13기')).toBeVisible()
    await expect(page.getByRole('heading', { name: '신청 댓글 1' })).toBeVisible()
    await expect(page.getByLabel('신청 내용')).toHaveValue('')

    // 수정
    await item.getByRole('button', { name: '댓글 수정' }).click()
    await item.getByLabel('댓글 수정').fill('수정한 신청 메시지')
    await item.getByRole('button', { name: '저장' }).click()
    await expect(item.getByText('수정한 신청 메시지')).toBeVisible()
    await expect(item.getByText('안녕하세요! 알고리즘 입문 중인데 참여하고 싶어요.')).toHaveCount(0)

    // 목록의 댓글 수에도 반영된다
    await page.goto('/studies')
    await page.getByRole('search').getByLabel('검색어').fill(title)
    await page.getByRole('search').getByRole('button', { name: '검색' }).click()
    await expect(page.getByRole('list', { name: '모집글 목록' }).getByText('신청 댓글 1')).toBeVisible()

    // 삭제
    await page.goto(`/studies/${postId}`)
    page.once('dialog', (dialog) => dialog.accept())
    await commentList(page).getByRole('button', { name: '댓글 삭제' }).click()
    await expect(page.getByText('아직 신청 댓글이 없어요.')).toBeVisible()
    await expect(page.getByRole('heading', { name: '신청 댓글 0' })).toBeVisible()
  })

  test('모집이 마감된 글에는 신청할 수 없고 서버도 409로 막는다', async ({ page, request }) => {
    const owner = await createLoggedInUser(request)
    const applicant = await createLoggedInUser(request)
    const postId = await createPostViaApi(request, owner.token, { title: `마감 ${uniqueId()}`, content: '마감된 글' })
    await closePostViaApi(request, owner.token, postId)

    await loginViaUi(page, applicant.user)
    await page.goto(`/studies/${postId}`)
    await expect(page.getByText('모집이 마감되어 더 이상 신청할 수 없어요.')).toBeVisible()
    await expect(page.getByLabel('신청 내용')).toHaveCount(0)
    await expect(page.getByRole('button', { name: '신청하기' })).toHaveCount(0)

    const response = await request.post(`${API_URL}/api/study-posts/${postId}/comments`, {
      data: { content: '늦었지만 신청해요' },
      headers: authHeader(applicant.token),
    })
    expect(response.status()).toBe(409)
    expect(await response.json()).toMatchObject({
      code: 'STUDY-003',
      message: '모집이 마감된 글에는 댓글을 쓸 수 없어요.',
    })
  })

  test('남의 댓글은 수정, 삭제 버튼이 없고 서버도 403으로 막는다', async ({ page, request }) => {
    const owner = await createLoggedInUser(request)
    const applicant = await createLoggedInUser(request)
    const postId = await createPostViaApi(request, owner.token, { title: `남의 댓글 ${uniqueId()}`, content: '내용' })
    const commentId = await createCommentViaApi(request, applicant.token, postId, '제가 쓴 신청 댓글이에요')

    // 글 작성자라도 신청자의 댓글은 바꿀 수 없다
    await loginViaUi(page, owner.user)
    await page.goto(`/studies/${postId}`)
    const item = commentList(page).getByRole('listitem')
    await expect(item.getByText('제가 쓴 신청 댓글이에요')).toBeVisible()
    await expect(item.getByRole('button', { name: '댓글 수정' })).toHaveCount(0)
    await expect(item.getByRole('button', { name: '댓글 삭제' })).toHaveCount(0)

    const patch = await request.patch(`${API_URL}/api/comments/${commentId}`, {
      data: { content: '바꿔치기' },
      headers: authHeader(owner.token),
    })
    expect(patch.status()).toBe(403)
    expect(await patch.json()).toMatchObject({ code: 'COMMENT-002' })

    const remove = await request.delete(`${API_URL}/api/comments/${commentId}`, { headers: authHeader(owner.token) })
    expect(remove.status()).toBe(403)

    await page.reload()
    await expect(item.getByText('제가 쓴 신청 댓글이에요')).toBeVisible()
  })

  test('로그인하지 않으면 댓글 목록은 보이지만 신청할 수 없다', async ({ page, request }) => {
    const owner = await createLoggedInUser(request)
    const applicant = await createLoggedInUser(request)
    const postId = await createPostViaApi(request, owner.token, { title: `비로그인 ${uniqueId()}`, content: '내용' })
    await createCommentViaApi(request, applicant.token, postId, '먼저 신청했어요')

    await page.goto(`/studies/${postId}`)
    await expect(commentList(page).getByText('먼저 신청했어요')).toBeVisible()
    await expect(page.getByText('하면 댓글로 스터디를 신청할 수 있어요.')).toBeVisible()
    await expect(page.getByLabel('신청 내용')).toHaveCount(0)

    const response = await request.post(`${API_URL}/api/study-posts/${postId}/comments`, {
      data: { content: '토큰 없이 신청' },
    })
    expect(response.status()).toBe(401)
  })

  test('빈 댓글은 등록되지 않고 검증 메시지를 보여준다', async ({ page, request }) => {
    const owner = await createLoggedInUser(request)
    const applicant = await createLoggedInUser(request)
    const postId = await createPostViaApi(request, owner.token, { title: `빈 댓글 ${uniqueId()}`, content: '내용' })

    await loginViaUi(page, applicant.user)
    await page.goto(`/studies/${postId}`)
    await page.getByLabel('신청 내용').fill('   ')
    await page.getByRole('button', { name: '신청하기' }).click()

    await expect(page.getByText('댓글 내용을 입력해 주세요.')).toBeVisible()
    await expect(page.getByText('아직 신청 댓글이 없어요.')).toBeVisible()
  })
})
