import { defineConfig, devices } from '@playwright/test'

// E2E 테스트 설정
// 백엔드(http://localhost:8080)는 먼저 직접 켜 두어야 해요. (backend/에서 .\gradlew.bat bootRun)
// 프론트 개발 서버는 꺼져 있으면 아래 webServer 설정으로 자동으로 켜져요.
export default defineConfig({
  testDir: './e2e',
  // 테스트들이 같은 DB를 쓰니까 하나씩 차례로 실행해요
  fullyParallel: false,
  workers: 1,
  retries: 0,
  // 터미널에 테스트 이름이 한 줄씩 나와요
  reporter: 'list',
  use: {
    baseURL: 'http://localhost:3000',
    // 실패한 테스트만 스크린샷과 trace를 남겨요 (test-results/)
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:3000',
    // 이미 켜 둔 개발 서버가 있으면 그대로 써요
    reuseExistingServer: true,
    timeout: 60_000,
  },
})
