import { useEffect, useState } from 'react'
import { api, errorMessage } from './api'

export function useLoad<T>(path: string, refresh = 0) {
  const [result, setResult] = useState<{ path: string; refresh: number; data?: T; error?: string }>()
  useEffect(() => {
    const controller = new AbortController()
    api<T>(path, { signal: controller.signal })
      .then(data => { if (!controller.signal.aborted) setResult({ path, refresh, data }) })
      .catch(error => {
        if (!controller.signal.aborted) setResult({ path, refresh, error: errorMessage(error) })
      })
    return () => controller.abort()
  }, [path, refresh])
  return result?.path === path && result.refresh === refresh ? result : undefined
}
