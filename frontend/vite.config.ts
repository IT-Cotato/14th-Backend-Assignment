import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        // Keep Host aligned with the browser Origin for same-origin API requests.
        changeOrigin: false,
      },
    },
  },
})
