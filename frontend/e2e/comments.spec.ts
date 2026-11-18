import { expect, test } from '@playwright/test'
import {
  closePostViaApi,
  createCommentViaApi,
  createPostViaApi,
  createUserAndLogin,
  loginViaUi,
  newUser,
  signupViaApi,
  uniqueTitle,
} from './helpers.ts'

test.describe('댓글 (스터디 신청)', () => {
  test('로그인한 사용자가 댓글을 쓰면 목록에 보이고 글 목록의 댓글 수도 늘어난다', async ({ page, request }) => {
    const author = newUser()
    await signupViaApi(request, author)
    const title = uniqueTitle('댓글받는글')
    const postId = await createPostViaApi(request, author, title)
    await createUserAndLogin(page, request)

    await page.goto(`/studies/${postId}`)
    const comments = page.getByRole('region', { name: '댓글' })
    await comments.getByLabel('신청 댓글').fill('참여하고 싶어요!')
    await comments.getByRole('button', { name: '댓글 등록' }).click()

    await expect(comments.getByText('참여하고 싶어요!')).toBeVisible()
    await expect(comments.getByRole('heading', { name: '댓글 1' })).toBeVisible()

    await page.goto('/studies')
    await page.getByLabel('검색어').fill(title)
    await page.getByRole('button', { name: '검색' }).click()
    await expect(page.getByRole('link', { name: new RegExp(`${title}.*댓글 1`) })).toBeVisible()
  })

  test('내용을 비우고 등록하면 안내가 보인다', async ({ page, request }) => {
    const author = newUser()
    await signupViaApi(request, author)
    const postId = await createPostViaApi(request, author, uniqueTitle('빈댓글'))
    await createUserAndLogin(page, request)

    await page.goto(`/studies/${postId}`)
    await page.getByRole('button', { name: '댓글 등록' }).click()

    await expect(page.getByRole('region', { name: '댓글' }).getByRole('alert'))
      .toHaveText('댓글 내용을 입력해 주세요.')
  })

  test('내 댓글을 수정하면 바뀐 내용이 보인다', async ({ page, request }) => {
    const author = newUser()
    await signupViaApi(request, author)
    const postId = await createPostViaApi(request, author, uniqueTitle('수정댓글'))
    const me = await createUserAndLogin(page, request)
    await createCommentViaApi(request, me, postId, '처음 쓴 댓글')

    await page.goto(`/studies/${postId}`)
    const comments = page.getByRole('region', { name: '댓글' })
    await comments.getByRole('button', { name: '수정' }).click()
    await comments.getByLabel('댓글 수정').fill('고친 댓글')
    await comments.getByRole('button', { name: '저장' }).click()

    await expect(comments.getByText('고친 댓글')).toBeVisible()
    await expect(comments.getByText('처음 쓴 댓글')).toHaveCount(0)
  })

  test('내 댓글을 삭제하면 목록에서 사라진다', async ({ page, request }) => {
    const author = newUser()
    await signupViaApi(request, author)
    const postId = await createPostViaApi(request, author, uniqueTitle('삭제댓글'))
    const me = await createUserAndLogin(page, request)
    await createCommentViaApi(request, me, postId, '지울 댓글')

    await page.goto(`/studies/${postId}`)
    const comments = page.getByRole('region', { name: '댓글' })
    page.once('dialog', (dialog) => dialog.accept())
    await comments.getByRole('button', { name: '삭제' }).click()

    await expect(comments.getByText('지울 댓글')).toHaveCount(0)
    await expect(comments.getByText('아직 댓글이 없어요.', { exact: false })).toBeVisible()
  })

  test('남의 댓글에는 수정·삭제 버튼이 없다', async ({ page, request }) => {
    const author = newUser()
    await signupViaApi(request, author)
    const postId = await createPostViaApi(request, author, uniqueTitle('남의댓글'))
    const other = newUser()
    await signupViaApi(request, other)
    await createCommentViaApi(request, other, postId, '다른 사람의 댓글')
    const visitor = newUser()
    await signupViaApi(request, visitor)
    await loginViaUi(page, visitor)

    await page.goto(`/studies/${postId}`)
    const comments = page.getByRole('region', { name: '댓글' })
    await expect(comments.getByText('다른 사람의 댓글')).toBeVisible()
    await expect(comments.getByRole('button', { name: '수정' })).toHaveCount(0)
    await expect(comments.getByRole('button', { name: '삭제' })).toHaveCount(0)
  })

  test('모집이 마감된 글에는 댓글 입력창 대신 안내가 보인다', async ({ page, request }) => {
    const author = newUser()
    await signupViaApi(request, author)
    const postId = await createPostViaApi(request, author, uniqueTitle('마감된글'))
    await closePostViaApi(request, author, postId)
    await createUserAndLogin(page, request)

    await page.goto(`/studies/${postId}`)
    const comments = page.getByRole('region', { name: '댓글' })

    await expect(comments.getByText('모집이 마감된 글에는 댓글을 쓸 수 없어요.')).toBeVisible()
    await expect(comments.getByLabel('신청 댓글')).toHaveCount(0)
  })

  test('로그인하지 않으면 댓글은 볼 수 있지만 쓰려면 로그인하라는 안내가 보인다', async ({ page, request }) => {
    const author = newUser()
    await signupViaApi(request, author)
    const postId = await createPostViaApi(request, author, uniqueTitle('비로그인'))
    await createCommentViaApi(request, author, postId, '미리 달린 댓글')

    await page.goto(`/studies/${postId}`)
    const comments = page.getByRole('region', { name: '댓글' })

    await expect(comments.getByText('미리 달린 댓글')).toBeVisible()
    await expect(comments.getByText('댓글을 쓰려면')).toBeVisible()
    await expect(comments.getByLabel('신청 댓글')).toHaveCount(0)
  })
})
