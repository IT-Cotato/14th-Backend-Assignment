import { defineConfig, devices } from '@playwright/test'

const FRONTEND_URL = 'http://localhost:3000'

export default defineConfig({
    testDir: './e2e',
    fullyParallel: false,
    workers: 1,
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