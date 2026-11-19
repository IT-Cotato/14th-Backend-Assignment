import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    // 백엔드 CORS 설정이 3000번만 허용한다
    port: 3000,
    strictPort: true,
  },
})
