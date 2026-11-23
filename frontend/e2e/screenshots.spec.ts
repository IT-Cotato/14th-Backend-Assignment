import { fileURLToPath } from 'node:url'
import { test, expect, main, loginViaUI, uniqueId, postIdFromUrl } from './fixtures'

// 주요 화면을 docs/screenshots/에 저장해요.
// 테스트 계정과 테스트 모집글만 보이도록 목록은 이번 실행의 키워드로 걸러서 찍어요.
const SCREENSHOT_DIR = fileURLToPath(new URL('../../docs/screenshots/', import.meta.url))

test('주요 화면 스크린샷', async ({ page, data }) => {
    await page.setViewportSize({ width: 1280, height: 860 })
    const shot = (name: string) => page.screenshot({ path: `${SCREENSHOT_DIR}${name}.png`, fullPage: true })

    const author = await data.createUser('BE')
    const applicant = await data.createUser('FE')
    const keyword = `스크린샷${uniqueId()}`
    await data.createPost(author, { title: `[${keyword}] 알고리즘 스터디원 모집`, content: '백준 골드 문제를 매주 함께 풀어요.' })
    await data.createPost(author, { title: `[${keyword}] 디자인 시스템 스터디`, content: '피그마 컴포넌트를 함께 만들어요.', closed: true })

    // 1. 회원가입 (비밀번호는 가려져서 보여요)
    const info = data.newUserInfo('PM')
    await page.goto('/signup')
    await main(page).getByLabel('이메일').fill(info.email)
    await main(page).getByLabel('비밀번호', { exact: true }).fill(info.password)
    await main(page).getByLabel('비밀번호 확인').fill(info.password)
    await main(page).getByLabel('이름').fill(info.name)
    await main(page).getByLabel('닉네임').fill(info.nickname)
    await main(page).getByLabel('파트').selectOption('PM')
    await shot('01-signup')

    // 2. 로그인
    await page.goto('/login')
    await main(page).getByLabel('이메일').fill(applicant.email)
    await shot('02-login')
    await loginViaUI(page, applicant, '')

    // 3. 스터디 목록
    await page.goto(`/studies?keyword=${encodeURIComponent(keyword)}`)
    await expect(main(page).getByRole('listitem')).toHaveCount(2)
    await shot('03-study-list')

    // 4. 스터디 상세 + 7. 댓글 작성 완료
    await main(page).getByRole('link', { name: /알고리즘 스터디원 모집/ }).click()
    await expect(main(page).getByLabel('신청 댓글 작성')).toBeVisible()
    await shot('04-study-detail')
    await main(page).getByLabel('신청 댓글 작성').fill('안녕하세요! 프론트엔드 파트인데 알고리즘 스터디에 참여하고 싶어요.')
    await main(page).getByRole('button', { name: '댓글 등록' }).click()
    await expect(main(page).getByTestId('comment-item')).toHaveCount(1)
    await shot('07-comment-created')

    // 5. 스터디 작성
    await page.goto('/studies/new')
    await main(page).getByLabel('제목').fill(`[${keyword}] React 스터디 모집`)
    await main(page).getByLabel('내용').fill('매주 수요일 저녁에 React 공식 문서를 함께 읽어요.\n모집 인원: 4명')
    await shot('05-study-create')
    await main(page).getByRole('button', { name: '등록' }).click()
    await expect(page).toHaveURL(/\/studies\/\d+$/)
    const postId = postIdFromUrl(page.url())
    data.trackPost(postId, applicant)

    // 6. 스터디 수정
    await page.goto(`/studies/${postId}/edit`)
    await expect(main(page).getByLabel('모집 상태')).toBeVisible()
    await shot('06-study-edit')

    // 8. 마이페이지
    await page.goto('/mypage')
    await expect(main(page).getByTestId('my-info')).toContainText(applicant.email)
    await shot('08-mypage')
})
