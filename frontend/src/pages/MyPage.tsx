import { useQuery } from '@tanstack/react-query'
import { getMyInfo } from '../api/auth'
import { getErrorMessage } from '../api/errors'
import { PART_LABEL } from '../api/types'
import { ME_QUERY_KEY } from '../auth/AuthContext'
import { ErrorMessage, Loading } from '../components/ui'

export function MyPage() {
  const { data: me, isPending, isError, error } = useQuery({
    queryKey: ME_QUERY_KEY,
    queryFn: getMyInfo,
    retry: false,
  })

  if (isPending) return <Loading />
  if (isError) return <ErrorMessage>{getErrorMessage(error)}</ErrorMessage>

  const rows = [
    { label: '이메일', value: me.email },
    { label: '이름', value: me.name },
    { label: '닉네임', value: me.nickname },
    { label: '기수', value: `${me.generation}기` },
    { label: '파트', value: `${PART_LABEL[me.part]} (${me.part})` },
  ]

  return (
    <section className="mx-auto max-w-md">
      <h1 className="mb-6 text-2xl font-bold">마이페이지</h1>
      <dl className="divide-y divide-stone-100 rounded-xl bg-white shadow-sm">
        {rows.map((row) => (
          <div key={row.label} className="flex flex-wrap items-center justify-between gap-2 px-5 py-4">
            <dt className="text-sm text-stone-500">{row.label}</dt>
            <dd className="min-w-0 break-all font-medium text-stone-900">{row.value}</dd>
          </div>
        ))}
      </dl>
    </section>
  )
}
