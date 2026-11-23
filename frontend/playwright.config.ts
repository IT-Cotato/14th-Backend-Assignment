import { defineConfig, devices } from '@playwright/test'

const FRONTEND_URL = 'http://localhost:3000'

export default defineConfig({
  testDir: './e2e',
  // 모든 테스트가 같은 개발 DB를 쓰므로 하나씩 순서대로 돌린다
  fullyParallel: false,
  workers: 1,
  forbidOnly: !!process.env.CI,
  retries: 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: FRONTEND_URL,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: {
    command: 'npm run dev',
    url: FRONTEND_URL,
    reuseExistingServer: true,
    timeout: 60_000,
  },
})
