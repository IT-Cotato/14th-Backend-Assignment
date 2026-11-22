import { defineConfig, devices } from '@playwright/test'

// 프론트 주소. 다른 주소로 돌리고 싶으면 E2E_BASE_URL을 바꿔요
const baseURL = process.env.E2E_BASE_URL ?? 'http://localhost:3000'

export default defineConfig({
  testDir: './e2e',
  // 모든 테스트가 같은 로컬 DB를 써서 한 번에 하나씩 실행해요
  workers: 1,
  fullyParallel: false,
  retries: 0,
  forbidOnly: !!process.env.CI,
  reporter: [['list'], ['html', { open: 'never' }]],
  // 백엔드는 webServer가 띄우지 않아서, 꺼져 있으면 시작 전에 알려줘요
  globalSetup: './e2e/global-setup.ts',
  use: {
    baseURL,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: {
    command: 'npm run dev',
    url: baseURL,
    // 이미 떠 있는 dev 서버(3000)가 있으면 그대로 써요
    reuseExistingServer: true,
    timeout: 60_000,
  },
})
