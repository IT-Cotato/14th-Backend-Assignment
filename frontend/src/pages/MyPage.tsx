import { useEffect, useState } from 'react'
import { getMe } from '../api/auth'
import { getErrorMessage } from '../api/error'
import { PART_LABELS, type User } from '../api/types'
import { ErrorAlert } from '../components/ErrorAlert'
import { Loading } from '../components/Loading'

export function MyPage() {
  const [me, setMe] = useState<User | null>(null)
  const [error, setError] = useState<string | null>(null)

  // 헤더용 정보와 별개로, 마이페이지는 들어올 때마다 서버에서 최신 정보를 받아와요
  useEffect(() => {
    let cancelled = false
    getMe()
      .then((user) => {
        if (!cancelled) setMe(user)
      })
      .catch((err: unknown) => {
        // 401이면 API 클라이언트가 /login으로 보내요
        if (!cancelled) setError(getErrorMessage(err))
      })
    return () => {
      cancelled = true
    }
  }, [])

  return (
    <section className="mx-auto w-full max-w-md">
      <h1 className="mb-6 text-2xl font-bold text-slate-900">마이페이지</h1>
      {error ? (
        <ErrorAlert message={error} />
      ) : !me ? (
        <Loading />
      ) : (
        <dl className="divide-y divide-slate-100 rounded-xl border border-slate-200 bg-white shadow-sm">
          <InfoRow label="이메일" value={me.email} />
          <InfoRow label="이름" value={me.name} />
          <InfoRow label="닉네임" value={me.nickname} />
          <InfoRow label="기수" value={`${me.generation}기`} />
          <InfoRow label="파트" value={`${me.part} (${PART_LABELS[me.part]})`} />
        </dl>
      )}
    </section>
  )
}

function InfoRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex flex-col gap-1 px-5 py-4 sm:flex-row sm:items-center sm:gap-4">
      <dt className="text-sm font-medium text-slate-500 sm:w-20 sm:shrink-0">{label}</dt>
      <dd className="break-all text-base text-slate-900">{value}</dd>
    </div>
  )
}
