import { defineConfig, devices } from '@playwright/test'

// 실제 프론트엔드(Vite, 3000)와 실제 백엔드(Spring Boot, 8080)를 대상으로 E2E 테스트를 실행해요.
// 이미 실행 중인 서버가 있으면 그대로 재사용해요.
export default defineConfig({
    testDir: './e2e',
    // 테스트들이 같은 로컬 DB를 쓰기 때문에 순서대로 실행해서 목록·페이지 검증이 서로 영향을 주지 않게 해요.
    fullyParallel: false,
    workers: 1,
    forbidOnly: !!process.env.CI,
    retries: 0,
    timeout: 30_000,
    expect: { timeout: 5_000 },

    // 실행 결과물 경로예요. (모두 .gitignore에 포함돼요)
    outputDir: './test-results',
    reporter: [['list'], ['html', { outputFolder: './playwright-report', open: 'never' }]],

    use: {
        baseURL: 'http://localhost:3000',
        locale: 'ko-KR',
        timezoneId: 'Asia/Seoul',
        trace: 'retain-on-failure',
        screenshot: 'only-on-failure',
        video: 'off',
    },

    projects: [
        {
            name: 'chromium',
            use: { ...devices['Desktop Chrome'] },
        },
    ],

    webServer: [
        {
            // 백엔드는 backend/.env의 로컬 DB 설정을 사용해요.
            command: process.platform === 'win32' ? 'gradlew.bat bootRun' : './gradlew bootRun',
            cwd: '../backend',
            url: 'http://localhost:8080/api/study-posts',
            reuseExistingServer: true,
            timeout: 180_000,
        },
        {
            command: 'npm run dev',
            url: 'http://localhost:3000',
            reuseExistingServer: true,
            timeout: 60_000,
        },
    ],
})
