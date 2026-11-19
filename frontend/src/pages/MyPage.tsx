import { partLabel } from '../api/types'
import { useAuth } from '../auth/authContext'
import { Loading } from '../components/StatusMessage'

// RequireAuth 안에서만 보여요. 내 정보는 AuthProvider가 /api/users/me로 불러온 값이에요.
export function MyPage() {
  const { user } = useAuth()

  if (!user) {
    return <Loading />
  }

  const rows: { label: string; value: string }[] = [
    { label: '이메일', value: user.email },
    { label: '이름', value: user.name },
    { label: '닉네임', value: user.nickname },
    { label: '기수', value: `${user.generation}기` },
    { label: '파트', value: partLabel(user.part) },
  ]

  return (
    <section className="mx-auto max-w-md">
      <h1 className="mb-6 text-2xl font-bold">마이페이지</h1>

      <dl className="divide-y divide-gray-200 rounded-lg border border-gray-200 bg-white">
        {rows.map((row) => (
          <div key={row.label} className="flex flex-col gap-1 px-4 py-3 sm:flex-row sm:gap-4">
            <dt className="w-20 shrink-0 text-sm text-gray-500">{row.label}</dt>
            <dd className="text-sm font-medium text-gray-900">{row.value}</dd>
          </div>
        ))}
      </dl>
    </section>
  )
}
