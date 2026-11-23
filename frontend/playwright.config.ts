import { defineConfig, devices } from '@playwright/test'

const FRONTEND_URL = 'http://localhost:3000'

export default defineConfig({
  testDir: './e2e',
  // 모든 테스트가 같은 DB를 쓰므로 한 번에 하나씩 실행한다
  fullyParallel: false,
  workers: 1,
  // 테스트 목록과 마지막 "N passed" 요약을 터미널에 그대로 보여준다
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: FRONTEND_URL, // page.goto('/login') 처럼 주소 앞부분을 생략한다
    trace: 'on-first-retry',
    screenshot: 'only-on-failure', // 실패하면 화면을 저장해서 원인을 볼 수 있게 한다
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: {
    command: 'npm run dev', // 프론트 서버가 없으면 자동으로 띄운다 (백엔드는 직접 먼저 실행한다)
    url: FRONTEND_URL,
    reuseExistingServer: true,
  },
})
