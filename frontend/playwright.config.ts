import { defineConfig, devices } from '@playwright/test'

const FRONTEND_URL = 'http://localhost:3000'

/**
 * 실행 전에 백엔드(8080)와 DB를 먼저 켜요. 프론트 서버는 webServer가 없으면 띄워요.
 * 백엔드를 다른 포트로 띄웠다면 BACKEND_URL=http://localhost:포트 로 넘겨요 (vite 프록시가 따라가요).
 */
export default defineConfig({
  testDir: './e2e',
  // 테스트가 같은 DB를 쓰므로 한 번에 하나씩 돌린다
  fullyParallel: false,
  workers: 1,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: FRONTEND_URL,
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: {
    command: 'npm run dev',
    url: FRONTEND_URL,
    reuseExistingServer: true,
  },
})
