import { useEffect, useState } from 'react'
import { getMyInfo } from '../api/auth'
import { getErrorMessage } from '../api/client'
import { PART_LABELS, type MyInfo } from '../api/types'
import { Card, ErrorMessage, Loading } from '../components/ui'

export default function MyPage() {
  const [me, setMe] = useState<MyInfo | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    getMyInfo()
      .then(setMe)
      .catch((err) => setError(getErrorMessage(err, '내 정보를 불러오지 못했어요.')))
  }, [])

  if (error) return <ErrorMessage message={error} />
  if (!me) return <Loading />

  const rows: [string, string][] = [
    ['이메일', me.email],
    ['이름', me.name],
    ['닉네임', me.nickname],
    ['기수', `${me.generation}기`],
    ['파트', `${PART_LABELS[me.part]} (${me.part})`],
  ]

  return (
    <div className="mx-auto max-w-md">
      <Card>
        <h1 className="mb-6 text-xl font-bold">마이페이지</h1>
        <dl className="divide-y divide-gray-100">
          {rows.map(([label, value]) => (
            <div key={label} className="flex justify-between gap-4 py-3 text-sm">
              <dt className="text-gray-500">{label}</dt>
              <dd className="break-all text-right font-medium text-gray-900">{value}</dd>
            </div>
          ))}
        </dl>
      </Card>
    </div>
  )
}
