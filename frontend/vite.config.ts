import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  // 백엔드 CORS 설정이 3000번 포트만 허용하므로 포트를 고정한다
  server: { port: 3000, strictPort: true },
})
