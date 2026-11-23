import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    // 백엔드 CORS 설정이 3000번 포트를 허용한다. 포트가 사용 중이면 다른 포트로 넘어가지 않고 실패한다.
    port: 3000,
    strictPort: true,
  },
})
