import { getPartLabel } from '../api/types'
import { useAuth } from '../auth/useAuth'
import { cardClass } from '../components/styles'

export function MyPage() {
  const { user } = useAuth()

  // ProtectedRoute가 로그인 여부를 보장하지만, 타입상 user는 null일 수 있다.
  if (!user) {
    return null
  }

  const rows: { label: string; value: string }[] = [
    { label: '이메일', value: user.email },
    { label: '이름', value: user.name },
    { label: '닉네임', value: user.nickname },
    { label: '기수', value: `${user.generation}기` },
    { label: '파트', value: getPartLabel(user.part) },
  ]

  return (
    <div className="mx-auto max-w-md">
      <h1 className="mb-6 text-2xl font-bold">마이페이지</h1>
      <dl className={`${cardClass} divide-y divide-slate-100`}>
        {rows.map((row) => (
          <div key={row.label} className="flex justify-between gap-4 py-3 first:pt-0 last:pb-0">
            <dt className="text-sm text-slate-500">{row.label}</dt>
            <dd className="text-sm font-medium text-slate-900">{row.value}</dd>
          </div>
        ))}
      </dl>
    </div>
  )
}
