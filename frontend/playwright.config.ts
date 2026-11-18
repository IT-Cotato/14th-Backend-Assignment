import { defineConfig, devices } from '@playwright/test'

// E2E를 돌리기 전에 MySQL과 백엔드(8080)를 먼저 켜 둔다. 프론트 개발 서버는 필요하면 자동으로 켠다.
export default defineConfig({
  testDir: './e2e',
  // 테스트가 같은 DB를 쓰므로 한 번에 하나씩 돌린다
  fullyParallel: false,
  workers: 1,
  timeout: 30_000,
  expect: { timeout: 7_000 },
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: 'http://localhost:3000',
    screenshot: 'only-on-failure',
    trace: 'on-first-retry',
  },
  projects: [
    // 컴퓨터에 설치된 Chrome을 쓴다. (브라우저를 따로 내려받지 않는다)
    // Chrome이 없으면 channel 줄을 지우고 `npx playwright install chromium`을 실행한다.
    { name: 'chromium', use: { ...devices['Desktop Chrome'], channel: 'chrome' } },
  ],
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:3000',
    reuseExistingServer: true,
    timeout: 60_000,
  },
})
