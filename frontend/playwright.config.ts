import { defineConfig } from '@playwright/test'

// E2E를 돌리기 전에 MySQL과 백엔드(8080)를 먼저 켜 둔다. 프론트 개발 서버는 필요하면 자동으로 켠다.
export default defineConfig({
  testDir: './e2e',
  timeout: 30_000,
  expect: { timeout: 7_000 },
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: 'http://localhost:3000',
    // 컴퓨터에 설치된 Chrome을 쓴다. (브라우저를 따로 내려받지 않는다)
    channel: 'chrome',
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
  },
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:3000',
    reuseExistingServer: true,
    timeout: 60_000,
  },
})
