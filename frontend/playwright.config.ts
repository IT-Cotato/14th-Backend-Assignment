import { defineConfig, devices } from '@playwright/test'

const FRONTEND_URL = 'http://localhost:3000'

export default defineConfig({
  testDir: './e2e',
  // 모든 테스트가 같은 개발 DB 를 쓰므로 한 번에 하나씩 돌린다
  fullyParallel: false,
  workers: 1,
  // 터미널 캡처에 테스트 목록과 통과 개수가 함께 보이도록 list 리포터를 쓴다
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: FRONTEND_URL,
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  // 프론트 서버는 Playwright 가 띄운다. 백엔드와 DB 는 미리 켜 두어야 한다.
  webServer: {
    command: 'npm run dev',
    url: FRONTEND_URL,
    reuseExistingServer: true,
  },
})
