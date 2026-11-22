import { defineConfig, devices } from '@playwright/test'
import { env } from 'node:process'

if (env.E2E_ISOLATED_MYSQL !== '1') {
  throw new Error('Run npm run test:e2e to start an isolated MySQL and Spring Boot instance.')
}

const baseURL = 'http://127.0.0.1:3000'

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  workers: 1,
  forbidOnly: Boolean(env.CI),
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL,
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: {
    command: 'npm run dev -- --host 127.0.0.1 --port 3000 --strictPort',
    url: baseURL,
    reuseExistingServer: false,
    env: { VITE_API_BASE_URL: '' },
  },
})
