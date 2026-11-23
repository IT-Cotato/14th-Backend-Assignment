import type { Page, TestInfo } from '@playwright/test'
import { test, expect, main, header, loginViaUI, uniqueId } from './fixtures'

// 모바일(375×812)에서 가로 스크롤, 화면 밖으로 나간 요소, 겹친 버튼, 잘린 글자를 점검해요.
test.use({ viewport: { width: 375, height: 812 }, isMobile: true, hasTouch: true, deviceScaleFactor: 2 })

interface LayoutIssue {
    kind: string
    detail: string
}

// 화면 안에서 실제로 보이는 버튼·링크·입력 요소를 기준으로 레이아웃 문제를 찾아요.
async function auditLayout(page: Page): Promise<LayoutIssue[]> {
    return page.evaluate(() => {
        const issues: { kind: string; detail: string }[] = []
        const vw = window.innerWidth
        const describe = (el: Element) =>
            `${el.tagName.toLowerCase()}「${(el.textContent || (el as HTMLInputElement).placeholder || el.getAttribute('aria-label') || '').trim().slice(0, 30)}」`

        if (document.documentElement.scrollWidth > vw) {
            issues.push({ kind: '가로 스크롤', detail: `scrollWidth ${document.documentElement.scrollWidth} > ${vw}` })
        }

        const visible = (el: Element) => {
            const r = el.getBoundingClientRect()
            const style = getComputedStyle(el)
            return r.width > 0 && r.height > 0 && style.visibility !== 'hidden' && style.display !== 'none'
        }

        const controls = [...document.querySelectorAll('button, a, input, select, textarea')].filter(visible)
        for (const el of controls) {
            const r = el.getBoundingClientRect()
            if (r.left < -0.5 || r.right > vw + 0.5) {
                issues.push({ kind: '화면 밖 요소', detail: `${describe(el)} left=${r.left.toFixed(1)} right=${r.right.toFixed(1)}` })
            }
        }

        // 서로 포함 관계가 아닌 컨트롤끼리 겹치는지 봐요.
        for (let i = 0; i < controls.length; i += 1) {
            for (let j = i + 1; j < controls.length; j += 1) {
                const a = controls[i]
                const b = controls[j]
                if (a.contains(b) || b.contains(a)) continue
                const ra = a.getBoundingClientRect()
                const rb = b.getBoundingClientRect()
                const overlapX = Math.min(ra.right, rb.right) - Math.max(ra.left, rb.left)
                const overlapY = Math.min(ra.bottom, rb.bottom) - Math.max(ra.top, rb.top)
                if (overlapX > 1 && overlapY > 1) {
                    issues.push({ kind: '요소 겹침', detail: `${describe(a)} ↔ ${describe(b)}` })
                }
            }
        }

        // 글자가 넘쳐서 잘린 버튼·링크·제목·라벨을 찾아요. (의도적으로 줄임표 처리한 truncate는 제외해요)
        const textEls = [...document.querySelectorAll('button, a, h1, h2, h3, label, p, dd, span')].filter(visible)
        for (const el of textEls) {
            // 스크린리더 전용(sr-only) 라벨은 화면에 보이지 않는 게 정상이라 빼요.
            if (el.classList.contains('truncate') || el.classList.contains('sr-only')) continue
            const style = getComputedStyle(el)
            const clipsX = style.overflowX !== 'visible' || style.whiteSpace === 'nowrap'
            if (clipsX && el.scrollWidth > el.clientWidth + 1 && el.clientWidth > 0) {
                issues.push({ kind: '글자 잘림', detail: `${describe(el)} ${el.scrollWidth}>${el.clientWidth}` })
            }
            const r = el.getBoundingClientRect()
            if (r.right > vw + 0.5) {
                issues.push({ kind: '글자 화면 밖', detail: `${describe(el)} right=${r.right.toFixed(1)}` })
            }
        }
        return issues
    })
}

async function check(page: Page, testInfo: TestInfo, name: string) {
    await page.screenshot({ path: testInfo.outputPath(`mobile-${name}.png`), fullPage: true })
    expect(await auditLayout(page), `${name} 모바일 레이아웃`).toEqual([])
}

// 띄어쓰기 없는 긴 문자열로 줄바꿈 처리를 확인해요.
const LONG_WORD = 'Algorithm_Study_With_A_Very_Long_Unbroken_Title_X'

test.describe('모바일 화면 (375×812)', () => {
    test('회원가입·로그인 화면', async ({ page }, testInfo) => {
        await page.goto('/signup')
        await main(page).getByRole('button', { name: '회원가입' }).click()
        await expect(main(page).getByText('파트를 선택해 주세요.')).toBeVisible()
        await check(page, testInfo, 'signup-errors')

        await page.goto('/login')
        await main(page).getByRole('button', { name: '로그인', exact: true }).click()
        await check(page, testInfo, 'login')
    })

    test('스터디 목록·상세·댓글·마이페이지 화면', async ({ page, data }, testInfo) => {
        const author = await data.createUser('BE')
        const applicant = await data.createUser('FE')
        const keyword = `m${uniqueId()}`
        const postId = await data.createPost(author, {
            title: `${LONG_WORD.slice(0, 40)}${keyword}`.slice(0, 50),
            content: `${LONG_WORD.repeat(4)}\n모바일 줄바꿈 확인용 본문이에요.`,
        })
        await data.createPost(author, { title: `모바일 목록 ${keyword}`, closed: true })
        await data.createComment(applicant, postId, LONG_WORD.repeat(3))
        await data.createComment(author, postId, '작성자 댓글이에요.')

        await loginViaUI(page, applicant)
        await expect(header(page).getByRole('button', { name: '로그아웃' })).toBeVisible()

        await page.goto(`/studies?keyword=${keyword}`)
        await expect(main(page).getByRole('listitem')).toHaveCount(2)
        await check(page, testInfo, 'study-list')

        await page.goto(`/studies/${postId}`)
        await expect(main(page).getByTestId('comment-item')).toHaveCount(2)
        await check(page, testInfo, 'study-detail-comments')

        // 댓글 수정 모드
        await main(page).getByTestId('comment-item').first().getByRole('button', { name: '수정' }).click()
        await expect(main(page).getByLabel('댓글 수정')).toBeVisible()
        await check(page, testInfo, 'comment-edit')

        await page.goto('/studies/new')
        await check(page, testInfo, 'study-create')

        await page.goto('/mypage')
        await expect(main(page).getByTestId('my-info')).toContainText(applicant.email)
        await check(page, testInfo, 'mypage')
    })
})
