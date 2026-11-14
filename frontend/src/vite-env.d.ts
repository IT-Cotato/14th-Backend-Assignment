/// <reference types="vite/client" />

interface ImportMetaEnv {
  // 백엔드 주소. 없으면 http://localhost:8081을 씁니다.
  readonly VITE_API_BASE_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
