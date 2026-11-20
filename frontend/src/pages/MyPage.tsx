import { useQuery } from '@tanstack/react-query'
import { fetchMe } from '../api/auth'
import { getErrorMessage } from '../api/client'
import { ErrorMessage } from '../components/ErrorMessage'
import { Loading } from '../components/Loading'
import { cardClass } from '../components/styles'
import { PART_LABELS } from '../types'

export function MyPage() {
  const { data: user, isPending, isError, error } = useQuery({
    queryKey: ['me', 'mypage'],
    queryFn: fetchMe,
  })

  return (
    <section className={`mx-auto max-w-md ${cardClass}`}>
      <h1 className="mb-4 text-xl font-bold">마이페이지</h1>
      {isPending && <Loading />}
      {isError && <ErrorMessage message={getErrorMessage(error)} />}
      {user && (
        <dl className="divide-y divide-slate-100 text-sm">
          <InfoRow term="이메일" description={user.email} />
          <InfoRow term="이름" description={user.name} />
          <InfoRow term="닉네임" description={user.nickname} />
          <InfoRow term="기수" description={`${user.generation}기`} />
          <InfoRow term="파트" description={`${user.part} (${PART_LABELS[user.part]})`} />
        </dl>
      )}
    </section>
  )
}

function InfoRow({ term, description }: { term: string; description: string }) {
  return (
    <div className="flex flex-wrap justify-between gap-2 py-3">
      <dt className="font-medium text-slate-500">{term}</dt>
      <dd className="break-all text-slate-900">{description}</dd>
    </div>
  )
}
