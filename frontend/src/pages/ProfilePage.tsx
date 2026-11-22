import { useEffect, useState } from 'react'
import { getProfile, type Profile } from '../api/auth'
import { ApiError, errorMessage } from '../api/client'

type Result = { profile: Profile } | { error: string }

export default function ProfilePage({ token, onUnauthorized }: {
  token: string
  onUnauthorized: (token: string) => void
}) {
  const [result, setResult] = useState<Result | null>(null)
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    getProfile(token, controller.signal).then((profile) => {
      if (!controller.signal.aborted) setResult({ profile })
    }).catch((error: unknown) => {
      if (controller.signal.aborted) return
      if (error instanceof ApiError && error.status === 401) {
        onUnauthorized(token)
      } else {
        setResult({ error: errorMessage(error) })
      }
    })
    return () => controller.abort()
  }, [token, attempt, onUnauthorized])

  return (
    <section className="panel" aria-labelledby="profile-heading">
      <h1 id="profile-heading">내 정보</h1>
      {!result && <p role="status">내 정보를 불러오는 중입니다.</p>}
      {result && 'error' in result && <>
        <p className="error" role="alert">{result.error}</p>
        <button type="button" onClick={() => { setResult(null); setAttempt((value) => value + 1) }}>다시 시도</button>
      </>}
      {result && 'profile' in result && (
        <dl className="profile">
          <div><dt>이름</dt><dd>{result.profile.name}</dd></div>
          <div><dt>이메일</dt><dd>{result.profile.email}</dd></div>
          <div><dt>닉네임</dt><dd>{result.profile.nickname}</dd></div>
          <div><dt>기수</dt><dd>{result.profile.generation}</dd></div>
          <div><dt>파트</dt><dd>{result.profile.part}</dd></div>
        </dl>
      )}
    </section>
  )
}
