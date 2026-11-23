import { useEffect, useState } from 'react'

type Result<T> = { data: T } | { error: unknown }

export function useReadRequest<T>(load: (signal: AbortSignal) => Promise<T>) {
  const [result, setResult] = useState<Result<T> | null>(null)
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    load(controller.signal).then((data) => {
      if (!controller.signal.aborted) setResult({ data })
    }).catch((error: unknown) => {
      if (!controller.signal.aborted) setResult({ error })
    })
    return () => controller.abort()
  }, [load, attempt])

  function retry() {
    setResult(null)
    setAttempt((value) => value + 1)
  }

  return { result, retry }
}
