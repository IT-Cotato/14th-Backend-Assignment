import { expect, test } from '@playwright/test'
import {
  callApi,
  closePostByApi,
  createCommentByApi,
  createPostByApi,
  createUser,
  errorCodeOf,
  uniqueToken,
} from './helpers/api.ts'
import { loginByUi } from './helpers/ui.ts'

test.describe('댓글(스터디 신청)', () => {
  test('댓글을 작성하고 수정하고 삭제할 수 있다', async ({ page, request }) => {
    const author = await createUser(request)
    const applicant = await createUser(request)
    const postId = await createPostByApi(request, author.token, { title: `e2e 댓글 ${uniqueToken()}` })
    const first = `신청합니다 ${uniqueToken()}`
    const second = `수정한 신청 내용 ${uniqueToken()}`

    await loginByUi(page, applicant.user)
    await page.goto(`/studies/${postId}`)
    await expect(page.getByText('아직 댓글이 없어요', { exact: false })).toBeVisible()

    // 작성
    await page.getByLabel('댓글 내용').fill(first)
    await page.getByRole('button', { name: '댓글 등록' }).click()
    await expect(page.getByText(first)).toBeVisible()
    await expect(page.getByRole('heading', { name: '댓글 (1)' })).toBeVisible()

    // 수정
    await page.getByRole('button', { name: '댓글 수정', exact: true }).click()
    await page.getByRole('textbox', { name: '댓글 수정' }).fill(second)
    await page.getByRole('button', { name: '저장', exact: true }).click()
    await expect(page.getByText(second)).toBeVisible()
    await expect(page.getByText(first)).toHaveCount(0)

    // 삭제: 확인 단계를 거쳐야 지워진다
    await page.getByRole('button', { name: '댓글 삭제', exact: true }).click()
    await expect(page.getByRole('alertdialog', { name: '댓글 삭제 확인' })).toBeVisible()
    await page.getByRole('button', { name: '댓글 삭제 확인', exact: true }).click()
    await expect(page.getByText(second)).toHaveCount(0)
    await expect(page.getByText('아직 댓글이 없어요', { exact: false })).toBeVisible()
  })

  test('빈 댓글은 등록할 수 없다', async ({ page, request }) => {
    const author = await createUser(request)
    const postId = await createPostByApi(request, author.token, { title: `e2e 빈 댓글 ${uniqueToken()}` })

    await loginByUi(page, author.user)
    await page.goto(`/studies/${postId}`)
    await page.getByRole('button', { name: '댓글 등록' }).click()

    await expect(page.getByText('댓글은 1자 이상 500자 이하로 입력해 주세요.')).toBeVisible()
    await expect(page.getByRole('heading', { name: '댓글 (0)' })).toBeVisible()
  })

  test('모집이 마감된 글에는 댓글을 작성할 수 없다', async ({ page, request }) => {
    const author = await createUser(request)
    const applicant = await createUser(request)
    const postId = await createPostByApi(request, author.token, { title: `e2e 마감 ${uniqueToken()}` })
    await closePostByApi(request, author.token, postId)

    // 화면: 입력창 대신 안내 문구가 보인다
    await loginByUi(page, applicant.user)
    await page.goto(`/studies/${postId}`)
    await expect(page.getByText('모집이 마감되어 댓글을 작성할 수 없어요.')).toBeVisible()
    await expect(page.getByRole('button', { name: '댓글 등록' })).toHaveCount(0)

    // 서버: API를 직접 호출해도 409로 막는다
    const response = await callApi(request, 'POST', `/api/study-posts/${postId}/comments`, {
      token: applicant.token,
      data: { content: '마감된 글에 신청' },
    })
    expect(response.status()).toBe(409)
    expect(await errorCodeOf(response)).toBe('STUDY-003')
  })

  test('남의 댓글은 수정하거나 삭제할 수 없다', async ({ page, request }) => {
    const author = await createUser(request)
    const applicant = await createUser(request)
    const postId = await createPostByApi(request, author.token, { title: `e2e 남의 댓글 ${uniqueToken()}` })
    const content = `남의 댓글 ${uniqueToken()}`
    const commentId = await createCommentByApi(request, applicant.token, postId, content)

    // 화면: 글 작성자가 봐도 신청자의 댓글에는 수정·삭제 버튼이 없다
    await loginByUi(page, author.user)
    await page.goto(`/studies/${postId}`)
    await expect(page.getByText(content)).toBeVisible()
    await expect(page.getByRole('button', { name: '댓글 수정', exact: true })).toHaveCount(0)
    await expect(page.getByRole('button', { name: '댓글 삭제', exact: true })).toHaveCount(0)

    // 서버: API를 직접 호출하면 403으로 막는다
    const patch = await callApi(request, 'PATCH', `/api/comments/${commentId}`, {
      token: author.token,
      data: { content: '가로채기' },
    })
    expect(patch.status()).toBe(403)
    expect(await errorCodeOf(patch)).toBe('COMMENT-002')

    const remove = await callApi(request, 'DELETE', `/api/comments/${commentId}`, { token: author.token })
    expect(remove.status()).toBe(403)
    expect(await errorCodeOf(remove)).toBe('COMMENT-002')

    // 댓글은 그대로 남아 있다
    const list = await callApi(request, 'GET', `/api/study-posts/${postId}/comments`)
    expect(await list.text()).toContain(content)
  })
})
