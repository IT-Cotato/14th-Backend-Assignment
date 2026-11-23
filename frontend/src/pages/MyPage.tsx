import { useEffect, useState } from 'react'
import { getMe } from '../api/auth'
import type { MeResponse } from '../api/auth'
import { getErrorMessage } from '../api/client'
import { ErrorMessage } from '../components/ErrorMessage'
import { cardClass } from '../components/styles'

export function MyPage() {
  const [me, setMe] = useState<MeResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    getMe()
      .then(setMe)
      .catch((err) => setError(getErrorMessage(err)))
      .finally(() => setLoading(false))
  }, [])

  const rows: [string, string][] = me
    ? [
        ['이메일', me.email],
        ['이름', me.name],
        ['닉네임', me.nickname],
        ['기수', `${me.generation}기`],
        ['파트', me.part],
      ]
    : []

  return (
    <section className={cardClass}>
      <h1 className="mb-6 text-2xl font-bold text-gray-900">마이페이지</h1>
      {loading && <p className="text-gray-500">불러오는 중...</p>}
      <ErrorMessage message={error} />
      {me && (
        <dl className="divide-y divide-gray-100">
          {rows.map(([label, value]) => (
            <div key={label} className="flex flex-col gap-1 py-3 sm:flex-row sm:gap-4">
              <dt className="text-sm font-medium text-gray-500 sm:w-24 sm:shrink-0">{label}</dt>
              <dd className="break-all text-gray-900">{value}</dd>
            </div>
          ))}
        </dl>
      )}
    </section>
  )
}
