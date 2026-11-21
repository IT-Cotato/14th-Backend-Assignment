import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    // 백엔드 CORS 설정(3주차)이 3000번을 허용하고 있어서 3000번으로 고정해요.
    // strictPort: 3000번이 이미 쓰이고 있으면 다른 포트로 바꾸지 않고 에러를 내요.
    port: 3000,
    strictPort: true,
  },
})
