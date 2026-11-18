import { PART_LABELS } from '../api/types.ts'
import { useAuth } from '../auth/useAuth.ts'

export default function MyPage() {
  const { user } = useAuth()
  if (!user) return null

  const rows: [string, string][] = [
    ['이메일', user.email],
    ['이름', user.name],
    ['닉네임', user.nickname],
    ['기수', `${user.generation}기`],
    ['파트', `${PART_LABELS[user.part]} (${user.part})`],
  ]

  return (
    <div className="rounded-lg bg-white p-6 shadow-sm">
      <h1 className="mb-4 text-xl font-bold">마이페이지</h1>
      <dl className="divide-y divide-gray-100">
        {rows.map(([label, value]) => (
          <div key={label} className="flex justify-between py-2 text-sm">
            <dt className="text-gray-500">{label}</dt>
            <dd className="font-medium">{value}</dd>
          </div>
        ))}
      </dl>
    </div>
  )
}
